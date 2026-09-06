package dev.velometr.backend

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses a Strava bulk-export `activities.csv`.
 *
 * Strava's export header has repeated column names (e.g. "Distance" and
 * "Elapsed Time" each appear twice - a legacy quirk of the export format), so
 * columns are resolved by position from the raw header row rather than by a
 * name -> value map, which would silently collapse duplicates. The first
 * "Distance"/"Elapsed Time" pair is coarser (rounded, from the activity
 * summary); the later "Distance" (meters) paired with "Moving Time" and
 * "Average Speed"/"Max Speed" (m/s) is the precise, internally-consistent set
 * and is what this parser uses.
 */
object ActivitiesCsv {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH)

    fun parse(csvText: String): List<ParsedActivity> {
        val format = CSVFormat.DEFAULT.builder().setIgnoreSurroundingSpaces(true).get()
        val records: List<CSVRecord> = CSVParser.parse(csvText, format).records
        if (records.isEmpty()) return emptyList()

        val header = records.first()
        val idIdx = firstIndex(header, "Activity ID") ?: return emptyList()
        val dateIdx = firstIndex(header, "Activity Date") ?: return emptyList()
        val nameIdx = firstIndex(header, "Activity Name")
        val distanceIdx = lastIndex(header, "Distance") ?: return emptyList()
        val durationIdx = firstIndex(header, "Moving Time")
            ?: lastIndex(header, "Elapsed Time")
            ?: return emptyList()
        val maxSpeedIdx = firstIndex(header, "Max Speed")
        val avgSpeedIdx = firstIndex(header, "Average Speed")
        val filenameIdx = firstIndex(header, "Filename")

        val result = mutableListOf<ParsedActivity>()
        for (record in records.drop(1)) {
            val id = record.get(idIdx).trim().toLongOrNull() ?: continue
            val date = runCatching { LocalDateTime.parse(record.get(dateIdx).trim(), DATE_FORMAT) }
                .getOrNull() ?: continue
            result += ParsedActivity(
                id = id,
                date = date.toString(),
                title = nameIdx?.let { record.get(it).trim().ifBlank { null } },
                distanceKm = record.get(distanceIdx).trim().toDoubleOrNull()?.div(1000.0) ?: 0.0,
                durationSec = record.get(durationIdx).trim().toDoubleOrNull()?.toLong() ?: 0L,
                avgSpeedKmh = avgSpeedIdx?.let { record.get(it).trim().toDoubleOrNull()?.times(3.6) },
                maxSpeedKmh = maxSpeedIdx?.let { record.get(it).trim().toDoubleOrNull()?.times(3.6) },
                trackFilename = filenameIdx?.let { record.get(it).trim().ifBlank { null } },
            )
        }
        return result
    }

    private fun firstIndex(header: CSVRecord, name: String): Int? =
        (0 until header.size()).firstOrNull { header.get(it) == name }

    private fun lastIndex(header: CSVRecord, name: String): Int? =
        (0 until header.size()).lastOrNull { header.get(it) == name }
}
