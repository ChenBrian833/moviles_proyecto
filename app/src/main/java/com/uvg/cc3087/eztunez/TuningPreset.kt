package com.uvg.cc3087.eztunez

import kotlin.math.pow
import androidx.annotation.StringRes

data class TuningPreset(
    val id: Int,
    val name: String = "",
    val midiNotes: List<Int>,
    val description: String = "",
    val category: PresetCategory,
    val preferFlats: Boolean = false,
    @StringRes val nameRes: Int? = null,
    @StringRes val descriptionRes: Int? = null
) {
    init {
        require(midiNotes.size == 6 && midiNotes.all { it in 0..127 })
    }

    val tuning: String get() = midiNotes.joinToString(" ") {
        noteNameForMidi(it, preferFlats, includeOctave = false)
    }

    fun guitarStrings(referencePitch: Int): List<GuitarString> {
        val reference = normalizeReferencePitch(referencePitch)
        return midiNotes.mapIndexed { index, midi ->
            GuitarString(
                id = index + 1,
                note = noteNameForMidi(midi, preferFlats),
                frequency = (reference * 2.0.pow((midi - 69) / 12.0)).toFloat()
            )
        }
    }
}

enum class PresetCategory {
    GUITAR,
    CUSTOM
}
