package com.zentones.app.tones

import com.zentones.app.tones.Instr.BEEP
import com.zentones.app.tones.Instr.BELL
import com.zentones.app.tones.Instr.GLASS
import com.zentones.app.tones.Instr.KALIMBA
import com.zentones.app.tones.Instr.MARIMBA
import com.zentones.app.tones.Instr.PAD
import com.zentones.app.tones.Instr.PLUCK

/** Secuencia a paso fijo. 0 = silencio. */
private fun seq(t0: Double, step: Double, ins: Instr, vel: Double, vararg m: Int): List<N> =
    m.withIndex().filter { it.value > 0 }.map { N(t0 + it.index * step, it.value, step, ins, vel) }

private fun chord(t: Double, dur: Double, ins: Instr, vel: Double, vararg m: Int, strum: Double = 0.0): List<N> =
    m.mapIndexed { i, x -> N(t + i * strum, x, dur, ins, vel) }

/**
 * Tonos de serie de Zentones. Composiciones originales con el carácter de los tonos clásicos
 * de iPhone (marimba, campanas, cristal), sintetizadas al vuelo.
 */
object BuiltinTones {
    val calls: List<SynthTone> = listOf(
        SynthTone(
            "syn:bambu", "Bambú", ToneKind.CALL, "Marimba de madera, alegre y saltarina", 6.4,
            seq(0.0, 0.16, MARIMBA, 0.8, 75, 0, 82, 79, 0, 84, 82, 0, 79, 0, 77, 79, 0, 0, 0, 0) +
                seq(2.56, 0.16, MARIMBA, 0.8, 75, 0, 82, 79, 0, 87, 84, 0, 82, 0, 79, 82, 0, 0, 0, 0) +
                listOf(N(0.0, 63, 0.2, MARIMBA, 0.55), N(1.28, 58, 0.2, MARIMBA, 0.5), N(2.56, 63, 0.2, MARIMBA, 0.55), N(3.84, 60, 0.2, MARIMBA, 0.5)),
            0.2,
        ),
        SynthTone(
            "syn:reflejo", "Reflejo", ToneKind.CALL, "Arpegio de kalimba sobre acordes suaves", 6.4,
            seq(0.0, 0.2, KALIMBA, 0.75, 69, 73, 76, 81, 76, 73, 69, 73) +
                seq(1.6, 0.2, KALIMBA, 0.75, 66, 69, 73, 78, 73, 69, 66, 69) +
                seq(3.2, 0.2, KALIMBA, 0.75, 62, 66, 69, 74, 69, 66, 62, 66) +
                seq(4.8, 0.2, KALIMBA, 0.75, 64, 68, 71, 76, 71, 68, 64, 0) +
                chord(0.0, 1.5, PAD, 0.22, 57, 61, 64) + chord(1.6, 1.5, PAD, 0.22, 54, 57, 61) +
                chord(3.2, 1.5, PAD, 0.22, 50, 54, 57) + chord(4.8, 1.4, PAD, 0.22, 52, 56, 59),
            0.35,
        ),
        SynthTone(
            "syn:apertura", "Apertura", ToneKind.CALL, "Escalas de marimba que se abren hacia arriba", 6.0,
            seq(0.0, 0.12, MARIMBA, 0.8, 72, 76, 79, 84) + seq(0.72, 0.12, MARIMBA, 0.8, 74, 77, 81, 86) +
                seq(1.44, 0.12, MARIMBA, 0.85, 76, 79, 83, 88) + listOf(N(1.98, 91, 0.6, BELL, 0.35)) +
                seq(3.0, 0.12, MARIMBA, 0.8, 72, 76, 79, 84) + seq(3.72, 0.12, MARIMBA, 0.8, 71, 74, 79, 83) +
                seq(4.44, 0.12, MARIMBA, 0.85, 72, 76, 79, 84) + listOf(N(4.98, 88, 0.6, BELL, 0.35)),
            0.25,
        ),
        SynthTone(
            "syn:faro", "Faro", ToneKind.CALL, "Pulsos de radar, imposible no oírlo", 2.4,
            (seq(0.0, 0.13, BEEP, 0.7, 88, 88, 88, 88) + seq(0.8, 0.13, BEEP, 0.7, 88, 88, 88, 88))
                .map { it.copy(dur = 0.07) } +
                listOf(N(0.0, 100, 0.05, GLASS, 0.15), N(0.8, 100, 0.05, GLASS, 0.15)),
            0.15,
        ),
        SynthTone(
            "syn:brisa", "Brisa", ToneKind.CALL, "Pad cálido con melodía de kalimba", 8.0,
            chord(0.0, 1.9, PAD, 0.25, 60, 64, 67) + chord(2.0, 1.9, PAD, 0.25, 57, 60, 64) +
                chord(4.0, 1.9, PAD, 0.25, 53, 57, 60) + chord(6.0, 1.6, PAD, 0.25, 55, 59, 62) +
                seq(0.0, 0.25, KALIMBA, 0.7, 79, 0, 76, 0, 72, 74, 76, 0, 76, 0, 72, 0, 69, 71, 72, 0) +
                seq(4.0, 0.25, KALIMBA, 0.7, 72, 0, 69, 0, 65, 67, 69, 0, 71, 0, 74, 0, 79, 0, 0, 0),
            0.4,
        ),
        SynthTone(
            "syn:clasico", "Clásico", ToneKind.CALL, "Campanilla de teléfono de toda la vida", 4.0,
            (0 until 22).map { N(it * 0.04, if (it % 2 == 0) 84 else 88, 0.04, GLASS, 0.5) } +
                (0 until 22).map { N(1.3 + it * 0.04, if (it % 2 == 0) 84 else 88, 0.04, GLASS, 0.5) },
            0.1,
        ),
        SynthTone(
            "syn:cumbre", "Cumbre", ToneKind.CALL, "Melodía brillante de kalimba en Re", 6.4,
            seq(0.0, 0.2, KALIMBA, 0.75, 74, 0, 78, 81, 83, 0, 81, 78, 76, 0, 78, 74, 0, 0, 71, 0) +
                seq(3.2, 0.2, KALIMBA, 0.75, 74, 0, 78, 81, 86, 0, 85, 83, 81, 0, 78, 76, 74, 0, 0, 0) +
                chord(0.0, 3.0, PAD, 0.18, 50, 57, 62) + chord(3.2, 3.0, PAD, 0.18, 55, 59, 62),
            0.3,
        ),
        SynthTone(
            "syn:pulso", "Pulso", ToneKind.CALL, "Bajo pulsado con marimba a contratiempo", 6.4,
            seq(0.0, 0.2, PLUCK, 0.9, 45, 0, 45, 0, 48, 0, 50, 0, 45, 0, 45, 0, 43, 0, 43, 0) +
                seq(3.2, 0.2, PLUCK, 0.9, 41, 0, 41, 0, 43, 0, 45, 0, 40, 0, 40, 0, 43, 0, 44, 0) +
                seq(0.1, 0.4, MARIMBA, 0.55, 76, 79, 81, 79, 76, 74, 72, 74, 77, 76, 72, 69, 71, 72, 74, 76),
            0.2,
        ),
    )

