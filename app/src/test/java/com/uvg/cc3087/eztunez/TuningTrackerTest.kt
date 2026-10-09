package com.uvg.cc3087.eztunez

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class TuningTrackerTest {
    private class Session(mode: TuningMode) {
        val tracker = TuningTracker(mode)
        var time = 0L
        fun play(frequency: Float, frames: Int = 6): TuningState {
            repeat(frames) {
                time += 93L
                tracker.update(convertFrequency(frequency, 0.99f), time)
            }
            return tracker.state
        }
        fun silence(duration: Long): TuningState {
            time += duration
            return tracker.update(null, time)
        }
    }

    private fun shifted(frequency: Float, cents: Float): Float =
        (frequency * 2.0.pow(cents / 1200.0)).toFloat()

    @Test fun standardNotesUseA440AndCorrectOctaves() {
        val midiNotes = listOf(40, 45, 50, 55, 59, 64)
        standardGuitarStrings.zip(midiNotes).forEach { (string, midi) ->
            val expected = 440.0 * 2.0.pow((midi - 69) / 12.0)
            assertEquals(expected, string.frequency.toDouble(), 0.0001)
            assertEquals(string.note, convertFrequency(string.frequency, 1f).note)
        }
    }

    @Test fun autoFollowsEveryStringInBothDirectionsAndDistinguishesBothEs() {
        val session = Session(TuningMode.AUTO)
        (standardGuitarStrings + standardGuitarStrings.reversed()).forEach { string ->
            val state = session.play(string.frequency)
            assertEquals(string.id, state.selectedString.id)
            assertEquals(string.note, state.pitch?.note)
            assertTrue(state.isInTune)
        }
    }

    @Test fun autoSelectsDetunedStringsAndReportsCorrectDirection() {
        standardGuitarStrings.forEach { string ->
            listOf(-40f, 40f).forEach { offset ->
                val session = Session(TuningMode.AUTO)
                val state = session.play(shifted(string.frequency, offset))
                assertEquals(string.id, state.selectedString.id)
                assertEquals(offset, state.cents, 0.02f)
                assertFalse(state.isInTune)
            }
        }
    }

    @Test fun manualNeverChangesTheSelectedString() {
        standardGuitarStrings.forEach { selected ->
            val session = Session(TuningMode.MANUAL)
            session.tracker.selectString(selected.id)
            standardGuitarStrings.forEach { played ->
                val state = session.play(played.frequency)
                assertEquals(selected.id, state.selectedString.id)
                assertEquals(played.id == selected.id, state.isInTune)
            }
        }
    }

    @Test fun chromaticallyCorrectWrongNoteIsNotInTuneWithTarget() {
        val session = Session(TuningMode.MANUAL)
        val state = session.play(87.30706f) // F2, one semitone above target E2.
        assertEquals("F2", state.pitch?.note)
        assertEquals("E2", state.selectedString.note)
        assertEquals(100f, state.cents, 0.02f)
        assertFalse(state.isInTune)
    }

    @Test fun oneHarmonicOrWrongStringReadingCannotMoveNeedleOrTarget() {
        val session = Session(TuningMode.AUTO)
        val before = session.play(shifted(82.40689f, -20f))
        assertEquals(before, session.play(164.81378f, frames = 1))
        assertEquals(before.selectedString, session.play(82.0f).selectedString)
    }

    @Test fun shortDropoutHoldsNeedleAndLongSilenceRemovesInTuneStatus() {
        val session = Session(TuningMode.AUTO)
        val before = session.play(shifted(110f, 20f))
        assertEquals(before, session.silence(200))
        val expired = session.silence(400)
        assertNull(expired.pitch)
        assertEquals(before.cents, expired.cents, 0f)
        assertEquals(before.selectedString, expired.selectedString)
        assertFalse(expired.isInTune)
        assertEquals("E4", session.play(329.62756f).selectedString.note)
    }

    @Test fun jitterIsReducedWithoutHidingARealAdjustment() {
        val session = Session(TuningMode.MANUAL)
        session.tracker.selectString(2)
        session.play(110f)
        repeat(20) { i ->
            val state = session.play(shifted(110f, if (i % 2 == 0) -8f else 8f), 1)
            assertTrue(abs(state.cents) < 4f)
        }
        val adjusted = session.play(shifted(110f, 30f), 12)
        assertEquals(30f, adjusted.cents, 1f)
        assertFalse(adjusted.isInTune)
    }

    @Test fun autoDoesNotAlternateAtStringMidpoint() {
        val session = Session(TuningMode.AUTO)
        session.play(82.40689f)
        // Midpoint between E2 and A2 is 250 cents above E2.
        repeat(16) { i ->
            val state = session.play(shifted(82.40689f, if (i % 2 == 0) 245f else 255f), 1)
            assertEquals("E2", state.selectedString.note)
        }
        assertEquals("A2", session.play(110f).selectedString.note)
    }

    @Test fun manualSelectionClearsThePreviousMeasurement() {
        val session = Session(TuningMode.MANUAL)
        session.play(82.40689f)
        val state = session.tracker.selectString(6)
        assertEquals("E4", state.selectedString.note)
        assertNull(state.pitch)
        assertFalse(state.isInTune)
    }

    @Test fun autoIgnoresManualSelection() {
        val session = Session(TuningMode.AUTO)
        val state = session.play(146.83238f)
        assertEquals(state, session.tracker.selectString(6))
    }

    @Test fun invalidDetectionsExpireInsteadOfLeavingAStaleNote() {
        val session = Session(TuningMode.AUTO)
        session.play(110f)
        val invalid = DetectedPitch(Float.NaN, "", 0f, 1f)
        val state = session.tracker.update(invalid, session.time + 600)
        assertNull(state.pitch)
        assertFalse(state.isInTune)
    }
    @Test fun quietG3AcquiresDespiteBriefGapsInDetection() {
        val tracker = TuningTracker(TuningMode.AUTO)
        val quietG = convertFrequency(195.99772f, 0.78f)
        tracker.update(quietG, 0)
        tracker.update(null, 46)
        tracker.update(quietG, 92)
        tracker.update(null, 138)
        val state = tracker.update(quietG, 184)
        assertEquals("G3", state.selectedString.note)
        assertTrue(state.isInTune)
    }

    @Test fun isolatedReadingsCannotAccumulateAcrossLongGaps() {
        val tracker = TuningTracker(TuningMode.AUTO)
        val quietG = convertFrequency(195.99772f, 0.78f)
        for (time in listOf(0L, 300L, 600L)) {
            assertNull(tracker.update(quietG, time).pitch)
        }
    }
}
