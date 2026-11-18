package com.zentones.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Pos { SINGLE, FIRST, MIDDLE, LAST }

fun posOf(i: Int, n: Int) = when {
    n == 1 -> Pos.SINGLE
    i == 0 -> Pos.FIRST
    i == n - 1 -> Pos.LAST
    else -> Pos.MIDDLE
}

data class NavTab(val label: String, val icon: ImageVector, val iconSel: ImageVector)

@Immutable
data class Pal(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHi: Color,
    val text: Color,
    val sub: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val divider: Color,
)

val LocalPal = staticCompositionLocalOf<Pal> { error("Sin paleta") }
val LocalSkin = staticCompositionLocalOf<Skin> { error("Sin skin") }

/** Colores de iconos tipo "ajustes" */
object Tints {
    val Blue = Color(0xFF3D7BFF)
    val Green = Color(0xFF2FB863)
    val Orange = Color(0xFFFF8A2A)
    val Purple = Color(0xFF8E5CFF)
    val Pink = Color(0xFFFF4F8B)
    val Teal = Color(0xFF14B5B0)
    val Red = Color(0xFFF0453A)
    val Gray = Color(0xFF8A8F98)
}

/**
 * Un "skin" no es solo una paleta: cada uno dibuja sus propias barras, listas, interruptores,
 * botones, hojas, chips y buscador.
 */
interface Skin {
    val id: String
    val name: String
    val tagline: String
    val choiceAtEnd: Boolean
    val sheetAlignment: Alignment

    @Composable fun palette(dark: Boolean): Pal
    @Composable fun colorScheme(dark: Boolean): ColorScheme
    fun glass(p: Pal): HazeStyle

    @Composable fun Frame(
        title: String,
        tabs: List<NavTab>,
        selected: Int,
        onSelect: (Int) -> Unit,
        collapsed: Boolean,
        haze: HazeState,
        content: @Composable (PaddingValues) -> Unit,
    )

    @Composable fun Header(title: String, subtitle: String?)
    @Composable fun SectionTitle(text: String)
    @Composable fun Item(
        title: String,
        subtitle: String?,
        leading: (@Composable () -> Unit)?,
        trailing: (@Composable () -> Unit)?,
        pos: Pos,
        onClick: (() -> Unit)?,
    )
    @Composable fun Leading(icon: ImageVector, tint: Color)
    @Composable fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit)
    @Composable fun Btn(text: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit)
    @Composable fun Choice(selected: Boolean)
    @Composable fun Chip(text: String, selected: Boolean, onClick: () -> Unit)
    @Composable fun SheetSurface(title: String, content: @Composable ColumnScope.() -> Unit)
    @Composable fun Tile(title: String, value: String, icon: ImageVector, alt: Boolean, modifier: Modifier, onClick: () -> Unit)
    @Composable fun Banner(text: String, action: String, onClick: () -> Unit)
    @Composable fun Field(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector?)
    @Composable fun Chevron()
    @Composable fun MiniPreview(dark: Boolean, modifier: Modifier)
}

object Skins {
    val all: List<Skin> by lazy {
        listOf(
            com.zentones.app.ui.skins.PixelSkin,
            com.zentones.app.ui.skins.HyperSkin,
            com.zentones.app.ui.skins.OneUiSkin,
            com.zentones.app.ui.skins.SonySkin,
        )
    }
    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}

fun schemeFrom(p: Pal): ColorScheme = if (p.dark) darkColorScheme(
    primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.text,
    secondaryContainer = p.accentSoft, onSecondaryContainer = p.text,
    background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
    onSurfaceVariant = p.sub, outlineVariant = p.divider, surfaceContainer = p.bg, surfaceContainerHigh = p.surfaceHi,
) else lightColorScheme(
    primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.text,
    secondaryContainer = p.accentSoft, onSecondaryContainer = p.text,
    background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
    onSurfaceVariant = p.sub, outlineVariant = p.divider, surfaceContainer = p.bg, surfaceContainerHigh = p.surfaceHi,
)

