package dev.velometr.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(api: ApiClient) {
    var year by remember { mutableStateOf(currentYear()) }
    var summary by remember { mutableStateOf<YearSummaryDto?>(null) }
    var weeks by remember { mutableStateOf<List<Double>>(emptyList()) }
    var activities by remember { mutableStateOf<List<ActivityDto>>(emptyList()) }
    var showImportModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun reload(y: Int) {
        summary = api.yearSummary(y)
        weeks = api.weeklyDistances(y).weeks
        activities = api.activities(y)
    }

    LaunchedEffect(year) { reload(year) }

    Box(Modifier.fillMaxSize().background(VelometrColors.background)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            DashboardHeader(
                year = year,
                onYearChange = { year = it },
                onImportClick = { showImportModal = true },
            )
            Spacer(Modifier.height(24.dp))
            summary?.let { HeroBlock(it) }
            Spacer(Modifier.height(24.dp))
            WeeklyChart(weeks)
            Spacer(Modifier.height(30.dp))
            TripList(activities)
        }

        if (showImportModal) {
            ImportModal(
                onClose = { showImportModal = false },
                onImport = { bytes, filename ->
                    api.importZip(bytes, filename)
                    showImportModal = false
                    scope.launch { reload(year) }
                },
            )
        }
    }
}

@Composable
private fun DashboardHeader(year: Int, onYearChange: (Int) -> Unit, onImportClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Пробег", color = VelometrColors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onYearChange(year - 1) }) {
                Text("‹", color = VelometrColors.textFaint, fontSize = 16.sp)
            }
            Text(year.toString(), color = VelometrColors.text, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            TextButton(onClick = { onYearChange(year + 1) }) {
                Text("›", color = VelometrColors.textFaint, fontSize = 16.sp)
            }
            Spacer(Modifier.width(18.dp))
            OutlinedButton(
                onClick = onImportClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VelometrColors.text),
            ) {
                Text("Импорт данных", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HeroBlock(summary: YearSummaryDto) {
    Column(Modifier.fillMaxWidth()) {
        Text("Пробег за год", color = VelometrColors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatOneDecimal(summary.totalDistanceKm),
                color = VelometrColors.text,
                fontSize = 56.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.width(8.dp))
            Text("км", color = VelometrColors.textMuted, fontSize = 22.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
            HeroStat(summary.tripCount.toString(), "поездок")
            HeroStat(formatOneDecimal(summary.avgWeeklyDistanceKm), "км в среднем за неделю")
            HeroStat(formatOneDecimal(summary.longestTripKm), "км, самая длинная поездка")
        }
        Spacer(Modifier.height(26.dp))
        HorizontalDivider(color = VelometrColors.line)
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(value, color = VelometrColors.text, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
        Spacer(Modifier.height(3.dp))
        Text(label, color = VelometrColors.textMuted, fontSize = 12.sp)
    }
}

private val MONTH_LABELS = listOf(
    "янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек",
)

@Composable
private fun WeeklyChart(weeks: List<Double>) {
    val max = (weeks.maxOrNull() ?: 0.0).coerceAtLeast(0.001)
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxWidth()
            .background(VelometrColors.panel, RoundedCornerShape(12.dp))
            .padding(22.dp),
    ) {
        Text("Пробег по неделям", color = VelometrColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.horizontalScroll(scrollState).height(120.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            weeks.forEach { value ->
                val heightFraction = (value / max).coerceIn(0.03, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight(heightFraction)
                        .background(
                            color = if (value > max * 0.86) VelometrColors.accent else VelometrColors.contour,
                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                        ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            MONTH_LABELS.forEach { label ->
                Text(label, color = VelometrColors.textFaint, fontSize = 11.sp, modifier = Modifier.width(60.dp))
            }
        }
    }
}

@Composable
private fun TripList(activities: List<ActivityDto>) {
    Column(Modifier.fillMaxWidth()) {
        Text("Последние поездки", color = VelometrColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        val maxDistance = activities.maxOfOrNull { it.distanceKm } ?: 0.0
        activities.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { activity ->
                    TripCard(
                        activity = activity,
                        isPeak = activity.distanceKm > maxDistance * 0.8,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TripCard(activity: ActivityDto, isPeak: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(VelometrColors.panel, RoundedCornerShape(10.dp))
            .padding(vertical = 16.dp, horizontal = 18.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isPeak) VelometrColors.accent else VelometrColors.contour),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(activity.title ?: "Поездка", color = VelometrColors.text, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatShortDate(activity.date),
                        color = VelometrColors.textFaint,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    "${formatOneDecimal(activity.distanceKm)} км",
                    color = VelometrColors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = VelometrColors.line)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                TripStat(formatDuration(activity.durationSec), "длительность")
                TripStat(formatSpeed(activity.avgSpeed), "ср. скорость, км/ч")
                TripStat(formatSpeed(activity.maxSpeed), "макс. скорость, км/ч")
            }
        }
    }
}

@Composable
private fun TripStat(value: String, label: String) {
    Column {
        Text(value, color = VelometrColors.text, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = VelometrColors.textFaint, fontSize = 11.sp)
    }
}
