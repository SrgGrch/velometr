package dev.velometr.frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Vertical position (as a fraction of height) and opacity of one contour line, per the mockup's `.contour-field` SVG paths. */
private data class ContourLine(val yFraction: Float, val opacity: Float)

private val contourLines = listOf(
    ContourLine(yFraction = 0.24f, opacity = 0.55f),
    ContourLine(yFraction = 0.34f, opacity = 0.45f),
    ContourLine(yFraction = 0.44f, opacity = 0.35f),
    ContourLine(yFraction = 0.64f, opacity = 0.40f),
    ContourLine(yFraction = 0.74f, opacity = 0.30f),
    ContourLine(yFraction = 0.84f, opacity = 0.25f),
)

@Composable
private fun ContourFieldBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val amplitude = h * 0.045f
        contourLines.forEach { line ->
            val baseY = line.yFraction * h
            val path = Path().apply {
                moveTo(-0.05f * w, baseY)
                cubicTo(
                    0.20f * w, baseY - amplitude,
                    0.36f * w, baseY + amplitude,
                    0.52f * w, baseY - amplitude * 0.6f,
                )
                cubicTo(
                    0.68f * w, baseY - amplitude * 1.4f,
                    0.86f * w, baseY - amplitude * 1.8f,
                    1.05f * w, baseY - amplitude * 0.5f,
                )
            }
            drawPath(
                path = path,
                color = VelometrColors.contour,
                // Mirrors the mockup: container opacity 0.5 combined with each path's own opacity.
                alpha = line.opacity * 0.5f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
fun LoginScreen(api: ApiClient, onSuccess: () -> Unit) {
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (submitting || passcode.isEmpty()) return
        submitting = true
        error = null
        scope.launch {
            val ok = runCatching { api.login(passcode) }.getOrDefault(false)
            submitting = false
            if (ok) onSuccess() else error = "Неверный код доступа"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(VelometrColors.background),
        contentAlignment = Alignment.Center,
    ) {
        ContourFieldBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(24.dp)) {
            Text(
                text = "Пробег",
                color = VelometrColors.text,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Личный трекер пробега.\nДоступ только для тебя.",
                color = VelometrColors.textMuted,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = passcode,
                onValueChange = {
                    passcode = it
                    error = null
                },
                label = { Text("Код доступа") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VelometrColors.text,
                    unfocusedTextColor = VelometrColors.text,
                    focusedBorderColor = VelometrColors.accent,
                    unfocusedBorderColor = VelometrColors.line,
                    focusedLabelColor = VelometrColors.textMuted,
                    unfocusedLabelColor = VelometrColors.textMuted,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error.orEmpty(), color = VelometrColors.accent, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { submit() },
                enabled = !submitting,
                colors = ButtonDefaults.buttonColors(containerColor = VelometrColors.accent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (submitting) "Вход..." else "Войти")
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Данные синхронизируются из Strava-экспорта",
                color = VelometrColors.textFaint,
                fontSize = 12.sp,
            )
        }
    }
}
