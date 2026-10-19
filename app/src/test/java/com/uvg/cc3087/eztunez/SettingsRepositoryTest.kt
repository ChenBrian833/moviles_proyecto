package com.uvg.cc3087.eztunez

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {
    @Test fun storedValuesAreRecovered() {
        val preferences = preferencesOf(
            DataStoreSettingsRepository.Keys.MIC_SENSITIVITY to 0.35f,
            DataStoreSettingsRepository.Keys.REFERENCE_PITCH to 432,
            DataStoreSettingsRepository.Keys.SHOW_REAL_TIME_HZ to false,
            DataStoreSettingsRepository.Keys.NOTATION_SYSTEM to NotationSystem.SOLFEGE.name,
            DataStoreSettingsRepository.Keys.DARK_THEME to false
        )

        val settings = settingsFromPreferences(preferences)
        assertEquals(0.35f, settings.micSensitivity, 0f)
        assertEquals(432, settings.referencePitch)
        assertFalse(settings.showRealTimeHz)
        assertEquals(NotationSystem.SOLFEGE, settings.notationSystem)
        assertFalse(settings.darkTheme)
    }

    @Test fun missingAndInvalidStoredValuesUseSafeDefaults() {
        assertEquals(TunerSettings(), settingsFromPreferences(preferencesOf()))
        val invalid = settingsFromPreferences(
            preferencesOf(
                DataStoreSettingsRepository.Keys.MIC_SENSITIVITY to 8f,
                DataStoreSettingsRepository.Keys.REFERENCE_PITCH to 1,
                DataStoreSettingsRepository.Keys.NOTATION_SYSTEM to "UNKNOWN"
            )
        )
        assertEquals(1f, invalid.micSensitivity, 0f)
        assertEquals(MIN_REFERENCE_PITCH, invalid.referencePitch)
        assertEquals(NotationSystem.STANDARD, invalid.notationSystem)
        assertTrue(invalid.showRealTimeHz)
        assertTrue(invalid.darkTheme)
    }
}
