package dev.velometr.backend

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.session
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

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
    // with no requirement to survive restarts already logged in.
    val sessionSecret = ByteArray(32).also { SecureRandom().nextBytes(it) }
    val sessionAuthKey = SecretKeySpec(sessionSecret, "HmacSHA256")

    install(ContentNegotiation) {
        json()
    }

    install(Sessions) {
        cookie<UserSession>("VELOMETR_SESSION") {
            cookie.httpOnly = true
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60L * 60 * 24 * 30
            transform(SessionTransportTransformerMessageAuthentication(sessionAuthKey.encoded))
        }
    }

    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session -> session.takeIf { it.authenticated } }
            challenge { call.respond(io.ktor.http.HttpStatusCode.Unauthorized) }
        }
    }

    routing {
        get("/health") { call.respond(io.ktor.http.HttpStatusCode.OK) }

        loginRoutes(config)

        authenticate("auth-session") {
            importRoutes(importService)
            activityRoutes(repository)
        }
    }
}
