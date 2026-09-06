package dev.velometr.frontend

import kotlin.math.roundToLong
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private val MONTHS_SHORT = listOf(
    "янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек",
)

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
