package com.uvg.cc3087.eztunez

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Real Room + UI/ViewModels, with a fake microphone and isolated preferences. */
class CustomPresetUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private lateinit var database: PresetDatabase
    private lateinit var presets: TuningPresetsViewModel
    private lateinit var tuner: TunerViewModel
    private lateinit var settings: SettingsViewModel
    private val captures = mutableListOf<TuningPreset>()

    @Before fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.RECORD_AUDIO)
        database = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, PresetDatabase::class.java).build()
        compose.runOnUiThread {
            presets = ViewModelProvider(store, TuningPresetsViewModel.Factory(
                RoomTuningPresetRepository(database, listOf("Standard", "Drop D"))
            ))[TuningPresetsViewModel::class.java]
            tuner = ViewModelProvider(store, TunerViewModel.Factory(
                PitchCaptureFactory { _, _, preset, _, onState ->
                    captures += preset
                    object : PitchCapture {
                        override fun start() { onState(MicrophoneState.LISTENING) }
                        override fun stop() {}
                    }
                }, { 0L }
            ))[TunerViewModel::class.java]
            settings = ViewModelProvider(store, SettingsViewModel.Factory(MemorySettings()))[SettingsViewModel::class.java]
        }
        compose.setContent { EztunezTheme(dynamicColor = false) { App(TunerSettings(), settings, tuner, presets) } }
        compose.waitUntil(5_000) { !presets.uiState.value.isLoading }
    }
    @After fun close() { compose.runOnUiThread { store.clear() }; database.close() }

    @Test fun createSelectInBothModesEditAndConfirmDelete() {
        navigate("Presets")
        compose.onNodeWithContentDescription("Create custom tuning").performClick()
        compose.onNodeWithText("E2").assertIsDisplayed()
        compose.onNodeWithTag("preset-name").performTextInput("Studio")
        compose.onNodeWithTag("preset-description").performTextInput("For practice")
        compose.onAllNodesWithText("Note: E").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("D").performClick()
        compose.onNodeWithTag("preset-save").performScrollTo().performClick()
        waitForPreset("Studio")
        compose.onNodeWithText("Custom").performClick()
        compose.onNodeWithText("Studio — D2 A2 D3 G3 B3 E4").performScrollTo().performClick()
        val id = presets.uiState.value.selectedPresetId
        assertTrue(id < 0)
        navigate("Home")
        compose.onNodeWithText("Manual Tune").performScrollTo().performClick()
        compose.onNodeWithText("Acoustic · Studio").assertIsDisplayed()
        compose.onAllNodesWithText("E4").onLast().performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(6, tuner.uiState.value.tuning.selectedString.id)
            assertEquals(id, captures.last().id)
        }
        navigate("Home")
        compose.onNodeWithText("Auto Tune").performScrollTo().performClick()
        compose.onNodeWithText("Acoustic · Studio").assertIsDisplayed()
        compose.onAllNodesWithText("D2").onLast().performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(id, captures.last().id) }
        navigate("Presets")
        compose.onNodeWithText("Custom").performClick()
        compose.onNodeWithText("Edit").performScrollTo().performClick()
        compose.onNodeWithTag("preset-name").performTextReplacement("Studio edited")
        compose.onNodeWithTag("preset-save").performScrollTo().performClick()
        waitForPreset("Studio edited")
        compose.runOnIdle { assertEquals(id, presets.uiState.value.selectedPresetId) }
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.onNodeWithText("Delete custom preset?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(id, presets.uiState.value.selectedPresetId) }
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.onAllNodesWithText("Delete").onLast().performClick()
        compose.waitUntil(5_000) { presets.uiState.value.deleteTarget == null && presets.uiState.value.presets.isEmpty() }
        compose.runOnIdle { assertEquals(1, presets.uiState.value.selectedPresetId) }
        compose.onNodeWithText("Guitar").performClick()
        compose.onNodeWithText("Edit").assertDoesNotExist()
        compose.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test fun duplicateNameKeepsFormAndCanBeCorrected() {
        navigate("Presets")
        compose.onNodeWithContentDescription("Create custom tuning").performClick()
        compose.onNodeWithTag("preset-name").performTextInput(" standard ")
        compose.onNodeWithTag("preset-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { presets.uiState.value.draftProblem == PresetProblem.DUPLICATE_NAME }
        compose.onNodeWithTag("preset-error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("preset-name").performScrollTo().performTextReplacement("Unique")
        compose.onNodeWithTag("preset-save").performScrollTo().performClick()
        waitForPreset("Unique")
    }

    private fun navigate(label: String) {
        compose.onNode((hasText(label) or hasContentDescription(label)) and hasClickAction()).performClick()
    }
    private fun waitForPreset(name: String) {
        compose.waitUntil(5_000) { presets.uiState.value.draft == null && presets.uiState.value.presets.any { it.name == name } }
    }
    private class MemorySettings : SettingsRepository {
        override val settings = MutableStateFlow(TunerSettings())
        override suspend fun saveSettings(settings: TunerSettings) { this.settings.value = settings }
        override suspend fun setMicSensitivity(value: Float) {}
        override suspend fun setReferencePitch(value: Int) {}
        override suspend fun setShowRealTimeHz(value: Boolean) {}
        override suspend fun setNotationSystem(value: NotationSystem) {}
        override suspend fun setDarkTheme(value: Boolean) {}
    }
}
