package dev.velometr.backend

import kotlinx.serialization.Serializable

/** An activity parsed from a Strava `activities.csv` row, before persistence. */
data class ParsedActivity(
    val id: Long,
    val date: String,
    val title: String?,
    val distanceKm: Double,
    val durationSec: Long,
    val avgSpeedKmh: Double?,
    val maxSpeedKmh: Double?,
    val trackFilename: String?,
)

@Serializable
data class ActivityDto(
    val id: Long,
    val date: String,
    val title: String?,
    val distanceKm: Double,
    val durationSec: Long,
    val avgSpeed: Double?,
    val maxSpeed: Double?,
)

@Serializable
data class YearSummaryDto(
    val year: Int,
    val totalDistanceKm: Double,
    val tripCount: Int,
    val avgWeeklyDistanceKm: Double,
    val longestTripKm: Double,
)

@Serializable
data class WeeklyDistanceDto(val year: Int, val weeks: List<Double>)
