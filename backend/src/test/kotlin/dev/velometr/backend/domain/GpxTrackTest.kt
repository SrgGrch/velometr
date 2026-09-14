package dev.velometr.backend.domain

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GpxTrackTest {

    private fun gzip(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { buffer ->
            GZIPOutputStream(buffer).use { it.write(bytes) }
        }.toByteArray()

    private val sampleGpx = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="test">
          <trk>
            <trkseg>
              <trkpt lat="55.751244" lon="37.618423"></trkpt>
              <trkpt lat="55.752000" lon="37.619000"></trkpt>
            </trkseg>
          </trk>
        </gpx>
    """.trimIndent()

    @Test
    fun `decodes ordered points from a gzip-compressed GPX file`() {
        val blob = gzip(sampleGpx.toByteArray())

        val points = GpxTrack.decode(blob)

        assertEquals(
            listOf(TrackPointDto(55.751244, 37.618423), TrackPointDto(55.752000, 37.619000)),
            points,
        )
    }

    @Test
    fun `returns null for a gzip-compressed non-GPX (FIT-like) byte sequence`() {
        // Real FIT files start with a binary header, not valid GPX XML - a corrupt/binary
        // payload that fails XML parsing is representative of that case.
        val fitLikeBytes = byteArrayOf(0x0E, 0x10, 0x43, 0x08, 0x00, 0x00, 0x00, 0x00, 0x2E, 0x46, 0x49, 0x54)
        val blob = gzip(fitLikeBytes)

        assertNull(GpxTrack.decode(blob))
    }

    @Test
    fun `returns null for an absent track`() {
        assertNull(GpxTrack.decode(null))
    }

    @Test
    fun `returns null for an empty track blob`() {
        assertNull(GpxTrack.decode(ByteArray(0)))
    }
}
