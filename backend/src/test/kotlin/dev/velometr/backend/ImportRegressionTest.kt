package dev.velometr.backend

import dev.velometr.backend.data.ActivitiesCsv
import dev.velometr.backend.data.ActivityRepository
import dev.velometr.backend.data.Database
import dev.velometr.backend.data.ImportStats
import dev.velometr.backend.domain.ImportService
import dev.velometr.backend.domain.InvalidArchiveException
import dev.velometr.backend.domain.LoginResponse
import dev.velometr.backend.presentation.LimitedUploadStream
import dev.velometr.backend.presentation.UploadTooLargeException
import dev.velometr.backend.presentation.importRoutes
import dev.velometr.backend.presentation.withUploadFile
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class ImportRegressionTest {
    private val header = "Activity ID,Activity Date,Distance,Moving Time\n"
    private val row = "1,\"Jan 1, 2026, 9:00:00 AM\",1000,60\n"

    @Test
    fun `blank secrets are rejected before serving requests`() {
        for (secret in listOf("", " ", "\t\n")) {
            assertFailsWith<IllegalArgumentException> { AppConfig("unused.db", secret, 8080) }
        }
    }

    @Test
    fun `CSV is consumed lazily and validates mandatory headers`() {
        val text = header + row.repeat(10_000)
        var consumed = 0
        val reader = object : StringReader(text) {
            override fun read(buffer: CharArray, offset: Int, length: Int): Int =
                super.read(buffer, offset, length).also { if (it > 0) consumed += it }
        }
        reader.use {
            assertEquals(1L, ActivitiesCsv.parse(it).first().id)
            assertTrue(consumed < text.length, "first row must not require reading the entire CSV")
        }
        for (invalid in listOf("", "Activity ID,Activity Date\n", "something else\n")) {
            assertFailsWith<InvalidArchiveException> { ActivitiesCsv.parse(invalid) }
        }
    }

    @Test
    fun `stream failure rolls back earlier rows`() {
        val directory = Files.createTempDirectory("velometr-rollback-test").toFile()
        try {
            val path = File(directory, "test.db").path
            Database.migrate(path)
            val repository = ActivityRepository(path)
            val input = sequence {
                yield(ActivitiesCsv.parse(header + row).single() to null)
                throw IOException("failed reading next track")
            }
            assertFailsWith<IOException> { repository.importBatch(input) }
            assertTrue(repository.listYear(2026).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `failed copy removes partial upload`() = runBlocking {
        var partial: File? = null
        assertFailsWith<IOException> {
            withUploadFile { file ->
                partial = file
                file.writeBytes(byteArrayOf(1, 2, 3))
                throw IOException("disconnected")
            }
        }
        assertFalse(assertNotNull(partial).exists())
    }

    @Test
    fun `upload size guard accepts boundary and rejects excess without writing it`() {
        val output = ByteArrayOutputStream()
        val limited = LimitedUploadStream(output, 3)
        limited.write(byteArrayOf(1, 2))
        limited.write(3)
        assertFailsWith<UploadTooLargeException> { limited.write(4) }
        assertFailsWith<UploadTooLargeException> { limited.write(byteArrayOf(4, 5)) }
        assertContentEquals(byteArrayOf(1, 2, 3), output.toByteArray())
    }

    @Test
    fun `oversized multipart returns 413`() = testApplication {
        application {
            routing { importRoutes(ImportService(ActivityRepository("unused.db")), uploadLimit = 16) }
        }
        val response = client.submitFormWithBinaryData(
            "/api/import",
            formData { append("file", ByteArray(32), Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=\"export.zip\"")
            }) },
        )
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `large ZIP imports and invalid archives return validation errors`() = testApplication {
        val directory = Files.createTempDirectory("velometr-upload-test").toFile()
        try {
            val path = File(directory, "test.db").path
            Database.migrate(path)
            application { module(AppConfig(path, "secret", 0)) }
            val login = client.post("/api/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"passcode":"secret"}""")
            }
            val token = Json.decodeFromString<LoginResponse>(login.bodyAsText()).token

            suspend fun upload(bytes: ByteArray) = client.submitFormWithBinaryData(
                "/api/import",
                formData { append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"export.zip\"")
                }) },
            ) { header(HttpHeaders.Authorization, "Bearer $token") }

            val large = zip(header + row, padding = 51 * 1024 * 1024)
            assertTrue(large.size > 50 * 1024 * 1024)
            val response = upload(large)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, Json.decodeFromString<ImportStats>(response.bodyAsText()).imported)
            for (invalid in listOf(zip(null), zip("Wrong,Headers\n"), "not zip".toByteArray())) {
                val rejected = upload(invalid)
                assertEquals(HttpStatusCode.BadRequest, rejected.status)
                assertTrue(rejected.bodyAsText().isNotBlank())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun zip(csv: String?, padding: Int = 0): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            if (csv != null) {
                zip.putNextEntry(ZipEntry("activities.csv"))
                zip.write(csv.toByteArray())
                zip.closeEntry()
            }
            if (padding > 0) {
                val block = ByteArray(1024 * 1024)
                val crc = CRC32().apply { repeat(padding / block.size) { update(block) } }
                zip.putNextEntry(ZipEntry("padding.bin").apply {
                    method = ZipEntry.STORED
                    size = padding.toLong()
                    compressedSize = size
                    this.crc = crc.value
                })
                repeat(padding / block.size) { zip.write(block) }
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }
}
