package com.zentones.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.BuildConfig
import com.zentones.app.brandName
import com.zentones.app.detectSkin
import com.zentones.app.ui.LocalApp
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSheets
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.Pos
import com.zentones.app.ui.Skins
import com.zentones.app.ui.Tints
import com.zentones.app.update.Updater
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(ls: LazyListState, pad: PaddingValues) {
    val skin = LocalSkin.current
    val pal = LocalPal.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Toca para comprobar") }

    val canWrite = remember(st.tick) { Settings.System.canWrite(ctx) }
    val canInstall = remember(st.tick) { ctx.packageManager.canRequestPackageInstalls() }

    @Composable
    fun StateLabel(ok: Boolean) = Text(
        if (ok) "Activo" else "Activar", fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = if (ok) pal.sub else pal.accent,
    )

    LazyColumn(state = ls, contentPadding = pad, modifier = Modifier.fillMaxSize()) {
        item { skin.Header("Ajustes", null) }

        item { skin.SectionTitle("Estilo de interfaz") }
        item {
            ChoiceItem(
                "Según mi móvil",
                "${brandName()} → ${Skins.byId(detectSkin()).name}",
                st.skinAuto, Pos.SINGLE,
            ) { st.setSkinAuto() }
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Skins.all.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        pair.forEach { s ->
                            val sel = !st.skinAuto && s.id == skin.id
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                                    .border(if (sel) 2.5.dp else 1.dp, if (sel) pal.accent else pal.divider, RoundedCornerShape(22.dp))
                                    .background(if (sel) pal.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { st.setSkin(s.id) }.padding(10.dp),
                            ) {
                                s.MiniPreview(pal.dark, Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pal.text, modifier = Modifier.weight(1f))
                                    if (sel) Box(Modifier.size(8.dp).clip(CircleShape).background(pal.accent))
                                }
                                Text(s.tagline, fontSize = 12.sp, color = pal.sub)
                            }
                        }
                    }
                }
            }
        }

        item { skin.SectionTitle("Apariencia") }
        item { ChoiceItem("Automático", "Sigue al sistema", st.darkMode == 0, Pos.FIRST) { st.setDark(0) } }
        item { ChoiceItem("Claro", null, st.darkMode == 1, Pos.MIDDLE) { st.setDark(1) } }
        item { ChoiceItem("Oscuro", null, st.darkMode == 2, Pos.LAST) { st.setDark(2) } }

        item { skin.SectionTitle("Permisos") }
        item {
            skin.Item("Cambiar tonos del sistema", "Para los tonos generales", { skin.Leading(Icons.Rounded.Tune, Tints.Orange) }, { StateLabel(canWrite) }, Pos.FIRST) {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }
        }
        item {
            skin.Item("Instalar actualizaciones", "Para actualizar desde GitHub", { skin.Leading(Icons.Rounded.Download, Tints.Green) }, { StateLabel(canInstall) }, Pos.LAST) {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")))
            }
        }

        item { skin.SectionTitle("Tienda") }
        item {
            skin.Item(
                "API key de Freesound",
                if (st.freesoundKey.isNotBlank()) "Configurada" else if (BuildConfig.FREESOUND_KEY.isNotBlank()) "Incluida en la app" else "Sin configurar · gratis en freesound.org",
                { skin.Leading(Icons.Rounded.Key, Tints.Purple) }, { skin.Chevron() }, Pos.SINGLE,
            ) { sheets.show("Freesound") { FreesoundKeySheet() } }
        }

        item { skin.SectionTitle("Actualizaciones") }
        item { skin.Item("Versión", BuildConfig.VERSION_NAME, { skin.Leading(Icons.Rounded.Info, Tints.Gray) }, null, Pos.FIRST, null) }
        item {
            skin.Item("Buscar actualizaciones", status, { skin.Leading(Icons.Rounded.SystemUpdate, Tints.Teal) }, { skin.Chevron() }, Pos.MIDDLE) {
                status = "Comprobando…"
                scope.launch {
                    runCatching { Updater.check() }
                        .onSuccess { r ->
                            if (r.isNewer) {
                                st.update = r
                                status = "Nueva versión ${r.version}"
                                sheets.show("Actualización disponible") { UpdateSheet() }
                            } else status = "Estás al día (${BuildConfig.VERSION_NAME})"
                        }
                        .onFailure { status = it.message ?: "No se pudo comprobar" }
                }
            }
        }
        item {
            skin.Item("Buscar al abrir", "Mira GitHub cada vez que abres la app", { skin.Leading(Icons.Rounded.Refresh, Tints.Blue) }, { skin.Toggle(st.autoUpdate) { st.setAuto(it) } }, Pos.MIDDLE) {
                st.setAuto(!st.autoUpdate)
            }
        }
        item {
            skin.Item("Repositorio", Updater.repo, { skin.Leading(Icons.Rounded.Code, Tints.Gray) }, { skin.Chevron() }, Pos.LAST) {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Updater.repoUrl)))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun FreesoundKeySheet() {
    val skin = LocalSkin.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    var key by remember { mutableStateOf(st.freesoundKey) }
    SheetNote("Crea una cuenta gratis en freesound.org, ve a freesound.org/apiv2/apply y copia la clave (\"Client secret/Api key\").")
    skin.Field(key, { key = it }, "Pega aquí tu API key", Icons.Rounded.Key)
    SheetButtons {
        skin.Btn("Guardar", true, Modifier.fillMaxWidth()) { st.setFsKey(key); sheets.close(); toast(ctx, "Clave guardada") }
        skin.Btn("Abrir freesound.org", false, Modifier.fillMaxWidth()) {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://freesound.org/apiv2/apply")))
        }
    }
}

