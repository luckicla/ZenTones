package com.zentones.app.ui.screens

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.AppRule
import com.zentones.app.tones.Previewer
import com.zentones.app.tones.ToneKind
import com.zentones.app.tones.ToneRepo
import com.zentones.app.ui.LocalApp
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.Pos
import com.zentones.app.ui.Tints
import com.zentones.app.ui.posOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(val pkg: String, val label: String)

private fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .sortedBy { it.label.lowercase() }
}

/**
 * Algunas apps eligen el tono dentro de sus propios ajustes en vez de en los de Android.
 * Para el resto se usa la guía genérica de categorías de notificación de Android.
 */
private class InAppGuide(val call: List<String>?, val msg: List<String>)

private val inAppGuides: Map<String, InAppGuide> = run {
    val wa = InAppGuide(
        call = listOf("Abre la app y toca ⋮ → Ajustes", "Entra en «Notificaciones»", "Baja a «Llamadas» y toca «Tono»"),
        msg = listOf("Abre la app y toca ⋮ → Ajustes", "Entra en «Notificaciones»", "En «Mensajes» toca «Tono de notificación»"),
    )
    val tg = InAppGuide(
        call = listOf("Abre la app → menú ☰ → Ajustes", "Entra en «Notificaciones y sonidos»", "En «Llamadas» toca «Tono»"),
        msg = listOf("Abre la app → menú ☰ → Ajustes", "Entra en «Notificaciones y sonidos»", "Toca «Chats privados» → «Sonido»"),
    )
    mapOf("com.whatsapp" to wa, "com.whatsapp.w4b" to wa, "org.telegram.messenger" to tg, "org.thunderdog.challegram" to tg)
}

private fun systemSteps(kind: ToneKind) = listOf(
    "Toca «Abrir ajustes de notificaciones» aquí abajo",
    if (kind == ToneKind.CALL) "Entra en la categoría de llamadas (suele llamarse «Llamadas» o «Llamadas entrantes»)"
    else "Entra en la categoría que quieras (por ejemplo «Mensajes» o «Chats»)",
    "Toca «Sonido»",
)

fun ruleSummary(r: AppRule?): String? {
    if (r == null) return null
    return listOfNotNull(
        r.call?.let { "Llamada: ${ToneRepo.name(it) ?: "?"}" },
        r.msg?.let { "Mensaje: ${ToneRepo.name(it) ?: "?"}" },
    ).joinToString(" · ").ifEmpty { null }
}

