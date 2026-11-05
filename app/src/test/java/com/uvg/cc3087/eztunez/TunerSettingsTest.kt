package com.uvg.cc3087.eztunez

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerSettingsTest {
    @Test fun sensitivityLimitsAndDefaultMatchTheEstablishedGate() {
        assertEquals(sensitivityToRmsThreshold(0f), sensitivityToRmsThreshold(-4f), 0f)
        assertEquals(sensitivityToRmsThreshold(1f), sensitivityToRmsThreshold(4f), 0f)
        assertEquals(
            DEFAULT_RMS_THRESHOLD,
            sensitivityToRmsThreshold(DEFAULT_MIC_SENSITIVITY),
            0.0000001f
        )
        assertEquals(
            DEFAULT_RMS_THRESHOLD,
            sensitivityToRmsThreshold(Float.NaN),
            0.0000001f
        )
    }

    @Test fun higherSensitivityAlwaysLowersTheRmsThreshold() {
        val thresholds = (0..100).map { sensitivityToRmsThreshold(it / 100f) }
        thresholds.zipWithNext().forEach { (lower, higher) ->
            assertTrue(higher <= lower)
        }
    }

    @Test fun defaultsPreserveCurrentBehavior() {
        assertEquals(0.78f, TunerSettings().micSensitivity, 0f)
        assertEquals(440, TunerSettings().referencePitch)
        assertTrue(TunerSettings().showRealTimeHz)
        assertEquals(NotationSystem.STANDARD, TunerSettings().notationSystem)
        assertTrue(TunerSettings().darkTheme)
    }

    @Test fun invalidValuesAreNormalized() {
        val low = TunerSettings(Float.NEGATIVE_INFINITY, 100).normalized()
        val high = TunerSettings(2f, 900).normalized()
        assertEquals(DEFAULT_MIC_SENSITIVITY, low.micSensitivity, 0f)
        assertEquals(MIN_REFERENCE_PITCH, low.referencePitch)
        assertEquals(1f, high.micSensitivity, 0f)
        assertEquals(MAX_REFERENCE_PITCH, high.referencePitch)
    }

    @Test fun notationChangesOnlyPresentationAndKeepsAccidentalsAndOctaves() {
        assertEquals("C#4", formatNote("C#4", NotationSystem.STANDARD))
        assertEquals("Do#4", formatNote("C#4", NotationSystem.SOLFEGE))
        assertEquals("Mi2", formatNote("E2", NotationSystem.SOLFEGE))
        assertEquals("Si3", formatNote("B3", NotationSystem.SOLFEGE))
    }

    @Test fun showHzChangesOnlyTheFormattedPresentation() {
        val pitch = DetectedPitch(110f, "A2", 0f, 0.9f)
        assertEquals(
            "A2 · 110.0 Hz",
            formatPitchDisplay(pitch, true, NotationSystem.STANDARD)
        )
        assertEquals("A2", formatPitchDisplay(pitch, false, NotationSystem.STANDARD))
        assertEquals(110f, pitch.frequency, 0f)
    }
}
