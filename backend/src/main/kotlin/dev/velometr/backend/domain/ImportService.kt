package dev.velometr.backend.domain

import dev.velometr.backend.data.ActivitiesCsv
import dev.velometr.backend.data.ActivityRepository
import dev.velometr.backend.data.ImportStats
import java.io.File
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipException

class InvalidArchiveException(message: String) : IllegalArgumentException(message)

class ImportService(private val repository: ActivityRepository) {

    /** Unzips the archive, parses activities.csv, and imports every row found,
     * attaching a gzip-compressed track file when the archive has one for it.
     * Distance/duration/speed always come from the CSV row, never recomputed
     * from track file contents. */
    fun importZip(zipFile: File): ImportStats {
        try {
            ZipFile(zipFile).use { zip ->
                val csvEntry = zip.entries().asSequence()
                    .firstOrNull { !it.isDirectory && it.name.substringAfterLast('/').equals("activities.csv", ignoreCase = true) }
                    ?: throw InvalidArchiveException("Archive must contain activities.csv")

                return zip.getInputStream(csvEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                    val activities = ActivitiesCsv.parse(reader)

                    val batch = activities.map { activity ->
                        val track = activity.trackFilename?.let { findTrackBytes(zip, it) }
                        activity to track
                    }
                    repository.importBatch(batch)
                }
            }
        } catch (e: ZipException) {
            throw InvalidArchiveException("Invalid ZIP archive")
        }
    }

    private fun findTrackBytes(zip: ZipFile, trackFilename: String): ByteArray? {
        val entry = zip.getEntry(trackFilename)
            ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("/$trackFilename") }
            ?: return null
        val buffer = java.io.ByteArrayOutputStream()
        zip.getInputStream(entry).use { input ->
            GZIPOutputStream(buffer).use { input.copyTo(it) }
        }
        return buffer.toByteArray()
    }
}
