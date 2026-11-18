package com.zentones.app.editor

import com.zentones.app.tones.ToneKind
import kotlin.math.abs

/** Lo que abre el editor: audio decodificado + sugerencias. */
data class EditJob(val pcm: Pcm, val name: String, val kind: ToneKind, val origin: String)

/** Ajustes del recorte. start/end en segundos. */
data class EditParams(
    val start: Double,
    val end: Double,
    val fadeIn: Boolean,
    val fadeOut: Boolean,
    val gainDb: Float,
    val normalize: Boolean,
)

object AudioEdit {
    fun apply(src: Pcm, p: EditParams): ShortArray {
        val sr = src.sampleRate
        val a = (p.start * sr).toInt().coerceIn(0, src.data.size)
        val b = (p.end * sr).toInt().coerceIn(a, src.data.size)
        val n = b - a
        val out = FloatArray(n) { src.data[a + it] / 32768f }
        var gain = Math.pow(10.0, p.gainDb / 20.0).toFloat()
        if (p.normalize) {
            var peak = 1e-6f
            for (v in out) peak = maxOf(peak, abs(v))
            gain *= 0.89f / peak
        }
        val fi = if (p.fadeIn) minOf((0.25 * sr).toInt(), n / 4) else 0
        val fo = if (p.fadeOut) minOf((0.9 * sr).toInt(), n / 3) else (0.01 * sr).toInt().coerceAtMost(n)
        return ShortArray(n) { i ->
            var v = out[i] * gain
            if (i < fi) v *= i.toFloat() / fi
            if (i >= n - fo && fo > 0) v *= (n - i).toFloat() / fo
            (v * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
