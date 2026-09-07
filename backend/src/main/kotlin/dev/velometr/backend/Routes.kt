package dev.velometr.backend

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val MAX_UPLOAD_BYTES = 500L * 1024 * 1024
internal class UploadTooLargeException : RuntimeException()

internal class LimitedUploadStream(private val delegate: OutputStream, private val limit: Long) : OutputStream() {
    private var written = 0L
    override fun write(value: Int) {
        if (written >= limit) throw UploadTooLargeException()
        delegate.write(value)
        written++
    }
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length.toLong() > limit - written) throw UploadTooLargeException()
        delegate.write(bytes, offset, length)
        written += length
    }
}

internal suspend fun <T> withUploadFile(block: suspend (File) -> T): T {
    val file = File.createTempFile("velometr-import", ".zip")
    return try { block(file) } finally { file.delete() }
}

fun Route.loginRoutes(config: AppConfig, jwtService: JwtService) {
    post("/api/login") {
        val request = call.receive<LoginRequest>()
        if (request.passcode == config.authPasscode) {
            call.respond(LoginResponse(jwtService.issueToken()))
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

fun Route.importRoutes(importService: ImportService, uploadLimit: Long = MAX_UPLOAD_BYTES) {
    post("/api/import") {
        try {
            withUploadFile { file ->
                var uploaded = false
                // Enforce our own byte limit so oversized uploads consistently return 413.
                call.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
                    try {
                        if (part is PartData.FileItem && !uploaded) {
                            file.outputStream().use { output ->
                                part.provider().copyTo(LimitedUploadStream(output, uploadLimit))
                            }
                            uploaded = true
                        }
                    } finally {
                        part.dispose()
                    }
                }
                if (!uploaded) {
                    call.respond(HttpStatusCode.BadRequest, "No file uploaded")
                } else {
                    val result = withContext(Dispatchers.IO) { importService.importZip(file) }
                    call.respond(result)
                }
            }
        } catch (e: UploadTooLargeException) {
            call.respond(HttpStatusCode.PayloadTooLarge, "Archive exceeds the 500 MiB upload limit")
        } catch (e: InvalidArchiveException) {
            call.respond(HttpStatusCode.BadRequest, e.message.orEmpty())
        }
    }
}

fun Route.activityRoutes(repository: ActivityRepository) {
    get("/api/activities") {
        val year = call.request.queryParameters["year"]?.toIntOrNull()
        if (year == null) {
            call.respond(HttpStatusCode.BadRequest, "year query parameter is required")
            return@get
        }
        call.respond(repository.listYear(year))
    }

    get("/api/stats/{year}/summary") {
        val year = call.parameters["year"]?.toIntOrNull()
        if (year == null) {
            call.respond(HttpStatusCode.BadRequest, "year path parameter must be numeric")
            return@get
        }
        call.respond(repository.yearSummary(year))
    }

    get("/api/stats/{year}/weekly") {
        val year = call.parameters["year"]?.toIntOrNull()
        if (year == null) {
            call.respond(HttpStatusCode.BadRequest, "year path parameter must be numeric")
            return@get
        }
        call.respond(WeeklyDistanceDto(year = year, weeks = repository.weeklyDistances(year)))
    }
}
