package dev.velometr.frontend.presentation

import dev.velometr.frontend.data.ActivityDto
import dev.velometr.frontend.data.ApiClient
import dev.velometr.frontend.data.UnauthorizedException
import dev.velometr.frontend.data.YearSummaryDto
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Below this content width the layout switches to the mockup's mobile arrangement (single-column trip grid, stacked header). */
private val NARROW_BREAKPOINT = 620.dp

@Composable
fun DashboardScreen(api: ApiClient, onLoggedOut: () -> Unit) {
    var year by remember { mutableStateOf(currentYear()) }
    var data by remember(year) { mutableStateOf<DashboardData?>(null) }
    var loadError by remember(year) { mutableStateOf(false) }
    var refreshVersion by remember { mutableStateOf(0) }
    var showImportModal by remember { mutableStateOf(false) }
    LaunchedEffect(year, refreshVersion) {
        val requestedYear = year
        loadError = false
        try {
            val loaded = DashboardData(
                api.yearSummary(requestedYear),
                api.weeklyDistances(requestedYear).weeks,
                api.activities(requestedYear),
            )
            currentCoroutineContext().ensureActive()
            data = loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            api.logout()
            onLoggedOut()
        } catch (e: Exception) {
            loadError = true
        }
    }

    Box(Modifier.fillMaxSize().background(VelometrColors.background)) {
        BoxWithConstraints(modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 900.dp).fillMaxWidth()) {
            val isNarrow = maxWidth < NARROW_BREAKPOINT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(if (isNarrow) 16.dp else 24.dp),
            ) {
                DashboardHeader(
                    year = year,
                    isNarrow = isNarrow,
                    onYearChange = { year = it },
                    onImportClick = { showImportModal = true },
                )
                Spacer(Modifier.height(24.dp))
                if (loadError) {
                    Text("Не удалось загрузить данные", color = VelometrColors.accent)
                    TextButton(onClick = { refreshVersion++ }) { Text("Повторить") }
                }
                data?.let { loaded ->
                    HeroBlock(loaded.summary, isNarrow)
                    Spacer(Modifier.height(24.dp))
                    WeeklyChart(loaded.weeks)
                    Spacer(Modifier.height(30.dp))
                    TripList(loaded.activities, isNarrow)
                }
            }
        }

        if (showImportModal) {
            ImportModal(
                onClose = { showImportModal = false },
                onImport = { bytes, filename ->
                    try {
                        api.importZip(bytes, filename)
                        showImportModal = false
                        refreshVersion++
                    } catch (e: UnauthorizedException) {
                        api.logout()
                        showImportModal = false
                        onLoggedOut()
                    }
                },
            )
        }
    }
}

private data class DashboardData(
    val summary: YearSummaryDto,
    val weeks: List<Double>,
    val activities: List<ActivityDto>,
)

@Composable
private fun DashboardHeader(year: Int, isNarrow: Boolean, onYearChange: (Int) -> Unit, onImportClick: () -> Unit) {
    val yearControls: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onYearChange(year - 1) }) {
                Text("‹", color = VelometrColors.textFaint, fontSize = 16.sp)
            }
            Text(year.toString(), color = VelometrColors.text, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            TextButton(onClick = { onYearChange(year + 1) }) {
                Text("›", color = VelometrColors.textFaint, fontSize = 16.sp)
            }
            if (year != currentYear()) {
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = { onYearChange(currentYear()) }) {
                    Text("Текущий год", color = VelometrColors.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
    val importButton: @Composable () -> Unit = {
        OutlinedButton(
            onClick = onImportClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VelometrColors.text),
        ) {
            Text("Импорт данных", fontSize = 13.sp)
        }
    }

    if (isNarrow) {
        Column {
            Text("Пробег", color = VelometrColors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                yearControls()
                importButton()
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Пробег", color = VelometrColors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                yearControls()
                Spacer(Modifier.width(18.dp))
                importButton()
            }
        }
    }
}

@Composable
private fun HeroBlock(summary: YearSummaryDto, isNarrow: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Text("Пробег за год", color = VelometrColors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatOneDecimal(summary.totalDistanceKm),
                color = VelometrColors.text,
                fontSize = if (isNarrow) 42.sp else 56.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.width(8.dp))
            Text("км", color = VelometrColors.textMuted, fontSize = 22.sp)
        }
        Spacer(Modifier.height(20.dp))
        if (isNarrow) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                    HeroStat(summary.tripCount.toString(), "поездок")
                    HeroStat(formatOneDecimal(summary.avgWeeklyDistanceKm), "км в среднем за неделю")
                }
                HeroStat(formatOneDecimal(summary.longestTripKm), "км, самая длинная поездка")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                HeroStat(summary.tripCount.toString(), "поездок")
                HeroStat(formatOneDecimal(summary.avgWeeklyDistanceKm), "км в среднем за неделю")
                HeroStat(formatOneDecimal(summary.longestTripKm), "км, самая длинная поездка")
            }
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
    var selectedWeek by remember(weeks) { mutableStateOf<Int?>(null) }
    var hoveredWeek by remember(weeks) { mutableStateOf<Int?>(null) }
    val activeWeek = hoveredWeek ?: selectedWeek

    Column(
        Modifier
            .fillMaxWidth()
            .background(VelometrColors.panel, RoundedCornerShape(12.dp))
            .padding(22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Пробег по неделям", color = VelometrColors.textMuted, fontSize = 14.sp)
            Text(
                text = activeWeek?.let { "Неделя ${it + 1} · ${formatOneDecimal(weeks[it])} км" }.orEmpty(),
                color = VelometrColors.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.horizontalScroll(scrollState).height(120.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            weeks.forEachIndexed { index, value ->
                val heightFraction = (value / max).coerceIn(0.03, 1.0).toFloat()
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                LaunchedEffect(isHovered) {
                    if (isHovered) hoveredWeek = index else if (hoveredWeek == index) hoveredWeek = null
                }
                val isActive = index == activeWeek
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .hoverable(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            selectedWeek = if (selectedWeek == index) null else index
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .background(
                                color = if (isActive || value > max * 0.86) VelometrColors.accent else VelometrColors.contour,
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                            ),
                    )
                }
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
private fun TripList(activities: List<ActivityDto>, isNarrow: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Text("Последние поездки", color = VelometrColors.textMuted, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        val maxDistance = activities.maxOfOrNull { it.distanceKm } ?: 0.0
        val columns = if (isNarrow) 1 else 2
        activities.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { activity ->
                    TripCard(
                        activity = activity,
                        isPeak = activity.distanceKm > maxDistance * 0.8,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (columns == 2 && row.size == 1) Spacer(Modifier.weight(1f))
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
