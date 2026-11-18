package com.zentones.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import com.zentones.app.editor.AudioDecoder
import com.zentones.app.editor.EditJob
import com.zentones.app.tones.Library
import com.zentones.app.tones.ToneKind
import com.zentones.app.ui.AppState
import com.zentones.app.ui.LocalApp
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.NavTab
import com.zentones.app.ui.SheetController
import com.zentones.app.ui.SheetHost
import com.zentones.app.ui.Skins
import com.zentones.app.ui.screens.AppsScreen
import com.zentones.app.ui.screens.EditorSheet
import com.zentones.app.ui.screens.SettingsScreen
import com.zentones.app.ui.screens.StoreScreen
import com.zentones.app.ui.screens.TonesScreen
import com.zentones.app.ui.screens.UpdateSheet
import com.zentones.app.ui.screens.toast
import com.zentones.app.update.Updater
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Library.init(applicationContext)
        state = AppState(applicationContext)
        handleShare(intent)
        setContent { ZentonesApp(state) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::state.isInitialized) state.tick++
    }

    /** "Compartir → Zentones" desde cualquier app de música o archivos abre el editor. */
    private fun handleShare(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)?.let { state.pendingImport = it }
        }
    }
}

@androidx.compose.runtime.Composable
fun ZentonesApp(st: AppState) {
    val skin = Skins.byId(st.skinId)
    val dark = when (st.darkMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val pal = skin.palette(dark)
    val scheme = skin.colorScheme(dark)
    val view = LocalView.current
    val ctx = LocalContext.current
    SideEffect {
        val w = (view.context as Activity).window
        WindowCompat.getInsetsController(w, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    val sheets = remember { SheetController() }
    val haze = remember { HazeState() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) st.pendingImport = uri
    }

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSkin provides skin, LocalPal provides pal, LocalSheets provides sheets, LocalApp provides st) {
            val tabs = remember {
                listOf(
                    NavTab("Tonos", Icons.Outlined.MusicNote, Icons.Rounded.MusicNote),
                    NavTab("Tienda", Icons.Outlined.Storefront, Icons.Rounded.Storefront),
                    NavTab("Apps", Icons.Outlined.Apps, Icons.Rounded.Apps),
                    NavTab("Ajustes", Icons.Outlined.Settings, Icons.Rounded.Settings),
                )
            }
            val l0 = rememberLazyListState()
            val l1 = rememberLazyListState()
            val l2 = rememberLazyListState()
            val l3 = rememberLazyListState()
            val lists = listOf(l0, l1, l2, l3)
            val current = lists[st.tab]
            val collapsed by remember(current) {
                derivedStateOf { current.firstVisibleItemIndex > 0 || current.firstVisibleItemScrollOffset > 160 }
            }
            val blur by animateDpAsState(if (sheets.spec != null && Build.VERSION.SDK_INT >= 31) 16.dp else 0.dp, tween(260), label = "blur")

            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().then(if (blur > 0.5.dp) Modifier.blur(blur) else Modifier)) {
                    skin.Frame(tabs[st.tab].label, tabs, st.tab, { st.tab = it }, collapsed, haze) { pad ->
                        Crossfade(targetState = st.tab, animationSpec = tween(220), label = "tabs") { t ->
                            when (t) {
                                0 -> TonesScreen(l0, pad) { picker.launch(arrayOf("audio/*")) }
                                1 -> StoreScreen(l1, pad)
                                2 -> AppsScreen(l2, pad)
                                else -> SettingsScreen(l3, pad)
                            }
                        }
                    }
                }
                SheetHost(sheets)
            }

            // Audio importado (selector de archivos o "Compartir") → editor
            LaunchedEffect(st.pendingImport) {
                val uri = st.pendingImport ?: return@LaunchedEffect
                st.pendingImport = null
                toast(ctx, "Abriendo audio…")
                runCatching { withContext(Dispatchers.Default) { AudioDecoder.decode(ctx, uri) } }
                    .onSuccess { pcm ->
                        val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.take(40) ?: "Mi tono"
                        sheets.editJob = EditJob(pcm, name, if (pcm.seconds > 6) ToneKind.CALL else ToneKind.MESSAGE, "Importado")
                        sheets.show("Editor") { EditorSheet() }
                    }
                    .onFailure { toast(ctx, "No se pudo abrir el audio: ${it.message}") }
            }

            // Comprobar actualizaciones al abrir
            LaunchedEffect(Unit) {
                if (!st.autoUpdate) return@LaunchedEffect
                runCatching { Updater.check() }.getOrNull()?.takeIf { it.isNewer }?.let { r ->
                    st.update = r
                    if (sheets.spec == null) sheets.show("Actualización disponible") { UpdateSheet() }
                }
            }
        }
    }
}
