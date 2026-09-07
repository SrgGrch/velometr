package dev.velometr.backend

import dev.velometr.backend.data.ActivityRepository
import dev.velometr.backend.data.Database
import dev.velometr.backend.domain.ImportService
import dev.velometr.backend.domain.JwtService
import dev.velometr.backend.presentation.activityRoutes
import dev.velometr.backend.presentation.importRoutes
import dev.velometr.backend.presentation.loginRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.security.SecureRandom

fun main() {
    val config = loadConfig()
    Database.migrate(config.dataPath)
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig) {
    val repository = ActivityRepository(config.dataPath)
    val importService = ImportService(repository)

    // A fresh key each process start is fine here: this is a single-user app
    // with no requirement for tokens to survive a backend restart.
    val jwtSecret = ByteArray(32).also { SecureRandom().nextBytes(it) }
    val jwtService = JwtService(jwtSecret)

    install(ContentNegotiation) {
        json()
    }

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtService.verifier)
            validate { credential ->
                credential.payload.getClaim(JwtService.CLAIM_AUTHENTICATED).asBoolean()
                    ?.takeIf { it }
                    ?.let { JWTPrincipal(credential.payload) }
            }
            challenge { _, _ -> call.respond(io.ktor.http.HttpStatusCode.Unauthorized) }
        }
    }

    routing {
        get("/health") { call.respond(io.ktor.http.HttpStatusCode.OK) }

        loginRoutes(config, jwtService)

        authenticate("auth-jwt") {
            importRoutes(importService)
            activityRoutes(repository)
        }
    }
}
