package dev.velometr.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader

/**
 * Only activities.csv is read on the backend; the note below mirrors the
 * mockup's copy so the user knows raw GPX files inside the zip aren't
 * uploaded separately - the whole archive goes up in one request.
 */
@Composable
fun ImportModal(onClose: () -> Unit, onImport: suspend (ByteArray, String) -> Unit) {
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Compose for wasmJs renders onto a single <canvas>, so there's no per-composable
    // DOM node to attach native drag events to via a Modifier. Instead we wire the
    // HTML5 drag-and-drop events directly onto the canvas element for as long as this
    // modal is in composition, and tear them down when it closes.
    DisposableEffect(Unit) {
        val canvas = document.getElementById("ComposeTarget") as? HTMLCanvasElement

        val onDragEnter: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            isDragOver = true
        }
        val onDragOver: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            isDragOver = true
        }
        val onDragLeave: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            isDragOver = false
        }
        val onDrop: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            isDragOver = false
            val file = event.dataTransfer?.files?.item(0)
            if (file == null) {
                // no-op: browsers can fire drop with an empty file list for non-file drags
            } else if (isZipFile(file)) {
                selectedFile = file
                error = null
            } else {
                error = "Нужен .zip архив"
            }
        }

        canvas?.ondragenter = onDragEnter
        canvas?.ondragover = onDragOver
        canvas?.ondragleave = onDragLeave
        canvas?.ondrop = onDrop

        onDispose {
            canvas?.ondragenter = null
            canvas?.ondragover = null
            canvas?.ondragleave = null
            canvas?.ondrop = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xB8080A09)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .background(VelometrColors.panel, RoundedCornerShape(14.dp))
                .padding(26.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text("Импорт данных", color = VelometrColors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) {
                    Text("✕", color = VelometrColors.textFaint, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Перетащи сюда архив выгрузки Strava (Download all your data) — новые поездки добавятся, повторы пропустятся автоматически.",
                color = VelometrColors.textMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(20.dp))

            val current = selectedFile
            if (current == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDragOver) VelometrColors.accent.copy(alpha = 0.08f) else Color.Transparent,
                            RoundedCornerShape(10.dp),
                        )
                        .border(
                            1.5.dp,
                            if (isDragOver) VelometrColors.accent else VelometrColors.line,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable {
                            pickZipFile { file -> selectedFile = file; error = null }
                        }
                        .padding(vertical = 34.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Перетащи архив сюда", color = VelometrColors.text, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("или нажми, чтобы выбрать файл — .zip", color = VelometrColors.textFaint, fontSize = 12.5.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VelometrColors.background, RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(current.name, color = VelometrColors.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${formatOneDecimal(current.size.toDouble() / 1024.0 / 1024.0)} МБ",
                            color = VelometrColors.textFaint,
                            fontSize = 12.sp,
                        )
                    }
                    TextButton(onClick = { selectedFile = null }) {
                        Text("✕", color = VelometrColors.textFaint, fontSize = 16.sp)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = VelometrColors.accent, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Обрабатывается только activities.csv из архива — исходные GPX-файлы не загружаются отдельно.",
                color = VelometrColors.textFaint,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val file = selectedFile ?: return@Button
                    if (submitting) return@Button
                    submitting = true
                    error = null
                    scope.launch {
                        try {
                            val bytes = file.readAllBytes()
                            onImport(bytes, file.name)
                        } catch (t: Throwable) {
                            error = "Не удалось импортировать файл"
                        } finally {
                            submitting = false
                        }
                    }
                },
                enabled = selectedFile != null && !submitting,
                colors = ButtonDefaults.buttonColors(containerColor = VelometrColors.accent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (submitting) "Импортируем..." else "Импортировать")
            }
        }
    }
}

private fun isZipFile(file: File): Boolean =
    file.name.endsWith(".zip", ignoreCase = true) ||
        file.type == "application/zip" ||
        file.type == "application/x-zip-compressed"

private fun pickZipFile(onPicked: (File) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".zip"
    input.onchange = {
        val file = input.files?.item(0)
        if (file != null) onPicked(file)
    }
    input.click()
}

private suspend fun File.readAllBytes(): ByteArray {
    val deferred = CompletableDeferred<ByteArray>()
    val reader = FileReader()
    reader.onload = {
        val buffer = reader.result as ArrayBuffer
        deferred.complete(Int8Array(buffer).toByteArray())
    }
    reader.onerror = { deferred.completeExceptionally(RuntimeException("failed to read file")) }
    reader.readAsArrayBuffer(this)
    return deferred.await()
}
