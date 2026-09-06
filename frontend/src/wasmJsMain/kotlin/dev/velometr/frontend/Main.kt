package dev.velometr.frontend

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val api = ApiClient()
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App(api)
    }
}
