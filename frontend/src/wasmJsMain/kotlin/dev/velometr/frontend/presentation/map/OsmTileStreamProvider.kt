package dev.velometr.frontend.presentation.map

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readBuffer
import kotlinx.io.RawSource
import ovh.plrapps.mapcompose.core.TileStreamProvider

/** Attribution OpenStreetMap's tile usage policy requires whenever tiles from it are displayed. */
const val OSM_ATTRIBUTION = "© OpenStreetMap contributors"

private const val OSM_TILE_URL_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

/**
 * Fetches tiles directly from the given XYZ tile server (default: the public OSM tile server),
 * using a standalone [HttpClient] - deliberately not [dev.velometr.frontend.data.ApiClient]'s
 * client, which points at this app's own backend and attaches its auth header.
 */
fun osmTileStreamProvider(urlTemplate: String = OSM_TILE_URL_TEMPLATE): TileStreamProvider {
    val client = HttpClient()
    return TileStreamProvider { row, col, zoomLvl ->
        val url = urlTemplate
            .replace("{z}", zoomLvl.toString())
            .replace("{x}", col.toString())
            .replace("{y}", row.toString())
        runCatching {
            client.prepareGet(url).execute { response ->
                val channel: ByteReadChannel = response.body()
                channel.readBuffer() as RawSource
            }
        }.getOrNull()
    }
}
