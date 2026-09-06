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
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File

fun Route.loginRoutes(config: AppConfig) {
    post("/api/login") {
        val request = call.receive<LoginRequest>()
        if (request.passcode == config.authPasscode) {
            call.sessions.set(UserSession())
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}

fun Route.importRoutes(importService: ImportService) {
    post("/api/import") {
        var tempFile: File? = null
        try {
            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem && tempFile == null) {
                    val file = File.createTempFile("velometr-import", ".zip")
                    file.outputStream().use { output -> part.provider().copyTo(output) }
                    tempFile = file
                }
                part.dispose()
            }
            val file = tempFile
            if (file == null) {
                call.respond(HttpStatusCode.BadRequest, "No file uploaded")
                return@post
            }
            val result = importService.importZip(file)
            call.respond(result)
        } finally {
            tempFile?.delete()
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
