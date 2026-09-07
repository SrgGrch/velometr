package dev.velometr.backend.data

import dev.velometr.backend.domain.ActivityDto
import dev.velometr.backend.domain.ParsedActivity
import dev.velometr.backend.domain.YearSummaryDto
import kotlinx.serialization.Serializable
import java.sql.Types
import java.time.LocalDateTime

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
        return YearSummaryDto(
            year = year,
            totalDistanceKm = total,
            tripCount = rows.size,
            avgWeeklyDistanceKm = total / 52.0,
            longestTripKm = rows.maxOfOrNull { it.distanceKm } ?: 0.0,
        )
    }

    /** 52 fixed 7-day buckets across the calendar year; the trailing days of a leap
     * year fold into the last bucket, matching the dashboard's fixed 52-bar chart. */
    fun weeklyDistances(year: Int): List<Double> {
        val weeks = DoubleArray(52)
        for (row in listYear(year)) {
            val dayOfYear = LocalDateTime.parse(row.date).dayOfYear
            val weekIndex = minOf(51, (dayOfYear - 1) / 7)
            weeks[weekIndex] += row.distanceKm
        }
        return weeks.toList()
    }
}
