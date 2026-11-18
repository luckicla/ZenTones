package com.zentones.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.editor.AudioDecoder
import com.zentones.app.editor.EditJob
import com.zentones.app.store.YouTube
import com.zentones.app.tones.ToneKind
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.Tints
import com.zentones.app.ui.posOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun dur(s: Double): String {
    if (s <= 0) return "—"
    val t = s.toInt()
    return if (t >= 3600) "%d:%02d:%02d".format(t / 3600, t / 60 % 60, t % 60) else "%d:%02d".format(t / 60, t % 60)
}

/** Buscar en YouTube (o pegar un enlace), bajar solo el audio y abrirlo en el editor. */
@Composable
fun YoutubeSheet(initial: String = "") {
    val skin = LocalSkin.current
    val pal = LocalPal.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var q by remember { mutableStateOf(initial) }
    var results by remember { mutableStateOf<List<YouTube.Video>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }

    fun search() {
        if (q.isBlank() || searching) return
        searching = true; error = null
        scope.launch {
            runCatching { YouTube.search(ctx, q) }
                .onSuccess { results = it; if (it.isEmpty()) error = "Sin resultados" }
                .onFailure { error = it.message?.lines()?.lastOrNull { l -> l.isNotBlank() } ?: "Error de yt-dlp" }
            searching = false
        }
    }

    fun get(v: YouTube.Video) {
        if (downloading != null) return
        downloading = v.id; progress = 0f
        scope.launch {
            runCatching {
                val f = YouTube.downloadAudio(ctx, v) { progress = it }
                f to withContext(Dispatchers.Default) { AudioDecoder.decode(f) }
            }.onSuccess { (file, pcm) ->
                sheets.editJob = EditJob(
                    pcm, v.title.take(40), if (pcm.seconds > 6) ToneKind.CALL else ToneKind.MESSAGE,
                    "YouTube · ${v.channel}", source = file,
                )
                sheets.show("Editor") { EditorSheet() }
            }.onFailure { toast(ctx, "No se pudo bajar el audio: ${it.message?.lines()?.lastOrNull { l -> l.isNotBlank() }}") }
            downloading = null
        }
    }

    LaunchedEffect(Unit) { if (initial.isNotBlank()) search() }
    DisposableEffect(Unit) { onDispose { if (downloading != null) YouTube.cancel() } }

    skin.Field(q, { q = it }, "Busca una canción o pega un enlace", Icons.Rounded.Search)
    SheetButtons {
        skin.Btn(if (searching) "Buscando…" else "Buscar", true, Modifier.fillMaxWidth()) { search() }
    }
    if (searching) SheetNote("La primera vez tarda un poco más: se prepara y actualiza yt-dlp.")
    error?.let { SheetNote(it) }

    results.forEachIndexed { i, v ->
        val busy = downloading == v.id
        skin.Item(
            v.title,
            if (busy) "Bajando audio… ${(progress * 100).toInt()} %" else "${v.channel} · ${dur(v.duration)}",
            { skin.Leading(Icons.Rounded.SmartDisplay, Tints.Red) },
            {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = pal.accent, strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.CloudDownload, "Bajar audio", tint = pal.accent)
                }
            },
            posOf(i, results.size),
        ) { get(v) }
    }
    if (results.isNotEmpty()) {
        Text(
            "Toca un vídeo para bajar solo el audio. Luego puedes cortarlo, usarlo entero o guardarlo en Descargas. Usa solo audio que tengas derecho a usar.",
            fontSize = 12.sp, color = pal.sub, fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}
