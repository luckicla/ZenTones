package com.zentones.app.ui.skins

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
 * Sony Xperia (sobrio): pestañas arriba con subrayado, listas planas a ancho completo con separadores,
 * iconos de línea monocromos, esquinas casi rectas y un acento acero apagado.
 */
object SonySkin : Skin {
    override val id = "sony"
    override val name = "Sony"
    override val tagline = "Sobrio, plano, preciso"
    override val choiceAtEnd = true
    override val sheetAlignment = Alignment.Center

    @Composable
    override fun palette(dark: Boolean): Pal = if (dark) Pal(
        true, Color(0xFF101112), Color(0xFF17181A), Color(0xFF222427), Color(0xFFE8E8E8), Color(0xFF9AA0A6),
        Color(0xFF7FB2CF), Color(0xFF0E1A21), Color(0x267FB2CF), Color(0x1FFFFFFF),
    ) else Pal(
        false, Color(0xFFFAFAFA), Color.White, Color(0xFFF0F0F0), Color(0xFF1B1B1B), Color(0xFF6E6E6E),
        Color(0xFF2D6A8A), Color.White, Color(0x1A2D6A8A), Color(0x1F000000),
    )

    @Composable override fun colorScheme(dark: Boolean): ColorScheme = schemeFrom(palette(dark))
    override fun glass(p: Pal): HazeStyle = glassOf(p, 0.82f, 20.dp)

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
            Box(Modifier.fillMaxSize().hazeSource(haze)) { content(PaddingValues(top = top + 102.dp, bottom = bottom + 12.dp)) }

            Column(Modifier.fillMaxWidth().hazeEffect(haze, style).padding(top = top)) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Zentones", fontSize = 20.sp, color = p.text, letterSpacing = 0.4.sp, modifier = Modifier.weight(1f))
                    Text(title.uppercase(), fontSize = 11.sp, color = p.sub, letterSpacing = 1.4.sp)
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(46.dp)) {
                    val w = maxWidth / tabs.size
                    val x by animateDpAsState(w * selected, label = "ind")
                    Row(Modifier.fillMaxSize()) {
                        tabs.forEachIndexed { i, t ->
                            val c by animateColorAsState(if (i == selected) p.accent else p.sub, label = "tc")
                            Box(
                                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(i) },
                                contentAlignment = Alignment.Center,
                            ) { Text(t.label.uppercase(), fontSize = 12.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp, color = c) }
                        }
                    }
                    Box(Modifier.align(Alignment.BottomStart).offset(x = x).width(w).padding(horizontal = 18.dp).height(2.dp).background(p.accent))
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
            }
            // franja de cristal bajo la barra de navegación del sistema
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottom).hazeEffect(haze, style))
        }
    }

    @Composable
    override fun Header(title: String, subtitle: String?) {
        if (subtitle != null) Text(
            subtitle, fontSize = 13.sp, color = LocalPal.current.sub,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
        ) else Spacer(Modifier.height(8.dp))
    }

    @Composable
    override fun SectionTitle(text: String) {
        Text(
            text.uppercase(), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = LocalPal.current.accent, letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp),
        )
    }

    @Composable
    override fun Item(
        title: String, subtitle: String?, leading: (@Composable () -> Unit)?, trailing: (@Composable () -> Unit)?,
        pos: Pos, onClick: (() -> Unit)?,
    ) {
        val p = LocalPal.current
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) { leading(); Spacer(Modifier.width(18.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = p.sub, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
                }
                if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
            }
            if (pos == Pos.FIRST || pos == Pos.MIDDLE) {
                Box(Modifier.padding(start = if (leading != null) 62.dp else 20.dp).fillMaxWidth().height(0.5.dp).background(p.divider))
            }
        }
    }

    @Composable
    override fun Leading(icon: ImageVector, tint: Color) {
        Icon(icon, null, tint = LocalPal.current.sub, modifier = Modifier.size(24.dp))
    }

    @Composable
    override fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
        val p = LocalPal.current
        val x by animateDpAsState(if (checked) 18.dp else 0.dp, label = "x")
        val track by animateColorAsState(if (checked) p.accent.copy(alpha = .45f) else p.sub.copy(alpha = .35f), label = "t")
        val thumb by animateColorAsState(if (checked) p.accent else if (p.dark) Color(0xFFBDBDBD) else Color(0xFFF5F5F5), label = "th")
        Box(
            Modifier.width(40.dp).height(24.dp).clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.padding(horizontal = 3.dp).fillMaxWidth().height(14.dp).clip(CircleShape).background(track))
            Box(Modifier.offset(x = x).size(22.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(thumb))
        }
    }

    @Composable
    override fun Btn(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val p = LocalPal.current
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier.height(46.dp).clip(shape)
                .then(if (primary) Modifier.background(p.accent) else Modifier.border(1.dp, p.accent, shape))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(text.uppercase(), fontSize = 13.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp, color = if (primary) p.onAccent else p.accent) }
    }

    @Composable
    override fun Choice(selected: Boolean) {
        val p = LocalPal.current
        Box(Modifier.size(20.dp).border(1.5.dp, if (selected) p.accent else p.sub, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(p.accent))
        }
    }

    @Composable
    override fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
        val p = LocalPal.current
        Column(Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text.uppercase(), fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium, color = if (selected) p.accent else p.sub)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(24.dp).height(2.dp).background(if (selected) p.accent else Color.Transparent))
        }
    }

    @Composable
    override fun SheetSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
        val p = LocalPal.current
        val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.82f
        Column(
            Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth().heightIn(max = maxH)
                .shadow(24.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(p.surface)
                .padding(vertical = 20.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = p.text, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) { content() }
        }
    }

    @Composable
    override fun Tile(title: String, value: String, icon: ImageVector, alt: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val p = LocalPal.current
        Row(
            modifier.border(1.dp, p.divider, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(42.dp).background(if (alt) p.sub else p.accent))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title.uppercase(), fontSize = 10.5.sp, letterSpacing = 1.3.sp, color = p.sub)
                Text(value, fontSize = 18.sp, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    @Composable
    override fun Banner(text: String, action: String, onClick: () -> Unit) {
        val p = LocalPal.current
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().border(1.dp, p.accent.copy(alpha = .6f), RoundedCornerShape(4.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, null, tint = p.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, fontSize = 13.5.sp, color = p.text, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(action.uppercase(), fontSize = 12.5.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = p.accent, textAlign = TextAlign.End, modifier = Modifier.clickable(onClick = onClick))
        }
    }

    @Composable
    override fun Field(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector?) {
        val p = LocalPal.current
        Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = p.sub, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)) }
                BasicTextField(
                    value = value, onValueChange = onChange, singleLine = true,
                    textStyle = TextStyle(color = p.text, fontSize = 15.sp), cursorBrush = SolidColor(p.accent), modifier = Modifier.weight(1f),
                    decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = p.sub, fontSize = 15.sp); inner() } },
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(if (value.isEmpty()) p.divider else p.accent))
        }
    }

    @Composable override fun Chevron() = Unit

    @Composable
    override fun MiniPreview(dark: Boolean, modifier: Modifier) =
        MiniMock(palette(dark), 0.dp, cards = false, centerHeader = false, topTabs = true, floatingNav = false, pill = false, modifier = modifier)
}
