package com.zentones.app.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.zentones.app.tones.Library
import com.zentones.app.tones.Previewer
import com.zentones.app.tones.ToneKind
import com.zentones.app.tones.ToneRepo
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.LocalSkin
import com.zentones.app.ui.posOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

@Composable
fun PlayBadge(key: String, busy: Boolean = false) {
    val ctx = LocalContext.current
    val pal = LocalPal.current
    val playing by Previewer.playing.collectAsState()
    val on = playing == key
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(if (on) pal.accent else pal.accentSoft)
            .clickable { Previewer.toggle(ctx, key) },
        contentAlignment = Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = pal.accent, strokeWidth = 2.dp)
        else Icon(if (on) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = if (on) pal.onAccent else pal.accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ChoiceItem(title: String, subtitle: String?, selected: Boolean, pos: com.zentones.app.ui.Pos, onClick: () -> Unit) {
    val skin = LocalSkin.current
    if (skin.choiceAtEnd) skin.Item(title, subtitle, null, { skin.Choice(selected) }, pos, onClick)
    else skin.Item(title, subtitle, { skin.Choice(selected) }, null, pos, onClick)
}

@Composable
fun SheetNote(text: String) {
    Text(text, fontSize = 13.sp, color = LocalPal.current.sub, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
}

@Composable
fun SheetButtons(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** Lista de tonos elegibles (mis tonos + de serie). allowNone = opción "Predeterminado". */
@Composable
fun TonePickerList(kind: ToneKind, selected: String?, allowNone: Boolean, onSelect: (String?) -> Unit) {
    val skin = LocalSkin.current
    val lib by Library.items.collectAsState()
    val list = remember(lib, kind) { ToneRepo.choices(kind) }
    val mine = list.filter { it.mine }
    val builtin = list.filterNot { it.mine }
    if (allowNone) ChoiceItem("Predeterminado de la app", "Zentones no sonará", selected == null, com.zentones.app.ui.Pos.SINGLE) { onSelect(null) }
    if (mine.isNotEmpty()) {
        skin.SectionTitle("Mis tonos")
        mine.forEachIndexed { i, t -> ChoiceItem(t.name, t.desc, selected == t.id, posOf(i, mine.size)) { onSelect(t.id) } }
    }
    skin.SectionTitle("De serie")
    builtin.forEachIndexed { i, t -> ChoiceItem(t.name, t.desc, selected == t.id, posOf(i, builtin.size)) { onSelect(t.id) } }
}

private val iconCache = HashMap<String, ImageBitmap>()

@Composable
fun AppIcon(pkg: String, size: Int = 40) {
    val ctx = LocalContext.current
    val bmp by produceState(iconCache[pkg], pkg) {
        if (value == null) value = withContext(Dispatchers.IO) {
            runCatching {
                val d: Drawable = ctx.packageManager.getApplicationIcon(pkg)
                d.toBitmap(128, 128).asImageBitmap().also { iconCache[pkg] = it }
            }.getOrNull()
        }
    }
    val b = bmp
    if (b != null) Image(b, null, Modifier.size(size.dp))
    else Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(LocalPal.current.surfaceHi))
}
