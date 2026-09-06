package dev.velometr.backend

import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApplicationTest {

    private fun tempDbPath(): String {
        val dir = Files.createTempDirectory("velometr-app-test")
        return dir.resolve("velometr.db").toString()
    }

    private fun config(dbPath: String) = AppConfig(dataPath = dbPath, authPasscode = "secret123", port = 0)

    @Test
    fun `health check responds without authentication`() = testApplication {
        val dbPath = tempDbPath()
        Database.migrate(dbPath)
        application { module(config(dbPath)) }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login succeeds with the correct passcode and fails with an incorrect one`() = testApplication {
        val dbPath = tempDbPath()
        Database.migrate(dbPath)
        application { module(config(dbPath)) }

        val ok = client.post("/api/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"passcode":"secret123"}""")
        }
        assertEquals(HttpStatusCode.OK, ok.status)

        val bad = client.post("/api/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"passcode":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, bad.status)
    }

    @Test
    fun `unauthenticated requests to protected routes are rejected`() = testApplication {
        val dbPath = tempDbPath()
        Database.migrate(dbPath)
        application { module(config(dbPath)) }

        val response = client.get("/api/activities?year=2026")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `authenticated stats endpoints return computed aggregates excluding track data`() = testApplication {
        val dbPath = tempDbPath()
        Database.migrate(dbPath)
        val repository = ActivityRepository(dbPath)
        repository.importBatch(
            listOf(
                ParsedActivity(1, "2026-01-05T10:00:00", "A", 10.0, 3600, 10.0, 15.0, null) to "gzipped-track-bytes".toByteArray(),
                ParsedActivity(2, "2026-06-10T10:00:00", "B", 40.0, 7200, 20.0, 30.0, null) to null,
            )
        )
        application { module(config(dbPath)) }
        val cookieClient = createClient { install(HttpCookies) }

        val login = cookieClient.post("/api/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"passcode":"secret123"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val summary = cookieClient.get("/api/stats/2026/summary")
        assertEquals(HttpStatusCode.OK, summary.status)
        val summaryDto = Json.decodeFromString<YearSummaryDto>(summary.bodyAsText())
        assertEquals(2, summaryDto.tripCount)
        assertEquals(50.0, summaryDto.totalDistanceKm, 1e-9)
        assertEquals(40.0, summaryDto.longestTripKm, 1e-9)

        val weekly = cookieClient.get("/api/stats/2026/weekly")
        assertEquals(HttpStatusCode.OK, weekly.status)
        val weeklyDto = Json.decodeFromString<WeeklyDistanceDto>(weekly.bodyAsText())
        assertEquals(52, weeklyDto.weeks.size)
        assertEquals(50.0, weeklyDto.weeks.sum(), 1e-9)

        val list = cookieClient.get("/api/activities?year=2026")
        assertEquals(HttpStatusCode.OK, list.status)
        val listBody = list.bodyAsText()
        val activities = Json.decodeFromString<List<ActivityDto>>(listBody)
        assertEquals(2, activities.size)
        assertFalse(listBody.contains("gzipped-track-bytes"), "list response must not include track data")
    }

    @Test
    fun `import endpoint accepts a multipart zip upload and imports its activities`() = testApplication {
        val dbPath = tempDbPath()
        Database.migrate(dbPath)
        application { module(config(dbPath)) }
        val cookieClient = createClient { install(HttpCookies) }
        cookieClient.post("/api/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"passcode":"secret123"}""")
        }

        val csv = buildString {
            appendLine("Activity ID,Activity Date,Activity Name,Distance,Moving Time,Max Speed,Average Speed,Filename")
            appendLine("1,\"Jan 1, 2026, 9:00:00 AM\",Morning ride,10000,3600,10,8,")
        }
        val zipBytes = ByteArrayOutputStream().also { buffer ->
            ZipOutputStream(buffer).use { zos ->
                zos.putNextEntry(ZipEntry("activities.csv"))
                zos.write(csv.toByteArray())
                zos.closeEntry()
            }
        }.toByteArray()

        val response = cookieClient.submitFormWithBinaryData(
            url = "/api/import",
            formData = formData {
                append("file", zipBytes, Headers.build {
                    append(HttpHeaders.ContentType, "application/zip")
                    append(HttpHeaders.ContentDisposition, "filename=\"export.zip\"")
                })
            }
        )
        assertEquals(HttpStatusCode.OK, response.status)
        val stats = Json.decodeFromString<ImportStats>(response.bodyAsText())
        assertEquals(1, stats.imported)
    }
}
