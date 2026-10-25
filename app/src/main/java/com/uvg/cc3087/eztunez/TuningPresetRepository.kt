package com.uvg.cc3087.eztunez

/** The current catalog is local and immutable; selecting it does not configure the tuner yet. */
interface TuningPresetRepository {
    fun getPresets(): List<TuningPreset>
}

class BuiltInTuningPresetRepository : TuningPresetRepository {
    override fun getPresets(): List<TuningPreset> = listOf(
        TuningPreset(
            id = 1,
            name = "Standard",
            tuning = "EADGBE",
            description = "Common layout suitable for most acoustic & electric guitars",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 2,
            name = "Drop D",
            tuning = "DADGBE",
            description = "Heavy low response, lower the 6th string a whole step",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 3,
            name = "Open G",
            tuning = "DGDGBD",
            description = "Popular for slide blues playing & folk compositions",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 4,
            name = "DADGAD Tuning",
            tuning = "",
            description = "Celtic folk tuning with rich natural resonance",
            category = PresetCategory.GUITAR
        ),
        TuningPreset(
            id = 5,
            name = "Half Step Down",
            tuning = "Eb Ab Db Gb Bb Eb",
            description = "Standard relative pitches, lowered by one semitone",
            category = PresetCategory.GUITAR
        )
    )
}
