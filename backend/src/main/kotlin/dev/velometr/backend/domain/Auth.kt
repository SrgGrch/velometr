package dev.velometr.backend.domain

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val passcode: String)

@Serializable
data class LoginResponse(val token: String)

/**
 * Issues and verifies the single-user auth token. No expiration is set: this
 * app has one shared passcode and no session-management requirements beyond
 * "logged in or not". The signing key is per-process (see Application.kt), so
 * a backend restart invalidates previously issued tokens.
 */
class JwtService(secret: ByteArray) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()

    fun issueToken(): String = JWT.create().withClaim(CLAIM_AUTHENTICATED, true).sign(algorithm)

    companion object {
        const val CLAIM_AUTHENTICATED = "authenticated"
    }
}
