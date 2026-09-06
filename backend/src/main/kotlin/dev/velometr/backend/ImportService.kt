package dev.velometr.backend

import java.io.File
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipFile

class ImportService(private val repository: ActivityRepository) {

    /** Unzips the archive, parses activities.csv, and imports every row found,
     * attaching a gzip-compressed track file when the archive has one for it.
     * Distance/duration/speed always come from the CSV row, never recomputed
     * from track file contents. */
    fun importZip(zipFile: File): ImportStats {
        ZipFile(zipFile).use { zip ->
            val csvEntry = zip.entries().asSequence()
                .firstOrNull { !it.isDirectory && it.name.substringAfterLast('/').equals("activities.csv", ignoreCase = true) }
                ?: return ImportStats(imported = 0, skipped = 0, total = 0)

            val csvText = zip.getInputStream(csvEntry).bufferedReader(Charsets.UTF_8).readText()
            val activities = ActivitiesCsv.parse(csvText)

            val batch = activities.map { activity ->
                val track = activity.trackFilename?.let { findTrackBytes(zip, it) }
                activity to track
            }
            return repository.importBatch(batch)
        }
    }

    private fun findTrackBytes(zip: ZipFile, trackFilename: String): ByteArray? {
        val entry = zip.getEntry(trackFilename)
            ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("/$trackFilename") }
            ?: return null
        val raw = zip.getInputStream(entry).use { it.readBytes() }
        val buffer = java.io.ByteArrayOutputStream()
        GZIPOutputStream(buffer).use { it.write(raw) }
        return buffer.toByteArray()
    }
}
