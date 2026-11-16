package com.uvg.cc3087.eztunez

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** Equal-tempered targets independent of an instrument or preset. */
internal object ChromaticScale {
    // Approximately C1–C7, within the useful period range of our 4096-sample window.
    val frequencyRange = 30f..2200f

    fun nearestNote(frequency: Float, referencePitch: Int): GuitarString {
        require(frequency.isFinite() && frequency > 0f)
        val reference = normalizeReferencePitch(referencePitch)
        val midi = (69 + 12 * ln(frequency.toDouble() / reference) / ln(2.0)).roundToInt()
        // The existing target value also represents a chromatic note; its ID is MIDI here.
        return GuitarString(midi, noteNameForMidi(midi),
            (reference * 2.0.pow((midi - 69) / 12.0)).toFloat())
    }
}
