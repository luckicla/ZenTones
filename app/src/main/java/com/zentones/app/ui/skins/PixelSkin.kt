package com.zentones.app.ui.skins

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zentones.app.ui.CookieShape
import com.zentones.app.ui.LocalPal
import com.zentones.app.ui.MiniMock
import com.zentones.app.ui.NavTab
import com.zentones.app.ui.Pal
import com.zentones.app.ui.Pos
import com.zentones.app.ui.Skin
import com.zentones.app.ui.glassOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Pixel / Material 3 Expressive: Material You, listas segmentadas, píldoras, formas "galleta". */
object PixelSkin : Skin {
    override val id = "pixel"
    override val name = "Pixel"
    override val tagline = "Material You expresivo"
    override val choiceAtEnd = false
    override val sheetAlignment = Alignment.BottomCenter

    @Composable
    override fun colorScheme(dark: Boolean): ColorScheme {
        val ctx = LocalContext.current
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else if (dark) darkColorScheme(primary = Color(0xFFB8C4FF), primaryContainer = Color(0xFF34418F))
        else lightColorScheme(primary = Color(0xFF4A5AC7), primaryContainer = Color(0xFFDDE1FF))
    }

    @Composable
    override fun palette(dark: Boolean): Pal {
        val cs = colorScheme(dark)
        return Pal(
            dark, cs.surfaceContainer, cs.surfaceBright, cs.surfaceContainerHigh, cs.onSurface, cs.onSurfaceVariant,
            cs.primary, cs.onPrimary, cs.primaryContainer, cs.outlineVariant,
        )
    }

    override fun glass(p: Pal): HazeStyle = glassOf(p, 0.72f, 28.dp)

