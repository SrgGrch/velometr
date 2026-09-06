package dev.velometr.backend

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object Database {
    fun connect(path: String): Connection {
        File(path).absoluteFile.parentFile?.mkdirs()
        val conn = DriverManager.getConnection("jdbc:sqlite:$path")
        conn.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
        conn.createStatement().use { it.execute("PRAGMA busy_timeout=5000") }
        return conn
    }

    fun migrate(path: String) {
        connect(path).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS activities (
                      id            INTEGER PRIMARY KEY,
                      date          TEXT NOT NULL,
                      title         TEXT,
                      distance_km   REAL NOT NULL,
                      duration_sec  INTEGER NOT NULL,
                      avg_speed     REAL,
                      max_speed     REAL,
                      track_gpx     BLOB,
                      created_at    TEXT DEFAULT CURRENT_TIMESTAMP
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
