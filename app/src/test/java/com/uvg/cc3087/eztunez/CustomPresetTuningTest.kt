package com.uvg.cc3087.eztunez

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class CustomPresetTuningTest {
    @Test fun customLowAndHighOctavesReachAutoAndManualThroughTheRealAnalyzer() {
        val preset = TuningPreset(-1, "Extended", listOf(31,45,50,55,59,69), category = PresetCategory.CUSTOM)
        for (mode in TuningMode.entries) {
            for (reference in listOf(432, 440)) {
                for (target in listOf(preset.guitarStrings(reference).first(), preset.guitarStrings(reference).last())) {
                    val analyzer = GuitarSignalAnalyzer(DEFAULT_RMS_THRESHOLD, reference, preset)
                    val tracker = TuningTracker(mode, reference, preset)
                    if (mode == TuningMode.MANUAL) tracker.selectString(target.id)
                    repeat(20) { frame ->
                        val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                            val sample = frame * GuitarSignalAnalyzer.HOP_SIZE + index
                            (0.1 * sin(2.0 * PI * target.frequency * sample / GuitarSignalAnalyzer.SAMPLE_RATE)).toFloat()
                        }
                        tracker.update(analyzer.analyze(block), frame * 46L)
                    }
                    assertEquals("$mode A4=$reference ${target.note}", target.id, tracker.state.selectedString.id)
                    assertTrue("$mode A4=$reference ${target.note}", tracker.state.isInTune)
                }
            }
        }
    }

    @Test fun builtInFrequencyWindowsRemainUnchanged() {
        for (preset in BuiltInTuningPresets.all) assertEquals(60f..400f, preset.captureFrequencyRange(432))
    }
}
