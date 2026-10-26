package com.uvg.cc3087.eztunez

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class TuningPresetTest {
    @Test fun catalogTargetsIncludeTheCorrectOctavesAndReferenceScaling() {
        val expected = listOf(
            "E2 A2 D3 G3 B3 E4",
            "D2 A2 D3 G3 B3 E4",
            "D2 G2 D3 G3 B3 D4",
            "D2 A2 D3 G3 A3 D4",
            "Eb2 Ab2 Db3 Gb3 Bb3 Eb4"
        )
        BuiltInTuningPresets.all.zip(expected).forEach { (preset, notes) ->
            val concert = preset.guitarStrings(440)
            assertEquals(notes, concert.joinToString(" ") { it.note })
            concert.zip(preset.guitarStrings(432)).forEach { (normal, calibrated) ->
                assertEquals(normal.frequency * 432f / 440f, calibrated.frequency, 0.0001f)
            }
        }
        val dropD = BuiltInTuningPresets.all.single { it.id == 2 }
        assertEquals(73.41619f, dropD.guitarStrings(440).first().frequency, 0.0001f)
    }

    @Test fun quietPcmNotesInEveryPresetReachBothManualAndAutomaticTuning() {
        for (preset in BuiltInTuningPresets.all) {
            for (target in preset.guitarStrings(440)) {
                val analyzer = GuitarSignalAnalyzer(preset = preset)
                val auto = TuningTracker(TuningMode.AUTO, preset = preset)
                val manual = TuningTracker(TuningMode.MANUAL, preset = preset)
                manual.selectString(target.id)
                repeat(16) { frame ->
                    val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                        val sample = frame * GuitarSignalAnalyzer.HOP_SIZE + index
                        val phase = 2 * PI * target.frequency * sample / GuitarSignalAnalyzer.SAMPLE_RATE
                        (0.001 * sin(phase) * 32768).toInt() / 32768f
                    }
                    val pitch = analyzer.analyze(block)
                    val time = frame * GuitarSignalAnalyzer.HOP_SIZE * 1000L / GuitarSignalAnalyzer.SAMPLE_RATE
                    auto.update(pitch, time)
                    manual.update(pitch, time)
                }
                for (tracker in listOf(auto, manual)) {
                    val label = "${preset.name}: ${target.note}"
                    assertEquals(label, target.id, tracker.state.selectedString.id)
                    assertEquals(label, target.note, tracker.state.pitch?.note)
                    assertTrue(label, tracker.state.isInTune)
                }
            }
        }
    }
}