@Composable
fun UpdateSheet() {
    val skin = LocalSkin.current
    val pal = LocalPal.current
    val st = LocalApp.current
    val sheets = LocalSheets.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rel = st.update ?: return
    var progress by remember { mutableFloatStateOf(-1f) }
    var apk by remember { mutableStateOf<java.io.File?>(null) }

    Text(
        "Zentones ${rel.version}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = pal.text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
    Text("Tienes la ${BuildConfig.VERSION_NAME}", fontSize = 14.sp, color = pal.sub, modifier = Modifier.padding(horizontal = 24.dp))
    if (rel.notes.isNotBlank()) {
        Text(
            rel.notes.take(1500), fontSize = 14.sp, color = pal.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)).background(pal.surfaceHi).padding(16.dp),
        )
    }
    if (progress >= 0f) {
        Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(pal.accentSoft)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(pal.accent))
        }
    }
    SheetButtons {
        skin.Btn(
            when {
                progress in 0f..0.999f -> "Descargando ${(progress * 100).toInt()} %"
                progress >= 1f -> "Instalar"
                else -> "Descargar e instalar"
            },
            true, Modifier.fillMaxWidth(),
        ) {
            val url = rel.apkUrl
            if (url == null) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.pageUrl))); return@Btn }
            if (progress in 0f..0.999f) return@Btn
            apk?.let { f ->
                if (!Updater.install(ctx, f)) toast(ctx, "Permite instalar apps de Zentones y vuelve a pulsar Instalar")
                return@Btn
            }
            scope.launch {
                runCatching {
                    progress = 0f
                    val f = Updater.download(ctx, url) { progress = it }
                    progress = 1f
                    apk = f
                    if (!Updater.install(ctx, f)) toast(ctx, "Permite instalar apps de Zentones y vuelve a pulsar Instalar")
                }.onFailure { progress = -1f; toast(ctx, "Error: ${it.message}") }
            }
        }
        skin.Btn("Más tarde", false, Modifier.fillMaxWidth()) { sheets.close() }
    }
}
