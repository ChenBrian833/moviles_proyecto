package com.uvg.cc3087.eztunez

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

class GuitarSignalAnalyzerTest {
    private fun tones(
        frequency: Float,
        amplitude: Float,
        fundamental: Double = 1.0,
        secondHarmonic: Double = 0.0
    ): List<DetectedPitch> {
        val analyzer = GuitarSignalAnalyzer()
        val pitches = mutableListOf<DetectedPitch>()
        repeat(16) { frame ->
            val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                val t = (frame * GuitarSignalAnalyzer.HOP_SIZE + index).toDouble() /
                    GuitarSignalAnalyzer.SAMPLE_RATE
                val phase = 2.0 * PI * frequency * t
                val signal = amplitude * (fundamental * sin(phase) + secondHarmonic * sin(2.0 * phase))
                // Quantize as PCM16, as on the actual microphone path.
                (signal * 32768).toInt() / 32768f
            }
            val pitch = analyzer.analyze(block)
            if (frame >= 3 && pitch != null) pitches.add(pitch)
        }
        return pitches
    }

    @Test fun quietNotesOnAllSixStringsSurviveTheVolumeGate() {
        standardGuitarStrings.forEach { string ->
            // RMS is below 0.001: these notes were rejected by the old 0.002 gate.
            val readings = tones(string.frequency, 0.001f)
            assertTrue("Missing quiet ${string.note}", readings.size >= 10)
            assertTrue(readings.all { abs(centsFrom(it.frequency, string.frequency)) < 5f })
        }
    }

    @Test fun g3WithADominantSecondHarmonicKeepsItsFundamental() {
        val readings = tones(195.99772f, 0.08f, fundamental = 0.25, secondHarmonic = 1.0)
        assertTrue(readings.size >= 10)
        assertTrue(readings.all { abs(centsFrom(it.frequency, 195.99772f)) < 5f })
    }

    @Test fun quietG3ReachesTheAutomaticStringSelector() {
        val tracker = TuningTracker(TuningMode.AUTO)
        val readings = tones(195.99772f, 0.001f)
        assertTrue(readings.size >= 10)
        readings.forEachIndexed { index, pitch -> tracker.update(pitch, index * 46L) }
        assertEquals("G3", tracker.state.selectedString.note)
        assertTrue(tracker.state.isInTune)
    }

    @Test fun silenceAndConstantMicrophoneOffsetAreNotNotes() {
        val analyzer = GuitarSignalAnalyzer()
        repeat(16) {
            assertNull(analyzer.analyze(FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { 0.004f }))
        }
    }

    @Test fun highEIsNotFoldedIntoLowE() {
        val readings = tones(329.62756f, 0.003f)
        assertTrue(readings.size >= 10)
        assertTrue(readings.all { it.note == "E4" })
    }
    @Test fun bassStringsRemainDetectableAsTheSoundDecays() {
        for (frequency in listOf(82.40689f, 110f, 146.83238f)) {
            val analyzer = GuitarSignalAnalyzer()
            val tracker = TuningTracker(TuningMode.AUTO)
            var accepted = 0
            repeat(26) { frame ->
                val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                    val t = (frame * GuitarSignalAnalyzer.HOP_SIZE + index).toDouble() /
                        GuitarSignalAnalyzer.SAMPLE_RATE
                    val phase = 2.0 * PI * frequency * t + 0.73
                    val envelope = 0.006 * exp(-t / 0.65)
                    val signal = 0.003 + envelope * (sin(phase) + 0.45 * sin(2 * phase))
                    (signal * 32768).toInt() / 32768f
                }
                val pitch = analyzer.analyze(block)
                tracker.update(pitch, frame * 46L)
                if (frame >= 3 && pitch != null && abs(centsFrom(pitch.frequency, frequency)) < 5f) {
                    accepted++
                }
            }
            assertTrue("Bass note lost during decay: $frequency Hz", accepted >= 20)
            assertTrue(tracker.state.isInTune)
            assertEquals(convertFrequency(frequency, 1f).note, tracker.state.selectedString.note)
        }
    }

    @Test fun analysisDoesNotModifyMicrophoneSamples() {
        val analyzer = GuitarSignalAnalyzer()
        val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
            (0.002 + 0.004 * sin(2 * PI * 82.40689 * index / GuitarSignalAnalyzer.SAMPLE_RATE)).toFloat()
        }
        val original = block.copyOf()
        repeat(4) {
            analyzer.analyze(block)
            assertArrayEquals(original, block, 0f)
        }
    }

    @Test fun lowEAndAWorkBelowThePreviousVolumeCutoff() {
        for (frequency in listOf(82.40689f, 110f)) {
            val readings = tones(frequency, 0.0006f)
            assertTrue("Missing soft bass: $frequency Hz", readings.size >= 10)
            assertTrue(readings.all { abs(centsFrom(it.frequency, frequency)) < 5f })
        }
    }
}
