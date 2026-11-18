package com.zentones.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.BuildConfig
import com.zentones.app.editor.AudioDecoder
import com.zentones.app.editor.EditJob
import com.zentones.app.store.Store
import com.zentones.app.store.StoreCategory
import com.zentones.app.store.StoreTone
import com.zentones.app.ui.LocalApp
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.posOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StoreScreen(ls: LazyListState, pad: PaddingValues) {
    val skin = LocalSkin.current
    val pal = LocalPal.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var cats by remember { mutableStateOf<List<StoreCategory>>(emptyList()) }
    var tones by remember { mutableStateOf<List<StoreTone>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var cat by rememberSaveable { mutableStateOf("all") }
    var kindFilter by rememberSaveable { mutableStateOf(0) } // 0 todos, 1 llamada, 2 mensaje
    var q by rememberSaveable { mutableStateOf("") }
    var fs by remember { mutableStateOf<List<StoreTone>>(emptyList()) }
    var fsLoading by remember { mutableStateOf(false) }
    var fsError by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var fsRetry by remember { mutableStateOf(0) }
    val fsKey = st.freesoundKey.ifBlank { BuildConfig.FREESOUND_KEY }

    suspend fun load(force: Boolean) {
        loading = true
        runCatching { Store.catalog(ctx, force) }
            .onSuccess { (c, t) -> cats = c; tones = t; error = null }
            .onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(Unit) { load(false) }

    // Búsqueda en Freesound con pequeño retardo mientras escribes
    LaunchedEffect(q, fsKey, fsRetry) {
        fsError = null
        if (q.length < 2) { fs = emptyList(); return@LaunchedEffect }
        if (fsKey.isBlank()) return@LaunchedEffect
        delay(450)
        fsLoading = true
        runCatching { Store.freesound(q, fsKey) }.onSuccess { fs = it }.onFailure { fsError = it.message }
        fsLoading = false
    }

    fun get(t: StoreTone) {
        if (downloading != null) return
        downloading = t.id
        scope.launch {
            runCatching {
                val f = Store.download(ctx, t)
                withContext(Dispatchers.Default) { AudioDecoder.decode(f) }
            }.onSuccess { pcm ->
                sheets.editJob = EditJob(pcm, t.name, t.kind, "${t.source} · ${t.author} · ${t.license}")
                sheets.show("Editar y guardar") { EditorSheet() }
            }.onFailure { toast(ctx, "No se pudo descargar: ${it.message}") }
            downloading = null
        }
    }

    val filtered = remember(tones, cat, kindFilter, q) {
        tones.filter { t ->
            (cat == "all" || t.category == cat) &&
                (kindFilter == 0 || (kindFilter == 1) == (t.kind == com.zentones.app.tones.ToneKind.CALL)) &&
                (q.isBlank() || t.name.contains(q, true) || cats.find { it.id == t.category }?.name?.contains(q, true) == true)
        }
    }

    @Composable
    fun ToneRow(t: StoreTone, i: Int, n: Int) {
        val catName = cats.find { it.id == t.category }?.name ?: t.source
        skin.Item(
            t.name,
            "${t.kind.label} · ${"%.1f".format(t.duration)} s · $catName" + if (t.source == "Freesound") " · ${t.author}" else "",
            { PlayBadge(t.url) },
            {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (downloading == t.id) CircularProgressIndicator(Modifier.size(20.dp), color = pal.accent, strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.CloudDownload, "Descargar", tint = pal.accent)
                }
            },
            posOf(i, n),
        ) { get(t) }
    }

    LazyColumn(state = ls, contentPadding = pad, modifier = Modifier.fillMaxSize()) {
        item { skin.Header("Tienda", "${tones.size} tonos en el catálogo · Freesound · YouTube") }
        item { skin.Field(q, { q = it }, "Busca: terror, retro, radio, láser…", Icons.Rounded.Search) }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                skin.Chip("Todo", kindFilter == 0) { kindFilter = 0 }
                skin.Chip("Llamadas", kindFilter == 1) { kindFilter = 1 }
                skin.Chip("Mensajes", kindFilter == 2) { kindFilter = 2 }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                skin.Chip("Todas", cat == "all") { cat = "all" }
                cats.forEach { c -> skin.Chip(c.name, cat == c.id) { cat = c.id } }
            }
        }

        if (q.length >= 2) {
            item { skin.SectionTitle("YouTube") }
            item {
                skin.Item("Buscar «$q» en YouTube", "Baja el audio y córtalo como quieras", { skin.Leading(Icons.Rounded.SmartDisplay, com.zentones.app.ui.Tints.Red) }, { skin.Chevron() }, com.zentones.app.ui.Pos.SINGLE) {
                    val query = q
                    sheets.show("YouTube") { YoutubeSheet(query) }
                }
            }
            item { skin.SectionTitle("Freesound") }
            when {
                fsKey.isBlank() -> item {
                    skin.Banner("Para buscar entre miles de sonidos de Freesound añade tu API key gratuita en Ajustes.", "Ir a Ajustes") { st.tab = 3 }
                }
                fsLoading -> item { Loading() }
                fsError != null -> item { skin.Banner("Freesound: $fsError", "Reintentar") { fsRetry++ } }
                fs.isEmpty() -> item { SheetNote("Sin resultados en Freesound") }
                else -> itemsIndexed(fs, key = { _, t -> t.id }) { i, t -> ToneRow(t, i, fs.size) }
            }
        }

        item { skin.SectionTitle(if (q.isBlank()) "Catálogo Zentones" else "En el catálogo") }
        when {
            loading -> item { Loading() }
            error != null && tones.isEmpty() -> item {
                skin.Banner("No pude cargar el catálogo ($error). Comprueba la conexión o que el repo ${BuildConfig.GITHUB_REPO} sea público.", "Reintentar") {
                    scope.launch { load(true) }
                }
            }
            filtered.isEmpty() -> item { SheetNote("Nada por aquí con esos filtros") }
            else -> itemsIndexed(filtered, key = { _, t -> t.id }) { i, t -> ToneRow(t, i, filtered.size) }
        }
        item {
            Text(
                "Al descargar un tono se abre el editor para recortarlo antes de guardarlo. Respeta la licencia de cada sonido.",
                fontSize = 12.sp, color = pal.sub, modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp).width(28.dp), color = LocalPal.current.accent)
    }
}
