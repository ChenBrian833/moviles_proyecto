package com.uvg.cc3087.eztunez

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PresetSelectionStorageTest {
    @Test fun selectionSurvivesReopeningDataStoreAndDoesNotOverwriteSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = context.preferencesDataStoreFile("selection-test-${UUID.randomUUID()}")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            var dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            val settings = DataStoreSettingsRepository(dataStore)
            settings.saveSettings(TunerSettings(referencePitch = 432))
            DataStorePresetSelectionRepository(dataStore).select(-12)
            settings.setDarkTheme(false)
            assertEquals(-12, DataStorePresetSelectionRepository(dataStore).selectedPresetId.first())
            scope.cancel(); scope.coroutineContext[Job]!!.join()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            assertEquals(-12, DataStorePresetSelectionRepository(dataStore).selectedPresetId.first())
            val recovered = DataStoreSettingsRepository(dataStore).settings.first()
            assertEquals(432, recovered.referencePitch)
            assertFalse(recovered.darkTheme)
        } finally {
            scope.cancel(); scope.coroutineContext[Job]!!.join()
            file.delete()
        }
    }
}
