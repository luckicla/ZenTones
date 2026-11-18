package com.zentones.app.ui.skins

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.MiniMock
import com.zentones.app.ui.NavTab
import com.zentones.app.ui.Pal
import com.zentones.app.ui.Pos
import com.zentones.app.ui.Skin
import com.zentones.app.ui.glassOf
import com.zentones.app.ui.schemeFrom
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * One UI: cabecera grande centrada en el tercio superior (zona de lectura) y contenido abajo al alcance
 * del pulgar, tarjetas muy redondeadas con separadores, valores en azul y barra inferior flotante.
 */
object OneUiSkin : Skin {
    override val id = "oneui"
    override val name = "One UI"
    override val tagline = "Cabecera alta, todo al alcance"
    override val choiceAtEnd = false
    override val sheetAlignment = Alignment.BottomCenter

    @Composable
    override fun palette(dark: Boolean): Pal = if (dark) Pal(
        true, Color(0xFF000000), Color(0xFF171717), Color(0xFF252525), Color(0xFFFAFAFA), Color(0xFF9A9A9A),
        Color(0xFF5C9BFF), Color.White, Color(0xFF26324A), Color(0x1FFFFFFF),
    ) else Pal(
        false, Color(0xFFF2F2F2), Color.White, Color(0xFFE9E9EB), Color(0xFF010101), Color(0xFF7D7D7D),
        Color(0xFF387AFF), Color.White, Color(0xFFE3ECFF), Color(0x14000000),
    )

    @Composable override fun colorScheme(dark: Boolean): ColorScheme = schemeFrom(palette(dark))
    override fun glass(p: Pal): HazeStyle = glassOf(p, 0.6f, 26.dp)

    @Composable
    override fun Frame(
        title: String, tabs: List<NavTab>, selected: Int, onSelect: (Int) -> Unit,
        collapsed: Boolean, haze: HazeState, content: @Composable (PaddingValues) -> Unit,
    ) {
        val p = LocalPal.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val style = glass(p)
        Box(Modifier.fillMaxSize().background(p.bg)) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) { content(PaddingValues(top = top, bottom = bottom + 96.dp)) }

