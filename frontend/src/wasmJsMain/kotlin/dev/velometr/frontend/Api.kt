package dev.velometr.frontend

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val passcode: String)

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

@Serializable
data class ImportStats(val imported: Int, val skipped: Int, val total: Int)
