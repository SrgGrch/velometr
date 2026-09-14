package dev.velometr.frontend.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.velometr.frontend.data.ApiClient
import dev.velometr.frontend.data.TrackDto
import dev.velometr.frontend.presentation.VelometrColors
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

// OSM's own raster tile pyramid tops out around here; deeper levels just aren't published.
private const val MAX_ZOOM = 19
private const val MAP_SIZE = TILE_SIZE * (1 shl MAX_ZOOM)

/**
 * Opened from a trip card (see `DashboardScreen`): shows the activity's recorded GPS track as a
 * path over OSM tiles, or a "no track available" state when the backend reports none. [onClose]
 * is invoked from this composable's own close affordance, shared by both the extra-pane and
 * full-screen placements so "closing" behaves identically in either.
 */
@Composable
fun ActivityMapView(activityId: Long, api: ApiClient, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var track by remember(activityId) { mutableStateOf<TrackDto?>(null) }
    var loadFailed by remember(activityId) { mutableStateOf(false) }

    LaunchedEffect(activityId) {
        track = null
        loadFailed = false
        track = runCatching { api.track(activityId) }.getOrElse {
            loadFailed = true
            null
        }
    }

    Box(modifier.fillMaxSize().background(VelometrColors.background)) {
        val points = track?.points.orEmpty()
        when {
            track == null && !loadFailed -> Text(
                "Загрузка карты…",
                color = VelometrColors.textMuted,
                modifier = Modifier.align(Alignment.Center),
            )
            loadFailed || track?.available == false || points.isEmpty() -> Text(
                "Трек недоступен для этой поездки",
                color = VelometrColors.textMuted,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> TrackMap(points)
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
        ) {
            TextButton(onClick = onClose) {
                Text("Закрыть", color = VelometrColors.text)
            }
        }
    }
}

@Composable
private fun TrackMap(points: List<dev.velometr.frontend.data.TrackPointDto>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }

        val mapState = remember(points, viewportWidthPx, viewportHeightPx) {
            buildMapState(points, viewportWidthPx, viewportHeightPx)
        }

        MapUI(Modifier.fillMaxSize(), state = mapState)

        Text(
            OSM_ATTRIBUTION,
            color = VelometrColors.textMuted,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(VelometrColors.background),
        )
    }
}

private fun buildMapState(
    points: List<dev.velometr.frontend.data.TrackPointDto>,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): MapState {
    val normalized = points.map { latLonToNormalized(it.lat, it.lon) }
    val minX = normalized.minOf { it.first }
    val maxX = normalized.maxOf { it.first }
    val minY = normalized.minOf { it.second }
    val maxY = normalized.maxOf { it.second }
    val centerX = (minX + maxX) / 2.0
    val centerY = (minY + maxY) / 2.0

    // Fit the bounding box in the viewport with ~25% padding on each side; a degenerate
    // (single-point) bbox yields an infinite ratio here, which coerceIn below turns into 1.0.
    val padding = 0.8
    val scaleX = (viewportWidthPx * padding) / ((maxX - minX) * MAP_SIZE)
    val scaleY = (viewportHeightPx * padding) / ((maxY - minY) * MAP_SIZE)
    val scale = minOf(scaleX, scaleY).coerceIn(0.0001, 1.0)

    return MapState(levelCount = MAX_ZOOM + 1, fullWidth = MAP_SIZE, fullHeight = MAP_SIZE, workerCount = 16) {
        scroll(centerX, centerY)
        scale(scale)
    }.apply {
        addLayer(osmTileStreamProvider())
        addPath(id = "track", color = Color(0xFF448AFF)) {
            addPoints(normalized)
        }
    }
}