@Composable
fun AppsScreen(ls: LazyListState, pad: PaddingValues) {
    val skin = LocalSkin.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val apps by produceState(emptyList<AppInfo>()) { value = withContext(Dispatchers.IO) { loadApps(ctx) } }
    var q by rememberSaveable { mutableStateOf("") }
    val custom = remember(apps, st.rules) { apps.filter { st.rules.containsKey(it.pkg) } }
    val filtered = remember(apps, q) { apps.filter { q.isBlank() || it.label.contains(q, true) } }

    fun open(a: AppInfo) = sheets.show(a.label) { AppSheet(a) }

    LazyColumn(state = ls, contentPadding = pad, modifier = Modifier.fillMaxSize()) {
        item { skin.Header("Apps", "Elige un tono y te guiamos para ponerlo en cada app") }
        item { skin.Field(q, { q = it }, "Buscar apps", Icons.Rounded.Search) }
        if (custom.isNotEmpty() && q.isBlank()) {
            item { skin.SectionTitle("Ya configuradas") }
            itemsIndexed(custom, key = { _, a -> "c_" + a.pkg }) { i, a ->
                skin.Item(a.label, ruleSummary(st.rules[a.pkg]), { AppIcon(a.pkg) }, { skin.Chevron() }, posOf(i, custom.size)) { open(a) }
            }
        }
        item { skin.SectionTitle(if (q.isBlank()) "Todas las apps" else "Resultados") }
        if (apps.isEmpty()) item { SheetNote("Cargando apps…") }
        itemsIndexed(filtered, key = { _, a -> a.pkg }) { i, a ->
            skin.Item(a.label, ruleSummary(st.rules[a.pkg]), { AppIcon(a.pkg) }, { skin.Chevron() }, posOf(i, filtered.size)) { open(a) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Pasos de la hoja de una app */
private sealed interface Step {
    data object Overview : Step
    data class Pick(val kind: ToneKind) : Step
    data class Guide(val kind: ToneKind, val toneId: String) : Step
}

@Composable
fun AppSheet(app: AppInfo) {
    val skin = LocalSkin.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rule = st.rules[app.pkg] ?: AppRule(null, null)
    var step by remember { mutableStateOf<Step>(Step.Overview) }
    var preparing by remember { mutableStateOf(false) }

    AnimatedContent(targetState = step, label = "appSheet") { s ->
        Column {
            when (s) {
                Step.Overview -> {
                    SheetNote("Elige qué tono quieres para ${app.label}. Zentones lo deja listo en tu móvil y te dice dónde seleccionarlo.")
                    skin.Item("Tono de llamada", ToneRepo.name(rule.call) ?: "Sin elegir", { skin.Leading(Icons.Rounded.Call, Tints.Green) }, { skin.Chevron() }, Pos.FIRST) { step = Step.Pick(ToneKind.CALL) }
                    skin.Item("Tono de mensaje", ToneRepo.name(rule.msg) ?: "Sin elegir", { skin.Leading(Icons.Rounded.ChatBubble, Tints.Blue) }, { skin.Chevron() }, Pos.LAST) { step = Step.Pick(ToneKind.MESSAGE) }
                    SheetButtons {
                        if (st.rules.containsKey(app.pkg)) skin.Btn("Olvidar esta app", false, Modifier.fillMaxWidth()) {
                            st.setRule(app.pkg, null); sheets.close()
                        }
                        skin.Btn("Cerrar", true, Modifier.fillMaxWidth()) { sheets.close() }
                    }
                }

                is Step.Pick -> {
                    var sel by remember { mutableStateOf(if (s.kind == ToneKind.CALL) rule.call else rule.msg) }
                    TonePickerList(s.kind, sel, allowNone = false) { id ->
                        sel = id
                        if (id != null) Previewer.play(ctx, id)
                    }
                    SheetButtons {
                        skin.Btn(if (preparing) "Preparando…" else "Siguiente", true, Modifier.fillMaxWidth()) {
                            val id = sel ?: return@Btn
                            if (preparing) return@Btn
                            preparing = true
                            scope.launch {
                                runCatching {
                                    // Lo copiamos a Tonos / Notificaciones para que aparezca en el selector de sonidos
                                    ToneRepo.export(ctx, id, if (s.kind == ToneKind.CALL) RingtoneManager.TYPE_RINGTONE else RingtoneManager.TYPE_NOTIFICATION)
                                }.onSuccess {
                                    Previewer.stop()
                                    val r = if (s.kind == ToneKind.CALL) rule.copy(call = id) else rule.copy(msg = id)
                                    st.setRule(app.pkg, r)
                                    step = Step.Guide(s.kind, id)
                                }.onFailure { toast(ctx, "No se pudo preparar el tono: ${it.message}") }
                                preparing = false
                            }
                        }
                        skin.Btn("Atrás", false, Modifier.fillMaxWidth()) { Previewer.stop(); step = Step.Overview }
                    }
                }

                is Step.Guide -> {
                    val toneName = "Zentones · ${ToneRepo.name(s.toneId) ?: ""}"
                    val custom = inAppGuides[app.pkg]
                    val inApp = custom?.let { if (s.kind == ToneKind.CALL) it.call else it.msg }
                    val steps = (inApp ?: systemSteps(s.kind)) + "Elige «$toneName» en la lista"
                    SheetNote("Listo, el tono ya está en tu móvil. Ahora selecciónalo en ${app.label}:")
                    GuideSteps(steps)
                    if (inApp == null) SheetNote("Los nombres pueden cambiar un poco según la app y tu versión de Android.")
                    SheetButtons {
                        if (inApp != null) {
                            skin.Btn("Abrir ${app.label}", true, Modifier.fillMaxWidth()) {
                                ctx.packageManager.getLaunchIntentForPackage(app.pkg)?.let { ctx.startActivity(it) }
                            }
                        } else {
                            skin.Btn("Abrir ajustes de notificaciones", true, Modifier.fillMaxWidth()) {
                                runCatching {
                                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, app.pkg))
                                }.onFailure { toast(ctx, "No se pudieron abrir los ajustes") }
                            }
                        }
                        skin.Btn("Hecho", false, Modifier.fillMaxWidth()) { step = Step.Overview }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSteps(steps: List<String>) {
    val pal = LocalPal.current
    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { i, text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(pal.accentSoft), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = pal.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(text, color = pal.text, fontSize = 15.sp)
            }
        }
    }
}
