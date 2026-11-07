package com.uvg.cc3087.eztunez

import org.junit.Assert.*
import org.junit.Test

class CustomPresetValidationTest {
    @Test fun draftStartsWithStandardInLowToHighOrder() {
        assertEquals(listOf(40, 45, 50, 55, 59, 64), CustomPresetDraft().midiNotes)
    }
    @Test fun namesAreTrimmedAndComparedWithoutCaseOrExtraSpaces() {
        assertEquals("My tuning", cleanPresetName("  My  tuning  "))
        assertEquals(presetNameKey("MY Tuning"), presetNameKey(" my  tuning "))
    }
    @Test fun rejectsEmptyNameInvalidNotesAndBuiltInEdits() {
        for ((draft, expected) in listOf(
            CustomPresetDraft(name = "  ") to PresetProblem.NAME_REQUIRED,
            CustomPresetDraft(name = "Test", midiNotes = listOf(40)) to PresetProblem.INVALID_NOTES,
            CustomPresetDraft(name = "Test", midiNotes = List(6) { 128 }) to PresetProblem.INVALID_NOTES,
            CustomPresetDraft(id = 1, name = "Test") to PresetProblem.READ_ONLY
        )) {
            val error = assertThrows(PresetException::class.java) { validateCustomPreset(draft) }
            assertEquals(expected, error.problem)
        }
    }
    @Test fun customMidiNotesUseTheConfiguredReference() {
        val preset = TuningPreset(-1, "Test", listOf(40, 45, 50, 55, 59, 69), category = PresetCategory.CUSTOM)
        assertEquals(432f, preset.guitarStrings(432).last().frequency, 0.001f)
    }
}
