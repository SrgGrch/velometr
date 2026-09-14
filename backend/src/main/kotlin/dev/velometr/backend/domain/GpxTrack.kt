package dev.velometr.backend.domain

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Decodes a stored track_gpx BLOB (gzip-compressed, either a GPX file or Strava's raw FIT
 * format) into GPS points. Returns null - never throws - when there's nothing usable: no blob,
 * corrupt gzip, or decompressed bytes that aren't GPX (FIT parsing is out of scope).
 */
object GpxTrack {
    fun decode(trackBlob: ByteArray?): List<TrackPointDto>? {
        if (trackBlob == null || trackBlob.isEmpty()) return null
        val decompressed = runCatching {
            GZIPInputStream(ByteArrayInputStream(trackBlob)).use { it.readBytes() }
        }.getOrNull() ?: return null
        return parseGpx(decompressed)
    }

    private fun parseGpx(bytes: ByteArray): List<TrackPointDto>? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // This XML comes from an untrusted import archive - never resolve external entities/DTDs.
            isExpandEntityReferences = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val document = runCatching {
            factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        }.getOrNull() ?: return null

        val root = document.documentElement ?: return null
        if (!root.tagName.equals("gpx", ignoreCase = true)) return null

        val trkpts = document.getElementsByTagName("trkpt")
        val points = mutableListOf<TrackPointDto>()
        for (i in 0 until trkpts.length) {
            val attributes = trkpts.item(i).attributes ?: continue
            val lat = attributes.getNamedItem("lat")?.nodeValue?.toDoubleOrNull()
            val lon = attributes.getNamedItem("lon")?.nodeValue?.toDoubleOrNull()
            if (lat != null && lon != null) points += TrackPointDto(lat, lon)
        }
        return points
    }
}
