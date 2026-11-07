package com.uvg.cc3087.eztunez

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class FftSignalAnalyzerTest {
    private fun readings(
        frequency: Float,
        reference: Int = 440,
        preset: TuningPreset = BuiltInTuningPresets.standard,
        amplitude: Float = 0.01f,
        harmonic: Boolean = false,
        noise: Float = 0f,
        threshold: Float = DEFAULT_RMS_THRESHOLD
    ): List<DetectedPitch> {
        val analyzer = GuitarSignalAnalyzer(threshold, reference, preset, DetectionAlgorithm.FFT)
        val random = Random(79)
        return (0 until 16).mapNotNull { frame ->
            val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                val sample = frame * GuitarSignalAnalyzer.HOP_SIZE + index
                val phase = 2.0 * PI * frequency * sample / GuitarSignalAnalyzer.SAMPLE_RATE
                val value = amplitude * (if (harmonic) {
                    0.25 * sin(phase) + sin(2 * phase) + 0.4 * sin(3 * phase)
                } else sin(phase)) + noise * (random.nextFloat() * 2 - 1)
                (value * 32768).toInt() / 32768f
            }
            analyzer.analyze(block).takeIf { frame >= 3 }
        }
    }

    @Test fun builtInPresetsAndReferencePitchesStayAccurate() {
        for (preset in BuiltInTuningPresets.all) {
            for (reference in listOf(400, 432, 440, 480)) {
                for (target in preset.guitarStrings(reference)) {
                    val detected = readings(target.frequency, reference, preset)
                    assertTrue("Lost ${preset.id} / $reference / ${target.note}", detected.size >= 10)
                    assertTrue(detected.all { abs(centsFrom(it.frequency, target.frequency)) < 5f })
                }
            }
        }
    }

    @Test fun detunedBassWithHarmonicsAndNoiseDoesNotSnapToAnFftBinOrAnOctave() {
        val frequency = (82.40689 * 2.0.pow(17.0 / 1200)).toFloat()
        val detected = readings(frequency, harmonic = true, noise = 0.0005f)
        assertTrue(detected.size >= 10)
        assertTrue(detected.all { abs(centsFrom(it.frequency, frequency)) < 5f })
    }

    @Test fun customLowAndHighOctavesAreAccepted() {
        val preset = TuningPreset(-1, "Extended", listOf(31, 45, 50, 55, 59, 76), category = PresetCategory.CUSTOM)
        for (reference in listOf(432, 440)) {
            for (target in preset.guitarStrings(reference)) {
                val detected = readings(target.frequency, reference, preset)
                assertTrue("Lost ${target.note}", detected.size >= 10)
                assertTrue(detected.all { abs(centsFrom(it.frequency, target.frequency)) < 5f })
            }
        }
    }

    @Test fun sensitivityStillControlsQuietAudio() {
        assertTrue(readings(110f, amplitude = 0.0002f).isEmpty())
        assertTrue(readings(110f, amplitude = 0.0002f, threshold = sensitivityToRmsThreshold(1f)).size >= 10)
    }

    @Test fun unpitchedNoiseIsRejected() {
        val analyzer = GuitarSignalAnalyzer(algorithm = DetectionAlgorithm.FFT)
        val random = Random(42)
        repeat(30) {
            assertNull(analyzer.analyze(FloatArray(GuitarSignalAnalyzer.HOP_SIZE) {
                (random.nextFloat() * 2 - 1) * 0.03f
            }))
        }
    }
}