            val barH by animateDpAsState(if (collapsed) top + 56.dp else top, label = "bar")
            val a by animateFloatAsState(if (collapsed) 1f else 0f, label = "barA")
            Box(
                Modifier.fillMaxWidth().height(barH).hazeEffect(haze, style).padding(top = top).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = p.text, modifier = Modifier.graphicsLayer { alpha = a }) }

            // Barra flotante de cristal
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 14.dp).padding(horizontal = 26.dp)
                    .fillMaxWidth().height(66.dp)
                    .shadow(16.dp, RoundedCornerShape(33.dp), ambientColor = Color.Black.copy(alpha = .25f), spotColor = Color.Black.copy(alpha = .25f))
                    .clip(RoundedCornerShape(33.dp))
                    .hazeEffect(haze, glassOf(p.copy(bg = p.surface), 0.62f, 30.dp))
                    .border(0.5.dp, if (p.dark) Color(0x22FFFFFF) else Color(0x14000000), RoundedCornerShape(33.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { i, t ->
                    val sel = i == selected
                    val bg by animateColorAsState(if (sel) p.text.copy(alpha = if (p.dark) .14f else .08f) else Color.Transparent, label = "tb")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(27.dp)).background(bg)
                            .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Icon(if (sel) t.iconSel else t.icon, null, tint = if (sel) p.text else p.sub, modifier = Modifier.size(23.dp))
                        Text(t.label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) p.text else p.sub)
                    }
                }
            }
        }
    }

    @Composable
    override fun Header(title: String, subtitle: String?) {
        val p = LocalPal.current
        val h = LocalConfiguration.current.screenHeightDp.dp * 0.32f
        Box(Modifier.fillMaxWidth().height(h), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
                Text(title, fontSize = 38.sp, color = p.text, fontWeight = FontWeight.Normal)
                if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = p.sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp))
            }
        }
    }

    @Composable
    override fun SectionTitle(text: String) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalPal.current.sub, modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp))
    }

    @Composable
    override fun Item(
        title: String, subtitle: String?, leading: (@Composable () -> Unit)?, trailing: (@Composable () -> Unit)?,
        pos: Pos, onClick: (() -> Unit)?,
    ) {
        val p = LocalPal.current
        val r = 26.dp
        val shape = when (pos) {
            Pos.SINGLE -> RoundedCornerShape(r)
            Pos.FIRST -> RoundedCornerShape(topStart = r, topEnd = r)
            Pos.MIDDLE -> RoundedCornerShape(0.dp)
            Pos.LAST -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        }
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(shape).background(if (pressed) p.surfaceHi else p.surface)) {
            if (pos == Pos.MIDDLE || pos == Pos.LAST) {
                Box(Modifier.padding(start = if (leading != null) 74.dp else 24.dp, end = 24.dp).fillMaxWidth().height(0.7.dp).background(p.divider))
            }
            Row(
                Modifier.fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(src, null, onClick = onClick) else Modifier)
                    .padding(horizontal = 22.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) { leading(); Spacer(Modifier.width(14.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 17.sp, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = p.accent, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
                if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
            }
        }
    }

    @Composable
    override fun Leading(icon: ImageVector, tint: Color) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
    }

    @Composable
    override fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
        val p = LocalPal.current
        val off = if (p.dark) Color(0xFF3A3A3A) else Color(0xFFD5D5D7)
        val track by animateColorAsState(if (checked) p.accent else off, label = "trk")
        val x by animateDpAsState(if (checked) 22.dp else 0.dp, label = "x")
        Box(
            Modifier.width(50.dp).height(28.dp).clip(CircleShape).background(track)
                .clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
        ) {
            Box(Modifier.offset(x = x).size(28.dp).clip(CircleShape).background(track).padding(2.5.dp).clip(CircleShape).background(Color.White))
        }
    }

    @Composable
    override fun Btn(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val p = LocalPal.current
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        Box(
            modifier.scale(if (pressed) .98f else 1f).height(52.dp).clip(RoundedCornerShape(26.dp))
                .background(if (primary) p.accent else p.surfaceHi).clickable(src, null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (primary) Color.White else p.text) }
    }

    @Composable
    override fun Choice(selected: Boolean) {
        val p = LocalPal.current
        val c by animateColorAsState(if (selected) p.accent else p.sub, label = "rc")
        Box(Modifier.size(22.dp).border(2.dp, c, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(p.accent))
        }
    }

    @Composable
    override fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
        val p = LocalPal.current
        Box(
            Modifier.height(36.dp).clip(CircleShape)
                .background(if (selected) p.accent else p.surface)
                .border(1.dp, if (selected) p.accent else p.divider, CircleShape)
                .clickable(onClick = onClick).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else p.text) }
    }

    @Composable
    override fun SheetSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
        val p = LocalPal.current
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.86f
        Column(
            Modifier.padding(start = 10.dp, end = 10.dp, bottom = bottom + 10.dp).fillMaxWidth().heightIn(max = maxH)
                .clip(RoundedCornerShape(30.dp)).background(p.surface).padding(bottom = 12.dp),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = p.text, modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 12.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { content() }
        }
    }

    @Composable
    override fun Tile(title: String, value: String, icon: ImageVector, alt: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val p = LocalPal.current
        Column(modifier.clip(RoundedCornerShape(26.dp)).background(p.surface).clickable(onClick = onClick).padding(20.dp)) {
            Icon(icon, null, tint = if (alt) Color(0xFF2FB863) else p.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(20.dp))
            Text(title, fontSize = 14.sp, color = p.sub)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    override fun Banner(text: String, action: String, onClick: () -> Unit) {
        val p = LocalPal.current
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(p.surface).padding(22.dp)) {
            Text(text, fontSize = 15.sp, color = p.text)
            Spacer(Modifier.height(12.dp))
            Text(action, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.accent, modifier = Modifier.clickable(onClick = onClick))
        }
    }

    @Composable
    override fun Field(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector?) {
        val p = LocalPal.current
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                .background(p.surface).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = p.sub, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)) }
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = p.text, fontSize = 16.sp), cursorBrush = SolidColor(p.accent), modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = p.sub, fontSize = 16.sp); inner() } },
            )
        }
    }

    @Composable override fun Chevron() = Unit

    @Composable
    override fun MiniPreview(dark: Boolean, modifier: Modifier) =
        MiniMock(palette(dark), 12.dp, cards = true, centerHeader = true, topTabs = false, floatingNav = true, pill = true, modifier = modifier)
}
