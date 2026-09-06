package dev.velometr.backend

data class AppConfig(
    val dataPath: String,
    val authPasscode: String,
    val port: Int,
)

fun loadConfig(): AppConfig {
    val dataPath = System.getenv("DATA_PATH")
        ?: error("DATA_PATH environment variable is required")
    val authPasscode = System.getenv("AUTH_PASSCODE")
        ?: error("AUTH_PASSCODE environment variable is required")
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    return AppConfig(dataPath = dataPath, authPasscode = authPasscode, port = port)
}
