package com.uvg.cc3087.eztunez

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.tunerSettingsDataStore by preferencesDataStore(name = "tuner_settings")

interface SettingsRepository {
    val settings: Flow<TunerSettings>
    suspend fun saveSettings(settings: TunerSettings)
    suspend fun setMicSensitivity(value: Float)
    suspend fun setReferencePitch(value: Int)
    suspend fun setShowRealTimeHz(value: Boolean)
    suspend fun setNotationSystem(value: NotationSystem)
    suspend fun setDarkTheme(value: Boolean)
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    constructor(context: Context) : this(context.tunerSettingsDataStore)

    override val settings: Flow<TunerSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::settingsFromPreferences)

    override suspend fun saveSettings(settings: TunerSettings) {
        val normalized = settings.normalized()
        dataStore.edit {
            it[Keys.MIC_SENSITIVITY] = normalized.micSensitivity
            it[Keys.REFERENCE_PITCH] = normalized.referencePitch
            it[Keys.SHOW_REAL_TIME_HZ] = normalized.showRealTimeHz
            it[Keys.NOTATION_SYSTEM] = normalized.notationSystem.name
            it[Keys.DARK_THEME] = normalized.darkTheme
        }
    }

    override suspend fun setMicSensitivity(value: Float) {
        dataStore.edit { it[Keys.MIC_SENSITIVITY] = normalizeSensitivity(value) }
    }

    override suspend fun setReferencePitch(value: Int) {
        dataStore.edit { it[Keys.REFERENCE_PITCH] = normalizeReferencePitch(value) }
    }

    override suspend fun setShowRealTimeHz(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_REAL_TIME_HZ] = value }
    }

    override suspend fun setNotationSystem(value: NotationSystem) {
        dataStore.edit { it[Keys.NOTATION_SYSTEM] = value.name }
    }

    override suspend fun setDarkTheme(value: Boolean) {
        dataStore.edit { it[Keys.DARK_THEME] = value }
    }

    internal object Keys {
        val MIC_SENSITIVITY = floatPreferencesKey("mic_sensitivity")
        val REFERENCE_PITCH = intPreferencesKey("reference_pitch")
        val SHOW_REAL_TIME_HZ = booleanPreferencesKey("show_real_time_hz")
        val NOTATION_SYSTEM = stringPreferencesKey("notation_system")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}

internal fun settingsFromPreferences(preferences: Preferences): TunerSettings {
    val notation = preferences[DataStoreSettingsRepository.Keys.NOTATION_SYSTEM]
        ?.let { stored -> NotationSystem.entries.firstOrNull { it.name == stored } }
        ?: NotationSystem.STANDARD
    return TunerSettings(
        micSensitivity = preferences[DataStoreSettingsRepository.Keys.MIC_SENSITIVITY]
            ?: DEFAULT_MIC_SENSITIVITY,
        referencePitch = preferences[DataStoreSettingsRepository.Keys.REFERENCE_PITCH]
            ?: DEFAULT_REFERENCE_PITCH,
        showRealTimeHz = preferences[DataStoreSettingsRepository.Keys.SHOW_REAL_TIME_HZ] ?: true,
        notationSystem = notation,
        darkTheme = preferences[DataStoreSettingsRepository.Keys.DARK_THEME] ?: true
    ).normalized()
}
