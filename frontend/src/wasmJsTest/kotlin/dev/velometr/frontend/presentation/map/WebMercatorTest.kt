package dev.velometr.frontend.presentation.map

import kotlin.test.Test
import kotlin.test.assertEquals

class WebMercatorTest {
    private val epsilon = 1e-6

    @Test
    fun equatorAndPrimeMeridianProjectToTheCenter() {
        val (x, y) = latLonToNormalized(lat = 0.0, lon = 0.0)
        assertEquals(0.5, x, epsilon)
        assertEquals(0.5, y, epsilon)
    }

    @Test
    fun longitudeExtremesProjectToTheHorizontalEdges() {
        assertEquals(0.0, latLonToNormalized(lat = 0.0, lon = -180.0).first, epsilon)
        assertEquals(1.0, latLonToNormalized(lat = 0.0, lon = 180.0).first, epsilon)
    }

    @Test
    fun webMercatorLatitudeLimitsProjectToTheVerticalEdges() {
        // +/-85.0511287798 deg is the well-known Web Mercator latitude limit: by definition, the
        // latitude at which the projected y coordinate reaches the top/bottom of the square map
        // (see the OSM wiki's "Slippy map tilenames" page). This is an independent mathematical
        // fact about the projection, not something derived from this file's own implementation.
        val north = latLonToNormalized(lat = 85.0511287798, lon = 0.0).second
        val south = latLonToNormalized(lat = -85.0511287798, lon = 0.0).second
        assertEquals(0.0, north, 1e-4)
        assertEquals(1.0, south, 1e-4)
    }

    @Test
    fun projectsToExpectedPixelCoordinatesAtAFixedZoomLevel() {
        val zoom = 2
        val mapSize = mapSizeAtZoom(zoom)
        assertEquals(1024, mapSize) // 256 * 2^2

        val (nx, ny) = latLonToNormalized(lat = 0.0, lon = 0.0)
        assertEquals(512.0, normalizedToPixel(nx, mapSize), epsilon)
        assertEquals(512.0, normalizedToPixel(ny, mapSize), epsilon)

        val (nxEdge, _) = latLonToNormalized(lat = 0.0, lon = 180.0)
        assertEquals(1024.0, normalizedToPixel(nxEdge, mapSize), epsilon)
    }
}
