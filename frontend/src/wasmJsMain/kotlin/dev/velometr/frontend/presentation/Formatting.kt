package dev.velometr.frontend.presentation

import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private val MONTHS_SHORT = listOf(
    "янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек",
)

@OptIn(kotlin.time.ExperimentalTime::class)
fun currentYear(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

/** One decimal place with thousands grouping, e.g. 1842.37 -> "1 842.4". */
fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10).roundToLong()
    val whole = rounded / 10
    val fraction = kotlin.math.abs(rounded % 10)
    return "${groupThousands(whole)}.$fraction"
}

private fun groupThousands(value: Long): String {
    val digits = kotlin.math.abs(value).toString()
    val sb = StringBuilder()
    for ((index, char) in digits.withIndex()) {
        val fromEnd = digits.length - index
        if (index != 0 && fromEnd % 3 == 0) sb.append(' ')
        sb.append(char)
    }
    return if (value < 0) "-$sb" else sb.toString()
}

/** seconds -> "H:MM" */
fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return "$hours:${minutes.toString().padStart(2, '0')}"
}

fun formatSpeed(kmh: Double?): String = if (kmh == null) "-" else formatOneDecimal(kmh)

/** Which Monday-Sunday calendar week (0-based, week 0 starting Jan 1) a date falls into -
 * mirrors the backend's ActivityRepository.weeklyDistances bucketing, so trip-list grouping
 * lines up with the weekly chart. */
fun weekIndexOf(isoDateTime: String): Int {
    val date = LocalDate.parse(isoDateTime.substringBefore('T'))
    val jan1 = LocalDate(date.year, 1, 1)
    val firstMonday = jan1.minus(jan1.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    return firstMonday.daysUntil(date) / 7
}

/** Date range of the given calendar week (0-based, mirrors [weekIndexOf]), clipped to
 * [year]'s own days - e.g. "18 - 24 авг", or a single date for a one-day partial week. */
fun weekRangeLabel(year: Int, weekIndex: Int): String {
    val jan1 = LocalDate(year, 1, 1)
    val dec31 = LocalDate(year, 12, 31)
    val firstMonday = jan1.minus(jan1.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val weekStart = maxOf(jan1, firstMonday.plus(weekIndex * 7, DateTimeUnit.DAY))
    val weekEnd = minOf(dec31, firstMonday.plus(weekIndex * 7 + 6, DateTimeUnit.DAY))
    return if (weekStart == weekEnd) {
        formatShortDate(weekStart.toString())
    } else {
        "${formatShortDate(weekStart.toString())} - ${formatShortDate(weekEnd.toString())}"
    }
}

/** Number of weekly buckets (per [weekIndexOf]'s convention, [weekCount] entries starting
 * Jan 1) whose start date falls in each calendar month, always 12 entries in January-December
 * order. Lets month labels claim a proportional share of the chart's width matching the equal-
 * width week bars beneath them, so both rows stay aligned without any fixed pixel widths. */
fun weeksPerMonthCounts(year: Int, weekCount: Int): List<Int> {
    val jan1 = LocalDate(year, 1, 1)
    val firstMonday = jan1.minus(jan1.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val counts = IntArray(12)
    for (weekIndex in 0 until weekCount) {
        val weekStart = maxOf(jan1, firstMonday.plus(weekIndex * 7, DateTimeUnit.DAY))
        val month = weekStart.toString().substring(5, 7).toInt()
        counts[month - 1]++
    }
    return counts.toList()
}

/** ISO local date-time -> "d mmm" (e.g. "5 сен") */
fun formatShortDate(isoDateTime: String): String {
    val datePart = isoDateTime.substringBefore('T')
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val month = parts[1].toIntOrNull() ?: return datePart
    val day = parts[2].toIntOrNull() ?: return datePart
    val monthName = MONTHS_SHORT.getOrNull(month - 1) ?: return datePart
    return "$day $monthName"
}
