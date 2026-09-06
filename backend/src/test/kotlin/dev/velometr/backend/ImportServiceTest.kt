package dev.velometr.backend

import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ImportServiceTest {

    private fun tempDbPath(): String {
        val dir = Files.createTempDirectory("velometr-import-test")
        return dir.resolve("velometr.db").toString()
    }

    private fun buildZip(csv: String, trackEntries: Map<String, ByteArray>): File {
        val file = File.createTempFile("velometr-import-test", ".zip")
        ZipOutputStream(file.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("activities.csv"))
            zos.write(csv.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            for ((name, bytes) in trackEntries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return file
    }

    private fun trackBlob(path: String, id: Long): ByteArray? {
        Database.connect(path).use { conn ->
            conn.prepareStatement("SELECT track_gpx FROM activities WHERE id = ?").use { stmt ->
                stmt.setLong(1, id)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return rs.getBytes("track_gpx")
                }
            }
        }
    }

    private fun newImportService(dbPath: String): Pair<ActivityRepository, ImportService> {
        Database.migrate(dbPath)
        val repository = ActivityRepository(dbPath)
        return repository to ImportService(repository)
    }

    @Test
    fun `imports rows from activities-csv found inside the zip`() {
        val dbPath = tempDbPath()
        val (repository, importService) = newImportService(dbPath)

        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("1,\"Jan 1, 2026, 9:00:00 AM\",Morning ride,10000,3600,10,8,activities/1.gpx")
            appendLine("2,\"Jan 2, 2026, 9:00:00 AM\",No track ride,5000,1800,9,7,activities/missing.gpx")
        }
        val zip = buildZip(csv, mapOf("activities/1.gpx" to "GPXDATA".toByteArray()))

        val result = importService.importZip(zip)

        assertEquals(2, result.total)
        assertEquals(2, result.imported)
        assertEquals(0, result.skipped)
        assertEquals(2, repository.listYear(2026).size)

        zip.delete()
    }

    @Test
    fun `attaches a gzip-compressed track file when the archive has a match`() {
        val dbPath = tempDbPath()
        val (_, importService) = newImportService(dbPath)

        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("1,\"Jan 1, 2026, 9:00:00 AM\",Morning ride,10000,3600,10,8,activities/1.gpx")
        }
        val rawTrack = "GPXDATA-CONTENT".toByteArray()
        val zip = buildZip(csv, mapOf("activities/1.gpx" to rawTrack))

        importService.importZip(zip)

        val stored = trackBlob(dbPath, 1L)
        assertNotNull(stored)
        val decompressed = GZIPInputStream(stored.inputStream()).readBytes()
        assertEquals(String(rawTrack), String(decompressed))

        zip.delete()
    }

    @Test
    fun `activity without a matching track file still imports with a null track`() {
        val dbPath = tempDbPath()
        val (repository, importService) = newImportService(dbPath)

        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("1,\"Jan 1, 2026, 9:00:00 AM\",No track ride,10000,3600,10,8,activities/missing.gpx")
        }
        val zip = buildZip(csv, emptyMap())

        val result = importService.importZip(zip)

        assertEquals(1, result.imported)
        assertEquals(1, repository.listYear(2026).size)
        assertNull(trackBlob(dbPath, 1L))

        zip.delete()
    }

    @Test
    fun `stats come from the CSV row even when a track file is also present`() {
        val dbPath = tempDbPath()
        val (repository, importService) = newImportService(dbPath)

        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("1,\"Jan 1, 2026, 9:00:00 AM\",Ride with track,12340,3600,11.5,9.2,activities/1.gpx")
        }
        val zip = buildZip(csv, mapOf("activities/1.gpx" to "irrelevant-track-bytes".toByteArray()))

        importService.importZip(zip)

        val activity = repository.listYear(2026).single()
        assertEquals(12.34, activity.distanceKm, 1e-9)
        assertEquals(3600L, activity.durationSec)
        assertEquals(9.2 * 3.6, activity.avgSpeed!!, 1e-9)
        assertEquals(11.5 * 3.6, activity.maxSpeed!!, 1e-9)

        zip.delete()
    }

    @Test
    fun `re-importing the same archive twice produces no duplicate rows`() {
        val dbPath = tempDbPath()
        val (repository, importService) = newImportService(dbPath)

        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("1,\"Jan 1, 2026, 9:00:00 AM\",Morning ride,10000,3600,10,8,")
            appendLine("2,\"Jan 2, 2026, 9:00:00 AM\",Evening ride,5000,1800,9,7,")
        }
        val zip = buildZip(csv, emptyMap())

        val firstImport = importService.importZip(zip)
        val secondImport = importService.importZip(zip)

        assertEquals(2, firstImport.imported)
        assertEquals(0, secondImport.imported)
        assertEquals(2, secondImport.skipped)
        assertEquals(2, repository.listYear(2026).size)

        zip.delete()
    }
}
