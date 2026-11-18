package com.zentones.app.ui.skins

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
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
import com.zentones.app.ui.Tints
import com.zentones.app.ui.glassOf
import com.zentones.app.ui.schemeFrom
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** HyperOS: títulos enormes a la izquierda, tarjetas blancas, iconos cuadrados de color, azul Xiaomi, rebote al pulsar. */
object HyperSkin : Skin {
    override val id = "hyper"
    override val name = "HyperOS"
    override val tagline = "Aireado, rebote y azul"
    override val choiceAtEnd = true
    override val sheetAlignment = Alignment.BottomCenter

    @Composable
    override fun palette(dark: Boolean): Pal = if (dark) Pal(
        true, Color(0xFF000000), Color(0xFF1A1A1A), Color(0xFF262626), Color.White, Color(0x99FFFFFF),
        Color(0xFF3482FF), Color.White, Color(0x333482FF), Color(0x14FFFFFF),
    ) else Pal(
        false, Color(0xFFF7F7F7), Color.White, Color(0xFFF0F0F0), Color.Black, Color(0x99000000),
        Color(0xFF0D84FF), Color.White, Color(0x1A0D84FF), Color(0x0F000000),
    )

    @Composable override fun colorScheme(dark: Boolean): ColorScheme = schemeFrom(palette(dark))
    override fun glass(p: Pal): HazeStyle = glassOf(p, 0.78f, 32.dp)

    @Composable
    private fun bounce(): Pair<MutableInteractionSource, Float> {
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val s by animateFloatAsState(
            if (pressed) 0.96f else 1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "bounce",
        )
        return src to s
    }

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
            Box(Modifier.fillMaxSize().hazeSource(haze)) { content(PaddingValues(top = top, bottom = bottom + 72.dp)) }

            val barH by animateDpAsState(if (collapsed) top + 52.dp else top, label = "bar")
            val a by animateFloatAsState(if (collapsed) 1f else 0f, label = "barA")
            Box(
                Modifier.fillMaxWidth().height(barH).hazeEffect(haze, style).padding(top = top),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = p.text, modifier = Modifier.graphicsLayer { alpha = a })
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().hazeEffect(haze, style)) {
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
                Row(Modifier.fillMaxWidth().padding(bottom = bottom).height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                    tabs.forEachIndexed { i, t ->
                        val sel = i == selected
                        val (src, s) = bounce()
                        val c by animateColorAsState(if (sel) p.accent else p.text.copy(alpha = .45f), label = "tabc")
                        Column(
                            Modifier.weight(1f).scale(s).clickable(src, null) { onSelect(i) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(if (sel) t.iconSel else t.icon, null, tint = c, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(t.label, fontSize = 10.5.sp, color = c, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Header(title: String, subtitle: String?) {
        val p = LocalPal.current
        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 72.dp, bottom = 18.dp)) {
            Text(title, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = p.text, letterSpacing = (-0.5).sp)
            if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = p.sub, modifier = Modifier.padding(top = 4.dp))
        }
    }

    @Composable
    override fun SectionTitle(text: String) {
        Text(text, fontSize = 13.sp, color = LocalPal.current.sub, modifier = Modifier.padding(start = 30.dp, top = 22.dp, bottom = 8.dp))
    }

    @Composable
    override fun Item(
        title: String, subtitle: String?, leading: (@Composable () -> Unit)?, trailing: (@Composable () -> Unit)?,
        pos: Pos, onClick: (() -> Unit)?,
    ) {
        val p = LocalPal.current
        val r = 20.dp
        val shape = when (pos) {
            Pos.SINGLE -> RoundedCornerShape(r)
            Pos.FIRST -> RoundedCornerShape(topStart = r, topEnd = r)
            Pos.MIDDLE -> RoundedCornerShape(0.dp)
            Pos.LAST -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        }
        val (src, s) = bounce()
        val pressed by src.collectIsPressedAsState()
        Row(
            Modifier.padding(horizontal = 14.dp).fillMaxWidth().scale(s).clip(shape)
                .background(if (pressed) p.surfaceHi else p.surface)
                .then(if (onClick != null) Modifier.clickable(src, null, onClick = onClick) else Modifier)
                .padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) { leading(); Spacer(Modifier.width(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = p.text, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, fontSize = 12.5.sp, color = p.sub, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
        }
    }

    @Composable
    override fun Leading(icon: ImageVector, tint: Color) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tint), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }

    @Composable
    override fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
        val p = LocalPal.current
        val track by animateColorAsState(if (checked) p.accent else if (p.dark) Color(0x33FFFFFF) else Color(0xFFE5E5E5), label = "trk")
        val x by animateDpAsState(
            if (checked) 23.dp else 3.dp,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "thumb",
        )
        Box(
            Modifier.width(49.dp).height(28.dp).clip(CircleShape).background(track)
                .clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
        ) {
            Box(Modifier.offset(x = x, y = 3.dp).size(22.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White))
        }
    }

    @Composable
    override fun Btn(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val p = LocalPal.current
        val (src, s) = bounce()
        Box(
            modifier.scale(s).height(50.dp).clip(RoundedCornerShape(25.dp))
                .background(if (primary) p.accent else p.surfaceHi)
                .clickable(src, null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (primary) Color.White else p.text) }
    }

    @Composable
    override fun Choice(selected: Boolean) {
        val p = LocalPal.current
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = p.accent, modifier = Modifier.size(24.dp))
        }
    }

    @Composable
    override fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
        val p = LocalPal.current
        val (src, s) = bounce()
        Box(
            Modifier.scale(s).height(34.dp).clip(CircleShape).background(if (selected) p.accent else p.surface)
                .clickable(src, null, onClick = onClick).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) Color.White else p.text) }
    }

    @Composable
    override fun SheetSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
        val p = LocalPal.current
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.86f
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = bottom + 12.dp).fillMaxWidth().heightIn(max = maxH)
                .clip(RoundedCornerShape(32.dp)).background(p.surface).padding(top = 22.dp, bottom = 14.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = p.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { content() }
        }
    }

    @Composable
    override fun Tile(title: String, value: String, icon: ImageVector, alt: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val p = LocalPal.current
        val (src, s) = bounce()
        Column(modifier.scale(s).clip(RoundedCornerShape(24.dp)).background(p.surface).clickable(src, null, onClick = onClick).padding(18.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(if (alt) Tints.Orange else p.accent), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, fontSize = 13.sp, color = p.sub)
        }
    }

    @Composable
    override fun Banner(text: String, action: String, onClick: () -> Unit) {
        val p = LocalPal.current
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(p.accentSoft).clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontSize = 14.sp, color = p.accent, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(action, fontSize = 14.sp, color = p.accent, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    override fun Field(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector?) {
        val p = LocalPal.current
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp))
                .background(if (p.dark) Color(0xFF1F1F1F) else Color(0xFFEDEDED)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = p.sub, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)) }
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = p.text, fontSize = 15.sp), cursorBrush = SolidColor(p.accent), modifier = Modifier.weight(1f),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = p.sub, fontSize = 15.sp); inner() } },
            )
        }
    }

    @Composable
    override fun Chevron() {
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = LocalPal.current.sub.copy(alpha = .5f), modifier = Modifier.size(22.dp))
    }

    @Composable
    override fun MiniPreview(dark: Boolean, modifier: Modifier) =
        MiniMock(palette(dark), 8.dp, cards = true, centerHeader = false, topTabs = false, floatingNav = false, pill = false, modifier = modifier)
}
