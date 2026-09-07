package dev.velometr.backend.data

import dev.velometr.backend.domain.ParsedActivity
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityRepositoryTest {

    private fun tempDbPath(): String {
        val dir = Files.createTempDirectory("velometr-test")
        return dir.resolve("velometr.db").toString()
    }

    private fun sampleActivity(id: Long, distanceKm: Double = 10.0) = ParsedActivity(
        id = id,
        date = "2026-06-01T10:00:00",
        title = "Test ride",
        distanceKm = distanceKm,
        durationSec = 3600,
        avgSpeedKmh = distanceKm,
        maxSpeedKmh = distanceKm * 1.5,
        trackFilename = null,
    )

    @Test
    fun `activities table is created on first startup`() {
        val path = tempDbPath()
        Database.migrate(path)

        Database.connect(path).use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='activities'"
                )
                assertTrue(rs.next(), "activities table should exist after migrate()")
            }
        }
    }

    @Test
    fun `journal mode is WAL and a concurrent read succeeds during an open write`() {
        val path = tempDbPath()
        Database.migrate(path)

        Database.connect(path).use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("PRAGMA journal_mode")
                rs.next()
                assertEquals("wal", rs.getString(1).lowercase())
            }
        }

        val writer = Database.connect(path)
        writer.autoCommit = false
        writer.prepareStatement(
            "INSERT INTO activities (id, date, distance_km, duration_sec) VALUES (?, ?, ?, ?)"
        ).use { stmt ->
            stmt.setLong(1, 1L)
            stmt.setString(2, "2026-01-01T00:00:00")
            stmt.setDouble(3, 5.0)
            stmt.setLong(4, 100)
            stmt.executeUpdate()
        }
        // Write transaction left open (not committed) on purpose.

        val readSucceeded = Database.connect(path).use { reader ->
            reader.createStatement().use { stmt ->
                stmt.executeQuery("SELECT count(*) FROM activities").use { rs ->
                    rs.next()
                    true
                }
            }
        }

        writer.rollback()
        writer.close()

        assertTrue(readSucceeded, "a read should not block/fail while a write transaction is open under WAL")
    }

    @Test
    fun `repeated insert of the same activity id is a no-op`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)

        val first = repository.importBatch(listOf(sampleActivity(1L) to null))
        assertEquals(1, first.imported)
        assertEquals(0, first.skipped)

        val second = repository.importBatch(listOf(sampleActivity(1L) to null))
        assertEquals(0, second.imported)
        assertEquals(1, second.skipped)

        assertEquals(1, repository.listYear(2026).size)
    }

    @Test
    fun `year summary for a year with no activities returns zero values without error`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)

        val summary = repository.yearSummary(2030)

        assertEquals(0, summary.tripCount)
        assertEquals(0.0, summary.totalDistanceKm)
        assertEquals(0.0, summary.longestTripKm)
        assertEquals(0.0, summary.avgWeeklyDistanceKm)
    }

    @Test
    fun `year summary averages distance over distinct weeks with multiple trips in the same week`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)
        repository.importBatch(
            listOf(
                // Both dates fall in the same Monday-Sunday calendar week.
                sampleActivity(1L, distanceKm = 10.0).copy(date = "2026-06-01T10:00:00") to null,
                sampleActivity(2L, distanceKm = 20.0).copy(date = "2026-06-03T10:00:00") to null,
            )
        )

        val summary = repository.yearSummary(2026)

        assertEquals(30.0, summary.avgWeeklyDistanceKm, 1e-9)
    }

    @Test
    fun `year summary averages distance over distinct weeks with trips across separate weeks`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)
        repository.importBatch(
            listOf(
                sampleActivity(1L, distanceKm = 10.0).copy(date = "2026-01-01T10:00:00") to null, // week 0
                sampleActivity(2L, distanceKm = 20.0).copy(date = "2026-01-08T10:00:00") to null, // week 1
            )
        )

        val summary = repository.yearSummary(2026)

        assertEquals(15.0, summary.avgWeeklyDistanceKm, 1e-9)
    }

    @Test
    fun `year summary counts a week containing only a zero-distance trip as active`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)
        repository.importBatch(
            listOf(
                sampleActivity(1L, distanceKm = 0.0).copy(date = "2026-01-01T10:00:00") to null, // week 0
                sampleActivity(2L, distanceKm = 20.0).copy(date = "2026-01-08T10:00:00") to null, // week 1
            )
        )

        val summary = repository.yearSummary(2026)

        // Divisor must be 2 (both weeks are active), not 1 (only the non-zero-distance bucket).
        assertEquals(20.0, summary.totalDistanceKm, 1e-9)
        assertEquals(10.0, summary.avgWeeklyDistanceKm, 1e-9)
    }

    @Test
    fun `weekly distances for a year with no activities is all zero buckets`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)

        // 2030-01-01 is a Tuesday, so the Monday-anchored grid needs a 53rd bucket
        // to cover the partial last week.
        val weeks = repository.weeklyDistances(2030)

        assertEquals(53, weeks.size)
        assertTrue(weeks.all { it == 0.0 })
    }

    @Test
    fun `weekly distances bucket activities by real calendar week and fold the year-end remainder into the last bucket`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)
        repository.importBatch(
            listOf(
                // 2026-01-01 is a Thursday, so week 0 is the partial week Jan 1-4.
                sampleActivity(1L, distanceKm = 5.0).copy(date = "2026-01-01T00:00:00") to null, // week 0
                sampleActivity(2L, distanceKm = 7.0).copy(date = "2026-01-08T00:00:00") to null, // week 1
                sampleActivity(3L, distanceKm = 9.0).copy(date = "2026-12-31T00:00:00") to null, // week 52 (last)
            )
        )

        val weeks = repository.weeklyDistances(2026)

        assertEquals(5.0, weeks[0], 1e-9)
        assertEquals(7.0, weeks[1], 1e-9)
        assertEquals(9.0, weeks[52], 1e-9)
    }

    @Test
    fun `weekly distances split a week straddling two years between each year's own bucket`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)
        repository.importBatch(
            listOf(
                // 2025-12-29 (Mon) - 2026-01-04 (Sun) is one calendar week straddling the year boundary.
                sampleActivity(1L, distanceKm = 4.0).copy(date = "2025-12-30T00:00:00") to null,
                sampleActivity(2L, distanceKm = 6.0).copy(date = "2026-01-02T00:00:00") to null,
            )
        )

        val weeks2025 = repository.weeklyDistances(2025)
        val weeks2026 = repository.weeklyDistances(2026)

        assertEquals(4.0, weeks2025.last(), 1e-9)
        assertEquals(6.0, weeks2026[0], 1e-9)
    }

    @Test
    fun `a batch with new and already-imported ids only inserts the new ones`() {
        val path = tempDbPath()
        Database.migrate(path)
        val repository = ActivityRepository(path)

        repository.importBatch(listOf(sampleActivity(1L) to null))

        val result = repository.importBatch(
            listOf(sampleActivity(1L) to null, sampleActivity(2L) to null)
        )

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertEquals(2, repository.listYear(2026).size)
    }
}
