package dev.velometr.frontend.presentation.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** Standard OSM/XYZ tile size in pixels. */
const val TILE_SIZE = 256

/** The pixel size (width == height) of the full Web Mercator square at [zoom]. */
fun mapSizeAtZoom(zoom: Int): Int = TILE_SIZE * 2.0.pow(zoom).toInt()

/**
 * Projects [lat]/[lon] (WGS84 degrees) into normalized Web Mercator coordinates in `[0.0, 1.0]`,
 * the coordinate space MapCompose's `addPath`/`addMarker` APIs expect directly - MapCompose scales
 * these by the `MapState`'s `fullWidth`/`fullHeight` internally.
 */
fun latLonToNormalized(lat: Double, lon: Double): Pair<Double, Double> {
    val latRad = lat * PI / 180.0
    val normalizedX = (lon + 180.0) / 360.0
    val normalizedY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0
    return normalizedX to normalizedY
}

/** Scales a normalized `[0.0, 1.0]` coordinate to a pixel offset within a [mapSize]-wide/tall square. */
fun normalizedToPixel(normalized: Double, mapSize: Int): Double = normalized * mapSize