fun glassOf(p: Pal, alpha: Float, blur: Dp): HazeStyle =
    HazeStyle(backgroundColor = p.bg, tints = listOf(HazeTint(p.bg.copy(alpha = alpha))), blurRadius = blur, noiseFactor = 0f)

/** Forma "galleta" ondulada de Material 3 Expressive. */
class CookieShape(private val waves: Int = 9, private val depth: Float = 0.07f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy) / (1f + depth)
        val steps = 180
        for (i in 0..steps) {
            val a = 2 * PI * i / steps
            val rr = r * (1 + depth * cos(waves * a))
            val x = cx + (rr * cos(a)).toFloat()
            val y = cy + (rr * sin(a)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return Outline.Generic(p)
    }
}

/** Miniatura esquemática de una interfaz para el selector de estilos. */
@Composable
fun MiniMock(
    p: Pal,
    radius: Dp,
    cards: Boolean,
    centerHeader: Boolean,
    topTabs: Boolean,
    floatingNav: Boolean,
    pill: Boolean,
    modifier: Modifier,
) {
    Box(modifier.aspectRatio(0.62f).clip(RoundedCornerShape(16.dp)).background(p.bg)) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            when {
                topTabs -> {
                    Box(Modifier.fillMaxWidth(0.45f).height(6.dp).background(p.text.copy(alpha = .75f)))
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { i ->
                            Column {
                                Box(Modifier.width(16.dp).height(3.dp).background(if (i == 0) p.accent else p.sub.copy(alpha = .5f)))
                                Spacer(Modifier.height(2.dp))
                                Box(Modifier.width(16.dp).height(1.5.dp).background(if (i == 0) p.accent else Color.Transparent))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                centerHeader -> Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth(0.42f).height(8.dp).clip(RoundedCornerShape(2.dp)).background(p.text.copy(alpha = .8f)))
                }
                else -> {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(0.55f).height(9.dp).clip(RoundedCornerShape(2.dp)).background(p.text.copy(alpha = .85f)))
                    Spacer(Modifier.height(10.dp))
                }
            }
            repeat(2) { g ->
                Column(
                    Modifier.fillMaxWidth()
                        .then(if (cards) Modifier.clip(RoundedCornerShape(radius)).background(p.surface) else Modifier),
                ) {
                    repeat(2) { r ->
                        Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).clip(if (cards) CircleShape else RoundedCornerShape(1.dp)).background(if (cards) p.accent.copy(alpha = .85f) else p.sub.copy(alpha = .6f)))
                            Spacer(Modifier.width(5.dp))
                            Column {
                                Box(Modifier.width(34.dp).height(3.5.dp).background(p.text.copy(alpha = .6f)))
                                Spacer(Modifier.height(2.dp))
                                Box(Modifier.width(20.dp).height(2.5.dp).background(p.sub.copy(alpha = .5f)))
                            }
                        }
                        if (!cards && r == 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
                    }
                }
                if (g == 0) Spacer(Modifier.height(6.dp))
            }
        }
        if (!topTabs) {
            Row(
                Modifier.align(Alignment.BottomCenter)
                    .then(if (floatingNav) Modifier.padding(6.dp).clip(RoundedCornerShape(12.dp)) else Modifier)
                    .fillMaxWidth()
                    .background(p.surface.copy(alpha = .92f))
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier.width(if (pill && i == 0) 18.dp else 8.dp).height(8.dp).clip(CircleShape)
                            .background(if (i == 0) (if (pill) p.accentSoft else p.accent) else p.sub.copy(alpha = .4f)),
                        contentAlignment = Alignment.Center,
                    ) { if (pill && i == 0) Box(Modifier.size(4.dp).clip(CircleShape).background(p.accent)) }
                }
            }
        }
    }
}
