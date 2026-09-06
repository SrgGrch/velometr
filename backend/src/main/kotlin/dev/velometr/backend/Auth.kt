package dev.velometr.backend

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val authenticated: Boolean = true)

@Serializable
data class LoginRequest(val passcode: String)
