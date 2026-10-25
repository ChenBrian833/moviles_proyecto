package com.uvg.cc3087.eztunez

import android.Manifest
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercises saved Compose state with retained ViewModels and a fake microphone. */
class AppStateRestorationTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val captures = mutableListOf<FakeCapture>()
    private lateinit var settings: SettingsViewModel
    private lateinit var tuner: TunerViewModel
    private lateinit var presets: TuningPresetsViewModel

    @Before fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName, Manifest.permission.RECORD_AUDIO
        )
        compose.runOnUiThread {
            settings = ViewModelProvider(
                store, SettingsViewModel.Factory(MemorySettingsRepository())
            )[SettingsViewModel::class.java]
            tuner = ViewModelProvider(
                store,
                TunerViewModel.Factory(
                    PitchCaptureFactory { _, _, _, onState ->
                        FakeCapture(onState).also(captures::add)
                    },
                    SystemClock::elapsedRealtime
                )
            )[TunerViewModel::class.java]
            presets = ViewModelProvider(
                store, TuningPresetsViewModel.Factory(BuiltInTuningPresetRepository())
            )[TuningPresetsViewModel::class.java]
        }
    }

    @After fun tearDown() {
        compose.runOnUiThread { store.clear() }
    }

    @Test fun settingsDestinationSurvivesRestorationWithoutShowingLoading() {
        val restoration = launchApp()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Audio & Sensitivity").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Audio & Sensitivity").assertIsDisplayed()
        compose.onNodeWithText("Loading settings…").assertDoesNotExist()
    }

    @Test fun presetDestinationFilterAndSelectionSurviveRestoration() {
        val restoration = launchApp()
        compose.onNodeWithText("Presets").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Drop D — DADGBE"))
        compose.onNodeWithText("Drop D — DADGBE").performClick()
        compose.onNodeWithText("Guitar").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Tuning Presets").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(PresetFilter.GUITAR, presets.uiState.value.selectedFilter)
            assertEquals(2, presets.uiState.value.selectedPresetId)
        }
    }

    @Test fun manualModeAndTargetSurviveRestorationWithOneReplacementCapture() {
        val restoration = launchApp()
        compose.onNodeWithText("Manual Tune").performClick()
        compose.runOnIdle { tuner.selectString(6) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Standard · Manual Tune").assertIsDisplayed()
        compose.onNodeWithText("Loading settings…").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(6, tuner.uiState.value.tuning.selectedString.id)
            assertEquals(2, captures.size)
            assertEquals(1, captures.first().stops)
            assertEquals(1, captures.last().starts)
            assertEquals(0, captures.last().stops)
        }
    }

    @Test fun navigatingAwayStillEndsTheTuningSession() {
        launchApp()
        compose.onNodeWithText("Manual Tune").performClick()
        compose.runOnIdle { tuner.selectString(6) }
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Manual Tune").performClick()
        compose.runOnIdle {
            assertEquals(1, tuner.uiState.value.tuning.selectedString.id)
            assertEquals(2, captures.size)
            assertEquals(1, captures.first().stops)
        }
    }

    private fun launchApp(): StateRestorationTester = StateRestorationTester(compose).also { tester ->
        tester.setContent {
            EztunezTheme(dynamicColor = false) {
                App(TunerSettings(), settings, tuner, presets)
            }
        }
    }

    private class FakeCapture(private val onState: (MicrophoneState) -> Unit) : PitchCapture {
        var starts = 0
        var stops = 0
        override fun start() {
            starts++
            onState(MicrophoneState.LISTENING)
        }
        override fun stop() {
            stops++
        }
    }

    private class MemorySettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(TunerSettings())
        override suspend fun setMicSensitivity(value: Float) { settings.value = settings.value.copy(micSensitivity = value) }
        override suspend fun setReferencePitch(value: Int) { settings.value = settings.value.copy(referencePitch = value) }
        override suspend fun setShowRealTimeHz(value: Boolean) { settings.value = settings.value.copy(showRealTimeHz = value) }
        override suspend fun setNotationSystem(value: NotationSystem) { settings.value = settings.value.copy(notationSystem = value) }
        override suspend fun setDarkTheme(value: Boolean) { settings.value = settings.value.copy(darkTheme = value) }
    }
}
