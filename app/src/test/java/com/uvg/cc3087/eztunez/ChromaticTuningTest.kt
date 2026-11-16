package com.uvg.cc3087.eztunez

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class ChromaticTuningTest {
    @Test fun `every semitone across the supported octaves becomes its own target`() {
        for (midi in 24..96) {
            val tracker = TuningTracker(TuningMode.CHROMATIC)
            val state = acquire(tracker, frequency(midi))
            assertEquals("MIDI $midi", midi, state.selectedString.id)
            assertTrue("MIDI $midi must be in tune", state.isInTune)
        }
    }

    @Test fun `sharp notes report positive cents relative to their nearest semitone`() {
        val state = acquire(TuningTracker(TuningMode.CHROMATIC), frequency(70, cents = 23.0))
        assertEquals("A#4", state.selectedString.note)
        assertEquals(23f, state.cents, 0.01f)
        assertFalse(state.isInTune)
    }

    @Test fun `flat notes report negative cents relative to their nearest semitone`() {
        val state = acquire(TuningTracker(TuningMode.CHROMATIC), frequency(61, cents = -19.0))
        assertEquals("C#4", state.selectedString.note)
        assertEquals(-19f, state.cents, 0.01f)
        assertFalse(state.isInTune)
    }

    @Test fun `calibration moves the chromatic target frequency`() {
        val state = acquire(TuningTracker(TuningMode.CHROMATIC, 432), 432f)
        assertEquals("A4", state.selectedString.note)
        assertEquals(432f, state.selectedString.frequency, 0.001f)
        assertTrue(state.isInTune)
    }

    @Test fun `changing guitar presets cannot change the chromatic result`() {
        val frequency = frequency(73, cents = 12.0)
        val standard = acquire(TuningTracker(TuningMode.CHROMATIC), frequency)
        val flatPreset = acquire(TuningTracker(TuningMode.CHROMATIC,
            preset = BuiltInTuningPresets.all.last()), frequency)
        assertEquals(standard, flatPreset)
    }

    @Test fun `crossing a semitone midpoint changes target and reverses cents direction`() {
        val tracker = TuningTracker(TuningMode.CHROMATIC)
        assertEquals(69, acquire(tracker, frequency(69, cents = 45.0)).selectedString.id)
        var state = tracker.state
        repeat(24) { state = tracker.update(convertFrequency(frequency(69, cents = 55.0), 0.99f), 500L + it * 46) }
        assertEquals("A#4", state.selectedString.note)
        assertEquals(-45f, state.cents, 0.1f)
    }

    @Test fun `a single octave outlier does not replace an established note`() {
        val tracker = TuningTracker(TuningMode.CHROMATIC)
        val established = acquire(tracker, 440f)
        assertEquals(established, tracker.update(convertFrequency(880f, 0.99f), 300))
    }

    @Test fun `silence expires the previous in tune measurement`() {
        val tracker = TuningTracker(TuningMode.CHROMATIC)
        assertTrue(acquire(tracker, 440f).isInTune)
        val state = tracker.update(null, 1000)
        assertNull(state.pitch)
        assertFalse(state.isInTune)
    }

    @Test fun `unreliable frequencies cannot acquire a chromatic note`() {
        for (f in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -440f, 20f, 3000f)) {
            assertNull(acquire(TuningTracker(TuningMode.CHROMATIC), f).pitch)
        }
    }

    @Test fun `low confidence readings cannot acquire a chromatic note`() {
        val tracker = TuningTracker(TuningMode.CHROMATIC)
        repeat(6) { tracker.update(DetectedPitch(440f, "A4", 0f, 0.2f), it * 46L) }
        assertNull(tracker.state.pitch)
    }

    private fun acquire(tracker: TuningTracker, frequency: Float): TuningState {
        repeat(6) { tracker.update(DetectedPitch(frequency, "", 0f, 0.99f), it * 46L) }
        return tracker.state
    }

    private fun frequency(midi: Int, cents: Double = 0.0) =
        (440 * 2.0.pow((midi - 69 + cents / 100) / 12)).toFloat()
}
