package dev.velometr.backend

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.Reader

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

    fun parse(csvText: String): List<ParsedActivity> = parse(csvText.reader()).toList()

    fun parse(reader: Reader): Sequence<ParsedActivity> = sequence {
        val format = CSVFormat.DEFAULT.builder().setIgnoreSurroundingSpaces(true).get()
        CSVParser.parse(reader, format).use { parser ->
            val records = parser.iterator()
            if (!records.hasNext()) throw InvalidArchiveException("activities.csv is empty")

            val header = records.next()
            fun missing(name: String): Nothing = throw InvalidArchiveException("Missing CSV column: $name")
            val idIdx = firstIndex(header, "Activity ID") ?: missing("Activity ID")
            val dateIdx = firstIndex(header, "Activity Date") ?: missing("Activity Date")
            val nameIdx = firstIndex(header, "Activity Name")
            val distanceIdx = lastIndex(header, "Distance") ?: missing("Distance")
            val durationIdx = firstIndex(header, "Moving Time")
                ?: lastIndex(header, "Elapsed Time")
                ?: missing("Moving Time / Elapsed Time")
            val maxSpeedIdx = firstIndex(header, "Max Speed")
            val avgSpeedIdx = firstIndex(header, "Average Speed")
            val filenameIdx = firstIndex(header, "Filename")

            for (record in records) {
                if (record.size() < header.size()) throw InvalidArchiveException("Incomplete CSV row ${record.recordNumber}")
                val id = record.get(idIdx).trim().toLongOrNull() ?: continue
                val date = runCatching { LocalDateTime.parse(record.get(dateIdx).trim(), DATE_FORMAT) }
                    .getOrNull() ?: continue
                yield(ParsedActivity(
                    id = id,
                    date = date.toString(),
                    title = nameIdx?.let { record.get(it).trim().ifBlank { null } },
                    distanceKm = record.get(distanceIdx).trim().toDoubleOrNull()?.div(1000.0) ?: 0.0,
                    durationSec = record.get(durationIdx).trim().toDoubleOrNull()?.toLong() ?: 0L,
                    avgSpeedKmh = avgSpeedIdx?.let { record.get(it).trim().toDoubleOrNull()?.times(3.6) },
                    maxSpeedKmh = maxSpeedIdx?.let { record.get(it).trim().toDoubleOrNull()?.times(3.6) },
                    trackFilename = filenameIdx?.let { record.get(it).trim().ifBlank { null } },
                ))
            }
        }
    }

    private fun firstIndex(header: CSVRecord, name: String): Int? =
        (0 until header.size()).firstOrNull { header.get(it) == name }

    private fun lastIndex(header: CSVRecord, name: String): Int? =
        (0 until header.size()).lastOrNull { header.get(it) == name }
}
