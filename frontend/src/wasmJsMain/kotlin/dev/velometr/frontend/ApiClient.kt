package dev.velometr.frontend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The frontend performs no computation of its own: every value shown on the
 * dashboard is exactly what one of these calls returns from the backend.
 */
class ApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun login(passcode: String): Boolean {
        val response = client.post("/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(passcode))
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun yearSummary(year: Int): YearSummaryDto =
        client.get("/api/stats/$year/summary").body()

    suspend fun weeklyDistances(year: Int): WeeklyDistanceDto =
        client.get("/api/stats/$year/weekly").body()

    suspend fun activities(year: Int): List<ActivityDto> =
        client.get("/api/activities") { parameter("year", year) }.body()

    suspend fun importZip(bytes: ByteArray, filename: String): ImportStats {
        val response = client.post("/api/import") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            },
                        )
                    }
                )
            )
        }
        return response.body()
    }
}
