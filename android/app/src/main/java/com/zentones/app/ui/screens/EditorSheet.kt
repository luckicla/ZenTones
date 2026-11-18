package com.zentones.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.editor.AudioEdit
import com.zentones.app.editor.EditParams
import com.zentones.app.tones.Library
import com.zentones.app.tones.Previewer
import com.zentones.app.tones.ToneKind
import com.zentones.app.tones.Wav
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.Pos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

private fun fmt(s: Double): String {
    val m = (s / 60).toInt()
    val r = s - m * 60
    return "%d:%04.1f".format(m, r)
}

/** Editor de recorte: forma de onda con dos tiradores, fundidos, volumen y guardado. */
@Composable
fun EditorSheet() {
    val sheets = LocalSheets.current
    val job = sheets.editJob ?: return
    val skin = LocalSkin.current
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pcm = job.pcm
    val total = pcm.seconds.coerceAtLeast(0.1)
    val peaks = remember(job) { pcm.peaks(120) }

    val defEnd = if (job.kind == ToneKind.CALL) minOf(total, 30.0) else minOf(total, 3.0)
    var start by remember(job) { mutableFloatStateOf(0f) }
    var end by remember(job) { mutableFloatStateOf((defEnd / total).toFloat()) }
    var name by remember(job) { mutableStateOf(job.name) }
    var kind by remember(job) { mutableStateOf(job.kind) }
    var fadeIn by remember(job) { mutableStateOf(false) }
    var fadeOut by remember(job) { mutableStateOf(true) }
    var normalize by remember(job) { mutableStateOf(true) }
    var gain by remember(job) { mutableFloatStateOf(0f) }
    var saving by remember { mutableStateOf(false) }
    val playing by Previewer.playing.collectAsState()

    fun params() = EditParams(start * total, end * total, fadeIn, fadeOut, gain, normalize)

    Text(
        "${fmt(start * total)}  →  ${fmt(end * total)}   ·   ${"%.1f".format((end - start) * total)} s",
        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = pal.text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
    )

    // Forma de onda
    Box(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(120.dp)
            .clip(RoundedCornerShape(16.dp)).background(pal.surfaceHi),
    ) {
        var dragging by remember { mutableStateOf(0) } // 1 inicio, 2 fin
        Canvas(
            Modifier.fillMaxWidth().height(120.dp)
                .pointerInput(job) {
                    detectTapGestures { o ->
                        val f = (o.x / size.width).coerceIn(0f, 1f)
                        if (abs(f - start) < abs(f - end)) start = minOf(f, end - 0.01f) else end = maxOf(f, start + 0.01f)
                    }
                }
                .pointerInput(job) {
                    detectDragGestures(
                        onDragStart = { o ->
                            val f = o.x / size.width
                            dragging = if (abs(f - start) < abs(f - end)) 1 else 2
                        },
                        onDragEnd = { dragging = 0 },
                    ) { change, drag ->
                        change.consume()
                        val d = drag.x / size.width
                        if (dragging == 1) start = (start + d).coerceIn(0f, end - 0.01f)
                        else end = (end + d).coerceIn(start + 0.01f, 1f)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val bw = w / peaks.size
            peaks.forEachIndexed { i, p ->
                val x = i * bw
                val inSel = (i + 0.5f) / peaks.size in start..end
                val bh = (p * h * 0.85f).coerceAtLeast(2f)
                drawRoundRect(
                    color = if (inSel) pal.accent else pal.sub.copy(alpha = .35f),
                    topLeft = Offset(x + bw * 0.18f, (h - bh) / 2),
                    size = Size(bw * 0.64f, bh),
                    cornerRadius = CornerRadius(bw * 0.3f),
                )
            }
            // zonas fuera del recorte
            drawRect(pal.bg.copy(alpha = .45f), Offset.Zero, Size(start * w, h))
            drawRect(pal.bg.copy(alpha = .45f), Offset(end * w, 0f), Size(w - end * w, h))
            // tiradores
            listOf(start, end).forEach { f ->
                drawRect(pal.accent, Offset(f * w - 1.5.dp.toPx(), 0f), Size(3.dp.toPx(), h))
                drawCircle(pal.accent, 7.dp.toPx(), Offset(f * w, h / 2))
            }
        }
    }
    SheetNote("Arrastra cerca de un borde para moverlo, o toca para colocarlo.")
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skin.Chip("Usar entero", start == 0f && end == 1f) { start = 0f; end = 1f }
        skin.Chip("Primeros 30 s", false) { start = 0f; end = (30.0 / total).toFloat().coerceAtMost(1f) }
        skin.Chip("Primeros 3 s", false) { start = 0f; end = (3.0 / total).toFloat().coerceAtMost(1f) }
    }

    skin.Field(name, { name = it }, "Nombre del tono", null)

    skin.SectionTitle("Ajustes")
    skin.Item("Fundido de entrada", null, null, { skin.Toggle(fadeIn) { fadeIn = it } }, Pos.FIRST) { fadeIn = !fadeIn }
    skin.Item("Fundido de salida", null, null, { skin.Toggle(fadeOut) { fadeOut = it } }, Pos.MIDDLE) { fadeOut = !fadeOut }
    skin.Item("Normalizar volumen", "Lo deja al máximo sin distorsionar", null, { skin.Toggle(normalize) { normalize = it } }, Pos.LAST) { normalize = !normalize }

    Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Ganancia", color = pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("${if (gain >= 0) "+" else ""}${"%.0f".format(gain)} dB", color = pal.sub, fontSize = 14.sp)
    }
    Slider(
        value = gain, onValueChange = { gain = it }, valueRange = -12f..12f,
        colors = SliderDefaults.colors(thumbColor = pal.accent, activeTrackColor = pal.accent, inactiveTrackColor = pal.accentSoft),
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    skin.SectionTitle("Usar como")
    ChoiceItem("Tono de llamada", "Suena en bucle", kind == ToneKind.CALL, Pos.FIRST) { kind = ToneKind.CALL }
    ChoiceItem("Tono de mensaje", "Mejor si dura menos de 3 s", kind == ToneKind.MESSAGE, Pos.LAST) { kind = ToneKind.MESSAGE }

    SheetButtons {
        job.source?.let { src ->
            skin.Btn("Guardar audio original en Descargas", false, Modifier.fillMaxWidth()) {
                scope.launch {
                    runCatching { com.zentones.app.store.YouTube.saveToDownloads(ctx, src, name) }
                        .onSuccess { toast(ctx, "Guardado en $it") }
                        .onFailure { toast(ctx, "No se pudo guardar: ${it.message}") }
                }
            }
        }
        skin.Btn(if (playing == "editor") "Parar" else "Escuchar recorte", false, Modifier.fillMaxWidth()) {
            if (playing == "editor") { Previewer.stop(); return@Btn }
            scope.launch {
                val f = withContext(Dispatchers.Default) {
                    val out = File(ctx.cacheDir, "editor-preview.wav")
                    Wav.write(out, AudioEdit.apply(pcm, params()), pcm.sampleRate)
                    out
                }
                Previewer.playFile(f.absolutePath, "editor")
            }
        }
        skin.Btn(if (saving) "Guardando…" else "Guardar en mis tonos", true, Modifier.fillMaxWidth()) {
            if (saving) return@Btn
            saving = true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.Default) {
                        Library.addPcm(ctx, name, kind, AudioEdit.apply(pcm, params()), pcm.sampleRate, job.origin)
                    }
                }.onSuccess {
                    toast(ctx, "Guardado en Mis tonos")
                    sheets.editJob = null
                    sheets.close()
                }.onFailure { toast(ctx, "Error al guardar: ${it.message}") }
                saving = false
            }
        }
    }
}