    @Composable
    override fun Frame(
        title: String, tabs: List<NavTab>, selected: Int, onSelect: (Int) -> Unit,
        collapsed: Boolean, haze: HazeState, content: @Composable (PaddingValues) -> Unit,
    ) {
        val cs = MaterialTheme.colorScheme
        val pal = LocalPal.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val style = glass(pal)
        Box(Modifier.fillMaxSize().background(cs.surfaceContainer)) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                content(PaddingValues(top = top, bottom = bottom + 92.dp))
            }
            val barH by animateDpAsState(if (collapsed) top + 64.dp else top, label = "bar")
            val a by animateFloatAsState(if (collapsed) 1f else 0f, label = "barA")
            Box(
                Modifier.fillMaxWidth().height(barH).hazeEffect(haze, style).padding(top = top).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(title, fontSize = 22.sp, color = cs.onSurface, modifier = Modifier.graphicsLayer { alpha = a }) }

            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().hazeEffect(haze, style)
                    .padding(bottom = bottom).height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { i, t ->
                    val sel = i == selected
                    val w by animateDpAsState(if (sel) 64.dp else 32.dp, label = "pill")
                    val bg by animateColorAsState(if (sel) cs.secondaryContainer else Color.Transparent, label = "pillc")
                    Column(
                        Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.width(w).height(32.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                            Icon(if (sel) t.iconSel else t.icon, null, tint = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            t.label, fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            color = if (sel) cs.onSurface else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun Header(title: String, subtitle: String?) {
        val cs = MaterialTheme.colorScheme
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 20.dp)) {
            Text(title, fontSize = 44.sp, lineHeight = 50.sp, color = cs.onSurface)
            if (subtitle != null) Text(subtitle, fontSize = 16.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }

    @Composable
    override fun SectionTitle(text: String) {
        Text(
            text, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 32.dp, top = 22.dp, bottom = 8.dp),
        )
    }

    @Composable
    override fun Item(
        title: String, subtitle: String?, leading: (@Composable () -> Unit)?, trailing: (@Composable () -> Unit)?,
        pos: Pos, onClick: (() -> Unit)?,
    ) {
        val cs = MaterialTheme.colorScheme
        val big = 24.dp
        val small = 4.dp
        val shape = when (pos) {
            Pos.SINGLE -> RoundedCornerShape(big)
            Pos.FIRST -> RoundedCornerShape(big, big, small, small)
            Pos.MIDDLE -> RoundedCornerShape(small)
            Pos.LAST -> RoundedCornerShape(small, small, big, big)
        }
        Row(
            Modifier.padding(horizontal = 16.dp)
                .padding(bottom = if (pos == Pos.FIRST || pos == Pos.MIDDLE) 2.dp else 0.dp)
                .fillMaxWidth().clip(shape).background(cs.surfaceBright)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) { leading(); Spacer(Modifier.width(16.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
        }
    }

    @Composable
    override fun Leading(icon: ImageVector, tint: Color) {
        val dark = LocalPal.current.dark
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = if (dark) .28f else .16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (dark) tint.copy(alpha = .95f) else tint, modifier = Modifier.size(22.dp)) }
    }

    @Composable
    override fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
        Switch(
            checked = checked, onCheckedChange = onChange,
            thumbContent = { Icon(if (checked) Icons.Rounded.Check else Icons.Rounded.Close, null, Modifier.size(SwitchDefaults.IconSize)) },
        )
    }

    @Composable
    override fun Btn(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
        if (primary) Button(onClick = onClick, modifier = modifier.height(56.dp)) { Text(text, fontSize = 16.sp) }
        else FilledTonalButton(onClick = onClick, modifier = modifier.height(56.dp)) { Text(text, fontSize = 16.sp) }
    }

    @Composable
    override fun Choice(selected: Boolean) = RadioButton(selected = selected, onClick = null)

    @Composable
    override fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (selected) cs.secondaryContainer else cs.surfaceBright)
                .clickable(onClick = onClick).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = cs.onSecondaryContainer); Spacer(Modifier.width(6.dp)) }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant)
        }
    }

    @Composable
    override fun SheetSurface(title: String, content: @Composable ColumnScope.() -> Unit) {
        val cs = MaterialTheme.colorScheme
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.88f
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxH)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cs.surfaceContainerLow).padding(bottom = bottom + 12.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(cs.onSurfaceVariant.copy(alpha = .4f)))
            }
            Text(title, fontSize = 24.sp, color = cs.onSurface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 8.dp)) { content() }
        }
    }

    @Composable
    override fun Tile(title: String, value: String, icon: ImageVector, alt: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        val bg = if (alt) cs.tertiaryContainer else cs.primaryContainer
        val fg = if (alt) cs.onTertiaryContainer else cs.onPrimaryContainer
        val badge = if (alt) cs.tertiary else cs.primary
        val onBadge = if (alt) cs.onTertiary else cs.onPrimary
        Column(modifier.clip(RoundedCornerShape(28.dp)).background(bg).clickable(onClick = onClick).padding(20.dp)) {
            Box(Modifier.size(52.dp).clip(CookieShape()).background(badge), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = onBadge)
            }
            Spacer(Modifier.height(18.dp))
            Text(title, fontSize = 14.sp, color = fg.copy(alpha = .8f))
            Text(value, fontSize = 22.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    override fun Banner(text: String, action: String, onClick: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(cs.tertiaryContainer).padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
        ) {
            Text(text, fontSize = 15.sp, color = cs.onTertiaryContainer)
            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) { Text(action, color = cs.tertiary) }
        }
    }

    @Composable
    override fun Field(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector?) {
        val cs = MaterialTheme.colorScheme
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(56.dp).clip(CircleShape)
                .background(cs.surfaceContainerHigh).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { Icon(icon, null, tint = cs.onSurfaceVariant); Spacer(Modifier.width(14.dp)) }
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp), cursorBrush = SolidColor(cs.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box { if (value.isEmpty()) Text(placeholder, color = cs.onSurfaceVariant, fontSize = 16.sp); inner() }
                },
            )
        }
    }

    @Composable override fun Chevron() = Unit

    @Composable
    override fun MiniPreview(dark: Boolean, modifier: Modifier) =
        MiniMock(palette(dark), 10.dp, cards = true, centerHeader = false, topTabs = false, floatingNav = false, pill = true, modifier = modifier)
}
