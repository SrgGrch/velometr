package dev.velometr.frontend

import dev.velometr.frontend.data.ApiClient
import dev.velometr.frontend.presentation.App
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.events.WheelEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    allowNativePinchZoom()
    val api = ApiClient()
    ComposeViewport {
        App(api)
    }
}

/**
 * The Compose canvas swallows every wheel event to drive its own scrolling, which also
 * blocks Chrome's trackpad pinch-to-zoom on macOS (delivered as wheel events with
 * ctrlKey=true). Intercept those in the capture phase - before they reach the canvas -
 * so the canvas's own handler never sees them and never calls preventDefault, letting
 * the browser zoom normally. Plain scrolling (ctrlKey=false) is untouched.
 */
private fun allowNativePinchZoom() {
    document.addEventListener(
        "wheel",
        { event -> if ((event as WheelEvent).ctrlKey) event.stopPropagation() },
        true,
    )
}
