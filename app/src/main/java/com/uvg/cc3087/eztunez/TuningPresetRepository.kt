package com.uvg.cc3087.eztunez

/** Local catalog shared by the preset picker and the tuning engine. */
interface TuningPresetRepository {
    fun getPresets(): List<TuningPreset>
}

class BuiltInTuningPresetRepository : TuningPresetRepository {
    override fun getPresets(): List<TuningPreset> = BuiltInTuningPresets.all
}

object BuiltInTuningPresets {
    val standard = TuningPreset(
        id = 1,
        name = "Standard",
        midiNotes = listOf(40, 45, 50, 55, 59, 64), // E2 A2 D3 G3 B3 E4
        description = "Common layout suitable for most acoustic & electric guitars",
        category = PresetCategory.GUITAR
    )

    val all = listOf(
        standard,
        TuningPreset(
            id = 2,
            name = "Drop D",
            midiNotes = listOf(38, 45, 50, 55, 59, 64), // D2 A2 D3 G3 B3 E4
            description = "Heavy low response, lower the 6th string a whole step",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 3,
            name = "Open G",
            midiNotes = listOf(38, 43, 50, 55, 59, 62), // D2 G2 D3 G3 B3 D4
            description = "Popular for slide blues playing & folk compositions",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 4,
            name = "DADGAD Tuning",
            midiNotes = listOf(38, 45, 50, 55, 57, 62), // D2 A2 D3 G3 A3 D4
            description = "Celtic folk tuning with rich natural resonance",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 5,
            name = "Half Step Down",
            midiNotes = listOf(39, 44, 49, 54, 58, 63), // Eb2 Ab2 Db3 Gb3 Bb3 Eb4
            description = "Standard relative pitches, lowered by one semitone",
            category = PresetCategory.GUITAR,
            preferFlats = true
        )
    )
}