    val messages: List<SynthTone> = listOf(
        SynthTone("syn:ding", "Ding", ToneKind.MESSAGE, "El ding de siempre", 2.6,
            listOf(N(0.0, 88, 1.0, BELL, 0.9), N(0.0, 95, 0.5, GLASS, 0.15)), 0.3),
        SynthTone("syn:tritono", "Tri-tono", ToneKind.MESSAGE, "Tres notas de cristal hacia arriba", 1.6,
            seq(0.0, 0.13, GLASS, 0.8, 79, 84, 91), 0.25),
        SynthTone("syn:nota", "Nota", ToneKind.MESSAGE, "Golpe corto de marimba", 1.2,
            listOf(N(0.0, 84, 0.2, MARIMBA, 0.85), N(0.09, 91, 0.2, MARIMBA, 0.55)), 0.2),
        SynthTone("syn:cristal", "Cristal", ToneKind.MESSAGE, "Copa de cristal, finito", 1.5,
            listOf(N(0.0, 96, 0.2, GLASS, 0.8), N(0.07, 103, 0.2, GLASS, 0.45)), 0.3),
        SynthTone("syn:burbuja", "Burbuja", ToneKind.MESSAGE, "Pop rápido y redondo", 1.0,
            seq(0.0, 0.07, PLUCK, 0.9, 72, 79, 84), 0.15),
        SynthTone("syn:acorde", "Acorde", ToneKind.MESSAGE, "Campanas en acorde mayor", 3.0,
            chord(0.0, 1.0, BELL, 0.55, 72, 76, 79, 84, strum = 0.03), 0.3),
        SynthTone("syn:campanilla", "Campanilla", ToneKind.MESSAGE, "Tres campanitas que bajan", 2.5,
            seq(0.0, 0.1, BELL, 0.5, 96, 91, 88), 0.3),
        SynthTone("syn:aviso", "Aviso", ToneKind.MESSAGE, "Dos pitidos limpios", 1.0,
            listOf(N(0.0, 81, 0.09, BEEP, 0.7), N(0.15, 88, 0.14, BEEP, 0.7)), 0.1),
        SynthTone("syn:gota", "Gota", ToneKind.MESSAGE, "Kalimba que cae como una gota", 1.4,
            listOf(N(0.0, 86, 0.1, KALIMBA, 0.85), N(0.1, 81, 0.1, KALIMBA, 0.6)), 0.25),
        SynthTone("syn:bambu-corto", "Bambú corto", ToneKind.MESSAGE, "Dos golpes de marimba", 1.3,
            listOf(N(0.0, 82, 0.1, MARIMBA, 0.8), N(0.12, 87, 0.1, MARIMBA, 0.8)), 0.2),
    )

    val all = calls + messages
    fun byId(id: String) = all.find { it.id == id }
}
