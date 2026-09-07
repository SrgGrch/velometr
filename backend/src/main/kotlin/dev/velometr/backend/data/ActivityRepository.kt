package dev.velometr.backend.data

import dev.velometr.backend.domain.ActivityDto
import dev.velometr.backend.domain.ParsedActivity
import dev.velometr.backend.domain.YearSummaryDto
import kotlinx.serialization.Serializable
import java.sql.Types
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Serializable
data class ImportStats(val imported: Int, val skipped: Int, val total: Int)

class ActivityRepository(private val dataPath: String) {

    private fun connection() = Database.connect(dataPath)

    /**
     * Inserts a batch of parsed activities in a single write transaction, using
     * INSERT OR IGNORE keyed on activity id so re-importing the same or an
     * overlapping archive never creates duplicates. Track bytes, when present,
     * are expected to already be gzip-compressed by the caller.
     */
    fun importBatch(activities: List<Pair<ParsedActivity, ByteArray?>>): ImportStats =
        importBatch(activities.asSequence())

    fun importBatch(activities: Sequence<Pair<ParsedActivity, ByteArray?>>): ImportStats {
        var inserted = 0
        var total = 0
        connection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement(
                    """
                    INSERT OR IGNORE INTO activities
                      (id, date, title, distance_km, duration_sec, avg_speed, max_speed, track_gpx)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { stmt ->
                    for ((activity, track) in activities) {
                        total++
                        stmt.setLong(1, activity.id)
                        stmt.setString(2, activity.date)
                        stmt.setString(3, activity.title)
                        stmt.setDouble(4, activity.distanceKm)
                        stmt.setLong(5, activity.durationSec)
                        setNullableDouble(stmt, 6, activity.avgSpeedKmh)
                        setNullableDouble(stmt, 7, activity.maxSpeedKmh)
                        if (track != null) stmt.setBytes(8, track) else stmt.setNull(8, Types.BLOB)
                        inserted += stmt.executeUpdate()
                    }
                }
                conn.commit()
            } catch (failure: Throwable) {
                runCatching { conn.rollback() }.exceptionOrNull()?.let { failure.addSuppressed(it) }
                throw failure
            }
        }
        return ImportStats(imported = inserted, skipped = total - inserted, total = total)
    }

    private fun setNullableDouble(stmt: java.sql.PreparedStatement, index: Int, value: Double?) {
        if (value != null) stmt.setDouble(index, value) else stmt.setNull(index, Types.REAL)
    }

    /** List/stats reads never select track_gpx - it is reserved for a future per-id endpoint. */
    fun listYear(year: Int): List<ActivityDto> {
        return connection().use { conn ->
            conn.prepareStatement(
                """
                SELECT id, date, title, distance_km, duration_sec, avg_speed, max_speed
                FROM activities
                WHERE date LIKE ?
                ORDER BY date DESC
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, "$year-%")
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<ActivityDto>()
                    while (rs.next()) {
                        result += ActivityDto(
                            id = rs.getLong("id"),
                            date = rs.getString("date"),
                            title = rs.getString("title"),
                            distanceKm = rs.getDouble("distance_km"),
                            durationSec = rs.getLong("duration_sec"),
                            avgSpeed = rs.getDouble("avg_speed").takeUnless { rs.wasNull() },
                            maxSpeed = rs.getDouble("max_speed").takeUnless { rs.wasNull() },
                        )
                    }
                    result
                }
            }
        }
    }

    fun yearSummary(year: Int): YearSummaryDto {
        val rows = listYear(year)
        val total = rows.sumOf { it.distanceKm }
        val firstMonday = LocalDate.of(year, 1, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val activeWeeks = rows
            .map { (ChronoUnit.DAYS.between(firstMonday, LocalDateTime.parse(it.date).toLocalDate()) / 7).toInt() }
            .toSet()
            .size
        return YearSummaryDto(
            year = year,
            totalDistanceKm = total,
            tripCount = rows.size,
            avgWeeklyDistanceKm = if (activeWeeks == 0) 0.0 else total / activeWeeks,
            longestTripKm = rows.maxOfOrNull { it.distanceKm } ?: 0.0,
        )
    }

    /** Real Monday-Sunday calendar weeks. Week 0 starts on Jan 1 itself (so it's
     * partial unless Jan 1 is a Monday), and the last week is likewise partial if
     * Dec 31 isn't a Sunday. A week straddling Dec 31/Jan 1 is never merged: since
     * [listYear] only returns rows dated within [year], each year's own share of
     * that shared week lands in its own bucket (last of one year, first of the next). */
    fun weeklyDistances(year: Int): List<Double> {
        val firstMonday = LocalDate.of(year, 1, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekCount = (ChronoUnit.DAYS.between(firstMonday, LocalDate.of(year, 12, 31)) / 7).toInt() + 1
        val weeks = DoubleArray(weekCount)
        for (row in listYear(year)) {
            val date = LocalDateTime.parse(row.date).toLocalDate()
            val weekIndex = (ChronoUnit.DAYS.between(firstMonday, date) / 7).toInt()
            weeks[weekIndex] += row.distanceKm
        }
        return weeks.toList()
    }
}
