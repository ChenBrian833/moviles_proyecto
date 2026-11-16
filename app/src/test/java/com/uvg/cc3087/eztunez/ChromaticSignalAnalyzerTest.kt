package com.uvg.cc3087.eztunez

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ChromaticSignalAnalyzerTest(private val algorithm: DetectionAlgorithm) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun algorithms() = DetectionAlgorithm.entries.map { arrayOf(it) }
    }

    @Test fun `chromatic capture accepts notes below the guitar range`() {
        assertTracks(32.7032f)
    }

    @Test fun `chromatic capture accepts notes above the guitar range`() {
        for (frequency in listOf(466.1638f, 987.7666f, 2093.0045f)) assertTracks(frequency)
    }

    @Test fun `a harmonic of an open guitar string is retained as the played note`() {
        assertTracks(659.2551f)
    }

    private fun assertTracks(frequency: Float) {
        val analyzer = GuitarSignalAnalyzer(mode = TuningMode.CHROMATIC, algorithm = algorithm)
        val tracker = TuningTracker(TuningMode.CHROMATIC)
        var accepted = 0
        repeat(16) { frame ->
            val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                val sample = frame * GuitarSignalAnalyzer.HOP_SIZE + index
                // Quantize like microphone PCM16 input.
                (0.02 * sin(2 * PI * frequency * sample / GuitarSignalAnalyzer.SAMPLE_RATE) * 32768).toInt() / 32768f
            }
            val pitch = analyzer.analyze(block)
            if (frame >= 3 && pitch != null) {
                accepted++
                assertTrue("$algorithm: $frequency became ${pitch.frequency}",
                    abs(centsFrom(pitch.frequency, frequency)) < 5f)
            }
            tracker.update(pitch, frame * 46L)
        }
        assertTrue("$algorithm lost $frequency", accepted >= 10)
        assertEquals(ChromaticScale.nearestNote(frequency, 440).note, tracker.state.selectedString.note)
        assertTrue("$algorithm cannot tune $frequency", tracker.state.isInTune)
    }
}
