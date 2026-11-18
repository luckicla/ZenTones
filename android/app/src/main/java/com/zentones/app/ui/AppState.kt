package com.zentones.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zentones.app.AppRule
import com.zentones.app.Prefs
import com.zentones.app.detectSkin
import com.zentones.app.editor.EditJob
import com.zentones.app.tones.Previewer
import com.zentones.app.update.Updater

class AppState(ctx: Context) {
    private val p = Prefs(ctx)

    /** true = el estilo sigue a la marca del móvil */
    var skinAuto by mutableStateOf(p.skin.isEmpty()); private set
    var skinId by mutableStateOf(p.skin.ifEmpty { detectSkin() }); private set
    var darkMode by mutableIntStateOf(p.darkMode); private set
    var autoUpdate by mutableStateOf(p.autoUpdate); private set
    var freesoundKey by mutableStateOf(p.freesoundKey); private set
    var rules by mutableStateOf(p.rules()); private set

    /** Se incrementa al volver a la app para refrescar el estado de permisos. */
    var tick by mutableIntStateOf(0)
    var update by mutableStateOf<Updater.Release?>(null)
    var pendingImport by mutableStateOf<Uri?>(null)
    var tab by mutableIntStateOf(0)

    fun setSkin(id: String) { skinAuto = false; skinId = id; p.skin = id }
    fun setSkinAuto() { skinAuto = true; skinId = detectSkin(); p.skin = "" }
    fun setDark(v: Int) { darkMode = v; p.darkMode = v }
    fun setAuto(v: Boolean) { autoUpdate = v; p.autoUpdate = v }
    fun setFsKey(v: String) { freesoundKey = v.trim(); p.freesoundKey = v }
    fun setRule(pkg: String, r: AppRule?) { p.setRule(pkg, r); rules = p.rules() }
    fun general(type: Int) = p.general(type)
    fun setGeneral(type: Int, id: String, uri: String) { p.setGeneral(type, id, uri); tick++ }
}

class SheetSpec(val title: String, val content: @Composable ColumnScope.() -> Unit)

class SheetController {
    var spec by mutableStateOf<SheetSpec?>(null); private set
    fun show(title: String, content: @Composable ColumnScope.() -> Unit) { spec = SheetSpec(title, content) }
    fun close() { spec = null; Previewer.stop() }

    /** El editor se abre como hoja; aquí se guarda el trabajo para que sobreviva a la recomposición. */
    var editJob by mutableStateOf<EditJob?>(null)
}

val LocalApp = staticCompositionLocalOf<AppState> { error("Sin estado") }
val LocalSheets = staticCompositionLocalOf<SheetController> { error("Sin hojas") }

@Composable
fun SheetHost(ctrl: SheetController) {
    val skin = LocalSkin.current
    val spec = ctrl.spec
    // Conservamos la última hoja para que se vea durante la animación de salida
    val last = remember { arrayOfNulls<SheetSpec>(1) }
    if (spec != null) last[0] = spec
    val shown = spec ?: last[0]
    val visible = spec != null
    BackHandler(enabled = visible) { ctrl.close() }

    AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
    }
    val center = skin.sheetAlignment == Alignment.Center
    AnimatedVisibility(
        visible,
        enter = if (center) fadeIn(tween(180)) + scaleIn(initialScale = 0.92f)
        else slideInVertically(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
        exit = if (center) fadeOut(tween(150)) + scaleOut(targetScale = 0.95f)
        else slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
    ) {
        Box(
            Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { ctrl.close() },
            contentAlignment = skin.sheetAlignment,
        ) {
            if (shown != null) {
                Box(Modifier.clickable(remember { MutableInteractionSource() }, null) {}) {
                    skin.SheetSurface(shown.title, shown.content)
                }
            }
        }
    }
}
