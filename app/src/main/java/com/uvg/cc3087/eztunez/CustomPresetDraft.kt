package com.uvg.cc3087.eztunez

import java.util.Locale

data class CustomPresetDraft(
    val id: Int? = null,
    val name: String = "",
    val description: String = "",
    val midiNotes: List<Int> = BuiltInTuningPresets.standard.midiNotes
)

enum class PresetProblem { NAME_REQUIRED, DUPLICATE_NAME, INVALID_NOTES, READ_ONLY, NOT_FOUND, STORAGE }
class PresetException(val problem: PresetProblem) : Exception(problem.name)

internal fun cleanPresetName(name: String): String = name.trim().replace(Regex("\\s+"), " ")
internal fun presetNameKey(name: String): String = cleanPresetName(name).lowercase(Locale.ROOT)

internal fun validateCustomPreset(draft: CustomPresetDraft) {
    if (cleanPresetName(draft.name).isEmpty()) throw PresetException(PresetProblem.NAME_REQUIRED)
    if (draft.midiNotes.size != 6 || draft.midiNotes.any { it !in 0..127 }) {
        throw PresetException(PresetProblem.INVALID_NOTES)
    }
    if (draft.id != null && (draft.id >= 0 || draft.id == Int.MIN_VALUE)) {
        throw PresetException(PresetProblem.READ_ONLY)
    }
}
