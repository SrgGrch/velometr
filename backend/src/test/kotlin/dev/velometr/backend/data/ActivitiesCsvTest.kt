package dev.velometr.backend.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActivitiesCsvTest {

    private fun sampleCsvText(): String =
        ActivitiesCsvTest::class.java.getResourceAsStream("/sample-activities.csv")!!
            .bufferedReader(Charsets.UTF_8).readText()

    @Test
    fun `parses a representative real Strava export`() {
        val activities = ActivitiesCsv.parse(sampleCsvText())

        assertEquals(48, activities.size)

        val first = activities.first { it.id == 20055596812L }
        assertEquals("2026-09-05T05:50:18", first.date)
        assertEquals("Lunch Mountain Bike Ride", first.title)
        assertEquals(29.3953, first.distanceKm, 1e-3)
        assertEquals(9286L, first.durationSec)
        assertEquals(11.3976, first.avgSpeedKmh!!, 1e-3)
        assertEquals(34.704, first.maxSpeedKmh!!, 1e-3)
        assertEquals("activities/20055596812.gpx", first.trackFilename)
    }

    @Test
    fun `uses the precise meter-based Distance column, not the rounded km one`() {
        // The header has two "Distance" columns: an early rounded-km one and a
        // later precise-meters one paired with Moving Time / Average Speed.
        // Picking the wrong one would silently desync distance from speed*time.
        val activities = ActivitiesCsv.parse(sampleCsvText())
        for (activity in activities) {
            val expectedAvgKmh = activity.distanceKm / (activity.durationSec / 3600.0)
            val actualAvgKmh = activity.avgSpeedKmh
            if (actualAvgKmh != null && activity.durationSec > 0) {
                assertEquals(expectedAvgKmh, actualAvgKmh, 0.05)
            }
        }
    }

    @Test
    fun `blank optional columns become null rather than throwing`() {
        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("42,\"Jan 1, 2026, 9:00:00 AM\",,1000,600,,,")
        }
        val activities = ActivitiesCsv.parse(csv)
        assertEquals(1, activities.size)
        val activity = activities.single()
        assertEquals(42L, activity.id)
        assertNull(activity.title)
        assertNull(activity.avgSpeedKmh)
        assertNull(activity.maxSpeedKmh)
        assertNull(activity.trackFilename)
        assertEquals(1.0, activity.distanceKm, 1e-9)
        assertEquals(600L, activity.durationSec)
    }
}
