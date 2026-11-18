package com.zentones.app.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zentones.app.tones.BuiltinTones
import com.zentones.app.tones.LibTone
import com.zentones.app.tones.Library
import com.zentones.app.tones.Previewer
import com.zentones.app.tones.ToneKind
import com.zentones.app.tones.ToneRepo
import com.zentones.app.ui.LocalApp
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.Pos
import com.zentones.app.ui.Tints
import com.zentones.app.ui.posOf
import kotlinx.coroutines.launch

enum class Target(val label: String, val type: Int, val kind: ToneKind) {
    CALL("Tono de llamada", RingtoneManager.TYPE_RINGTONE, ToneKind.CALL),
    MSG("Notificaciones", RingtoneManager.TYPE_NOTIFICATION, ToneKind.MESSAGE),
    ALARM("Alarma", RingtoneManager.TYPE_ALARM, ToneKind.CALL),
}

@Composable
fun TonesScreen(ls: LazyListState, pad: PaddingValues, onImport: () -> Unit) {
    val skin = LocalSkin.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val lib by Library.items.collectAsState()

    fun label(t: Target): String {
        val g = st.general(t.type)
        val sys = RingtoneManager.getActualDefaultRingtoneUri(ctx, t.type)?.toString()
        return if (g != null && g.second == sys) ToneRepo.name(g.first) ?: "—" else ToneRepo.systemTitle(ctx, t.type) ?: "Sistema"
    }
    val labels = remember(st.tick, lib) { Target.entries.associateWith { label(it) } }
    fun pick(t: Target) = sheets.show(t.label) { GeneralPicker(t) }

    LazyColumn(state = ls, contentPadding = pad, modifier = Modifier.fillMaxSize()) {
        item { skin.Header("Tonos", "Llamadas, mensajes y alarmas") }
        item {
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                skin.Tile("Llamadas", labels.getValue(Target.CALL), Icons.Rounded.Call, false, Modifier.weight(1f)) { pick(Target.CALL) }
                skin.Tile("Mensajes", labels.getValue(Target.MSG), Icons.Rounded.ChatBubble, true, Modifier.weight(1f)) { pick(Target.MSG) }
            }
        }
        item { skin.SectionTitle("Sistema") }
        item { skin.Item("Tono de llamada", labels[Target.CALL], { skin.Leading(Icons.Rounded.Call, Tints.Green) }, { skin.Chevron() }, Pos.FIRST) { pick(Target.CALL) } }
        item { skin.Item("Notificaciones", labels[Target.MSG], { skin.Leading(Icons.Rounded.NotificationsActive, Tints.Blue) }, { skin.Chevron() }, Pos.MIDDLE) { pick(Target.MSG) } }
        item { skin.Item("Alarma", labels[Target.ALARM], { skin.Leading(Icons.Rounded.Alarm, Tints.Orange) }, { skin.Chevron() }, Pos.LAST) { pick(Target.ALARM) } }

        item { skin.SectionTitle("Crear") }
        item {
            skin.Item("Crear tono desde un audio", "Recorta cualquier canción o sonido de tu móvil", { skin.Leading(Icons.Rounded.ContentCut, Tints.Pink) }, { skin.Chevron() }, Pos.SINGLE, onImport)
        }

        if (lib.isNotEmpty()) {
            item { skin.SectionTitle("Mis tonos") }
            itemsIndexed(lib, key = { _, t -> t.id }) { i, t ->
                skin.Item(
                    t.name, "${t.kind.label} · ${t.origin}",
                    { skin.Leading(Icons.Rounded.AudioFile, Tints.Purple) }, { PlayBadge(t.id) }, posOf(i, lib.size),
                ) { sheets.show(t.name) { MyToneSheet(t) } }
            }
        }

        item { skin.SectionTitle("De serie · Llamada") }
        itemsIndexed(BuiltinTones.calls, key = { _, t -> t.id }) { i, t ->
            skin.Item(t.name, t.desc, { skin.Leading(Icons.Rounded.MusicNote, Tints.Green) }, { PlayBadge(t.id) }, posOf(i, BuiltinTones.calls.size)) {
                Previewer.toggle(ctx, t.id)
            }
        }
        item { skin.SectionTitle("De serie · Mensaje") }
        itemsIndexed(BuiltinTones.messages, key = { _, t -> t.id }) { i, t ->
            skin.Item(t.name, t.desc, { skin.Leading(Icons.Rounded.GraphicEq, Tints.Blue) }, { PlayBadge(t.id) }, posOf(i, BuiltinTones.messages.size)) {
                Previewer.toggle(ctx, t.id)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Elegir y aplicar un tono general del sistema. */
@Composable
fun GeneralPicker(t: Target) {
    val skin = LocalSkin.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var sel by remember { mutableStateOf(st.general(t.type)?.first) }
    var busy by remember { mutableStateOf(false) }
    val canWrite = remember(st.tick) { Settings.System.canWrite(ctx) }

    TonePickerList(t.kind, sel, allowNone = false) { id ->
        sel = id
        if (id != null) Previewer.play(ctx, id)
    }
    if (!canWrite) SheetNote("Android pide un permiso especial para cambiar los tonos del sistema. Actívalo para Zentones y vuelve.")
    SheetButtons {
        if (!canWrite) {
            skin.Btn("Dar permiso", true, Modifier.fillMaxWidth()) {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }
        } else {
            skin.Btn(if (busy) "Aplicando…" else "Aplicar", true, Modifier.fillMaxWidth()) {
                val id = sel ?: return@Btn
                if (busy) return@Btn
                busy = true
                scope.launch {
                    runCatching {
                        val uri = ToneRepo.export(ctx, id, t.type)
                        RingtoneManager.setActualDefaultRingtoneUri(ctx, t.type, uri)
                        st.setGeneral(t.type, id, uri.toString())
                    }.onSuccess {
                        toast(ctx, "${t.label}: ${ToneRepo.name(id)}")
                        sheets.close()
                    }.onFailure { toast(ctx, "No se pudo aplicar: ${it.message}") }
                    busy = false
                }
            }
        }
        skin.Btn("Cancelar", false, Modifier.fillMaxWidth()) { sheets.close() }
    }
}

/** Opciones de un tono propio: renombrar, cambiar tipo, borrar. */
@Composable
fun MyToneSheet(t: LibTone) {
    val skin = LocalSkin.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(t.name) }
    var kind by remember { mutableStateOf(t.kind) }
    skin.Field(name, { name = it }, "Nombre", null)
    skin.SectionTitle("Usar como")
    ChoiceItem("Tono de llamada", null, kind == ToneKind.CALL, Pos.FIRST) { kind = ToneKind.CALL }
    ChoiceItem("Tono de mensaje", null, kind == ToneKind.MESSAGE, Pos.LAST) { kind = ToneKind.MESSAGE }
    SheetButtons {
        skin.Btn("Guardar", true, Modifier.fillMaxWidth()) {
            Library.update(ctx, t.id, name, kind); sheets.close()
        }
        skin.Btn("Escuchar", false, Modifier.fillMaxWidth()) { Previewer.toggle(ctx, t.id) }
        skin.Btn("Eliminar", false, Modifier.fillMaxWidth()) {
            Library.remove(ctx, t.id); sheets.close(); toast(ctx, "Tono eliminado")
        }
    }
}
