package dev.velometr.frontend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.serialization.json.Json

/** Thrown when the backend rejects the stored token (missing, invalid, or issued by a previous process). */
class UnauthorizedException : RuntimeException()
class ImportException(message: String) : RuntimeException(message)

private const val TOKEN_STORAGE_KEY = "velometr_token"

/**
 * The frontend performs no computation of its own: every value shown on the
 * dashboard is exactly what one of these calls returns from the backend.
 */
class ApiClient {
    // No expiry on the token itself, so a stored value survives page reloads
    // and browser restarts until the backend process (and its signing key) restarts.
    private var token: String? = runCatching { localStorage.getItem(TOKEN_STORAGE_KEY) }.getOrNull()

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // Ktor resolves scheme-less request URLs (e.g. "/api/login") against
        // http://localhost by default, not the page's actual origin (KTOR-2363)
        // - fatal when the app is opened via a LAN IP rather than localhost.
        defaultRequest {
            url {
                protocol = if (window.location.protocol == "https:") URLProtocol.HTTPS else URLProtocol.HTTP
                host = window.location.hostname
                window.location.port.toIntOrNull()?.let { port = it }
            }
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
            }
        }
    }

    fun isAuthenticated(): Boolean = token != null

    fun logout() {
        token = null
        runCatching { localStorage.removeItem(TOKEN_STORAGE_KEY) }
    }

    suspend fun login(passcode: String): Boolean {
        val response = client.post("/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(passcode))
        }
        if (response.status != HttpStatusCode.OK) return false
        val issued = response.body<LoginResponse>().token
        token = issued
        runCatching { localStorage.setItem(TOKEN_STORAGE_KEY, issued) }
        return true
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
        if (response.status == HttpStatusCode.PayloadTooLarge) {
            throw ImportException("Архив превышает лимит 500 МиБ")
        }
        if (response.status == HttpStatusCode.BadRequest) throw ImportException(response.bodyAsText())
        if (response.status != HttpStatusCode.OK) throw ImportException("Не удалось импортировать файл")
        return response.body()
    }
}
