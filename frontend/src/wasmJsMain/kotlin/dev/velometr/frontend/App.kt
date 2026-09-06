package dev.velometr.frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun App(api: ApiClient) {
    var authenticated by remember { mutableStateOf(false) }

    if (authenticated) {
        DashboardScreen(api = api)
    } else {
        LoginScreen(api = api, onSuccess = { authenticated = true })
    }
}
