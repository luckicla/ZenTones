package com.zentones.app.tones

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class Instr { MARIMBA, KALIMBA, BELL, GLASS, PLUCK, PAD, BEEP }

enum class ToneKind(val label: String) { CALL("Llamada"), MESSAGE("Mensaje") }

/** Una nota: momento (s), nota MIDI, duración (s), instrumento, intensidad */
data class N(val t: Double, val midi: Int, val dur: Double, val ins: Instr, val vel: Double = 0.8)

/** Tono sintetizado en el propio móvil (no ocupa espacio en el APK). */
data class SynthTone(
    val id: String,
    val name: String,
    val kind: ToneKind,
    val desc: String,
    val length: Double,
    val notes: List<N>,
    val reverb: Double = 0.25,
)

/**
 * Sintetizador aditivo pequeño: marimba, kalimba, campana, cristal, cuerda pulsada (Karplus-Strong),
 * pad y pitido. Añade una reverb tipo Schroeder y normaliza.
 */
object ToneSynth {
    const val SR = 44100

    private fun freq(m: Int) = 440.0 * 2.0.pow((m - 69) / 12.0)

    /** (ratio de frecuencia, amplitud, velocidad de caída) */
    private val partials: Map<Instr, List<Triple<Double, Double, Double>>> = mapOf(
        Instr.MARIMBA to listOf(Triple(1.0, 1.0, 7.0), Triple(3.93, 0.32, 22.0), Triple(9.2, 0.07, 45.0)),
        Instr.KALIMBA to listOf(Triple(1.0, 1.0, 4.2), Triple(2.0, 0.08, 7.0), Triple(5.4, 0.18, 20.0)),
        Instr.BELL to listOf(
            Triple(0.5, 0.22, 1.1), Triple(1.0, 1.0, 1.6), Triple(2.0, 0.45, 2.4),
            Triple(2.76, 0.32, 3.2), Triple(5.4, 0.14, 5.5), Triple(8.93, 0.05, 8.0),
        ),
        Instr.GLASS to listOf(Triple(1.0, 1.0, 4.0), Triple(2.32, 0.38, 8.0), Triple(4.25, 0.12, 14.0)),
    )

    private val tails = mapOf(
        Instr.MARIMBA to 1.1, Instr.KALIMBA to 1.6, Instr.BELL to 3.0, Instr.GLASS to 1.4,
        Instr.PLUCK to 1.2, Instr.PAD to 0.4, Instr.BEEP to 0.02,
    )

    fun render(tone: SynthTone): ShortArray {
        val buf = DoubleArray((tone.length * SR).toInt())
        for (n in tone.notes) addNote(buf, n)
        if (tone.reverb > 0) reverb(buf, tone.reverb)
        // Fundido final de 40 ms y normalizado a -1 dBFS
        val fade = (0.04 * SR).toInt()
        for (i in 0 until fade) {
            val idx = buf.size - 1 - i
            if (idx >= 0) buf[idx] *= i.toDouble() / fade
        }
        var peak = 1e-9
        for (v in buf) peak = maxOf(peak, abs(v))
        val g = 0.89 / peak
        return ShortArray(buf.size) { (buf[it] * g * 32767).toInt().coerceIn(-32768, 32767).toShort() }
    }

    private fun addNote(buf: DoubleArray, n: N) {
        val f = freq(n.midi)
        val start = (n.t * SR).toInt()
        if (start >= buf.size) return
        val len = min(((n.dur + (tails[n.ins] ?: 1.0)) * SR).toInt(), buf.size - start)
        when (n.ins) {
            Instr.PLUCK -> pluck(buf, start, len, f, n.vel)
            Instr.PAD -> pad(buf, start, len, f, n.dur, n.vel)
            Instr.BEEP -> beep(buf, start, len, f, n.dur, n.vel)
            else -> {
                val parts = partials.getValue(n.ins)
                for ((ratio, amp, decay) in parts) {
                    val pf = f * ratio
                    if (pf > SR / 2.3) continue
                    val w = 2 * PI * pf / SR
                    val d = exp(-decay / SR)
                    var env = amp * n.vel
                    for (i in 0 until len) {
                        val atk = min(1.0, i / (0.003 * SR))
                        buf[start + i] += sin(w * i) * env * atk
                        env *= d
                    }
                }
            }
        }
    }

    private fun pluck(buf: DoubleArray, start: Int, len: Int, f: Double, vel: Double) {
        val period = (SR / f).toInt().coerceAtLeast(2)
        val rnd = Random(period)
        val line = DoubleArray(period) { rnd.nextDouble(-1.0, 1.0) }
        var idx = 0
        for (i in 0 until len) {
            val next = (idx + 1) % period
            val v = line[idx]
            line[idx] = 0.996 * 0.5 * (line[idx] + line[next])
            buf[start + i] += v * vel * 0.7
            idx = next
        }
    }

    private fun pad(buf: DoubleArray, start: Int, len: Int, f: Double, dur: Double, vel: Double) {
        val a = 0.08 * SR
        val r = 0.4 * SR
        val sus = dur * SR
        for (i in 0 until len) {
            val env = when {
                i < a -> i / a
                i < sus -> 1.0
                else -> maxOf(0.0, 1 - (i - sus) / r)
            }
            val t = i.toDouble() / SR
            val s = sin(2 * PI * f * t) + 0.5 * sin(2 * PI * f * 1.003 * t) + 0.18 * sin(2 * PI * f * 2 * t)
            buf[start + i] += s * env * vel * 0.35
        }
    }

    private fun beep(buf: DoubleArray, start: Int, len: Int, f: Double, dur: Double, vel: Double) {
        val on = (dur * SR).toInt()
        val edge = 0.005 * SR
        for (i in 0 until min(len, on + (0.02 * SR).toInt())) {
            val env = min(1.0, i / edge) * if (i > on) maxOf(0.0, 1 - (i - on) / (0.02 * SR)) else 1.0
            val t = i.toDouble() / SR
            val s = sin(2 * PI * f * t) + 0.2 * sin(2 * PI * 3 * f * t) + 0.08 * sin(2 * PI * 5 * f * t)
            buf[start + i] += s * env * vel * 0.6
        }
    }

    private fun reverb(x: DoubleArray, mix: Double) {
        val combs = intArrayOf(1557, 1617, 1491, 1422, 1277, 1356)
        val wet = DoubleArray(x.size)
        for (d in combs) {
            val line = DoubleArray(d)
            var idx = 0
            var lp = 0.0
            for (i in x.indices) {
                val y = line[idx]
                lp = y * 0.75 + lp * 0.25
                line[idx] = x[i] + lp * 0.8
                wet[i] += y
                idx = (idx + 1) % d
            }
        }
        for (d in intArrayOf(225, 556)) {
            val line = DoubleArray(d)
            var idx = 0
            for (i in wet.indices) {
                val b = line[idx]
                val y = -wet[i] + b
                line[idx] = wet[i] + b * 0.5
                wet[i] = y
                idx = (idx + 1) % d
            }
        }
        for (i in x.indices) x[i] += wet[i] * mix * 0.12
    }
}

object Wav {
    fun write(f: File, pcm: ShortArray, sampleRate: Int) {
        val dataLen = pcm.size * 2
        val bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + dataLen); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(sampleRate); bb.putInt(sampleRate * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(dataLen)
        for (s in pcm) bb.putShort(s)
        f.writeBytes(bb.array())
    }
}
