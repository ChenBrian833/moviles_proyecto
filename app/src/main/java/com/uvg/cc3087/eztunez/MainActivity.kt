package com.uvg.cc3087.eztunez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(DataStoreSettingsRepository(applicationContext))
    }
    private val tunerViewModel: TunerViewModel by viewModels {
        // The retained ViewModel's factory captures only the application context.
        val appContext = applicationContext
        TunerViewModel.Factory(
            captureFactory = PitchCaptureFactory { threshold, reference, preset, onPitch, onState ->
                GuitarPitchDetector(appContext, threshold, reference, onPitch, onState, preset)
            },
            nowMs = android.os.SystemClock::elapsedRealtime
        )
    }
    private val presetsViewModel: TuningPresetsViewModel by viewModels {
        TuningPresetsViewModel.Factory(BuiltInTuningPresetRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow content behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide system bars
        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView
        )

        controller.hide(WindowInsetsCompat.Type.systemBars())

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val settings = settingsViewModel.settings.collectAsStateWithLifecycle().value
            EztunezTheme(
                darkTheme = settings?.darkTheme ?: true,
                dynamicColor = false
            ) {
                if (settings == null) {
                    LoadingScreen()
                } else {
                    App(
                        settings = settings,
                        settingsViewModel = settingsViewModel,
                        tunerViewModel = tunerViewModel,
                        presetsViewModel = presetsViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun App(
    settings: TunerSettings,
    settingsViewModel: SettingsViewModel,
    tunerViewModel: TunerViewModel,
    presetsViewModel: TuningPresetsViewModel
) {
    val presetsState by presetsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedPreset = presetsState.selectedPreset

    var currentScreen by rememberSaveable {
        mutableStateOf(AppScreen.HOME)
    }
    var tuningMode by rememberSaveable { mutableStateOf(TuningMode.AUTO) }

    val navigateTo: (AppScreen) -> Unit = { destination ->
        if (destination != currentScreen) {
            // Ending a screen visit is different from recreating its UI on rotation.
            when (currentScreen) {
                AppScreen.TUNE -> tunerViewModel.endSession()
                AppScreen.PRESETS -> presetsViewModel.endSession()
                else -> Unit
            }
            currentScreen = destination
        }
    }

    when (currentScreen) {
        AppScreen.PRESETS -> TuningPresetsRoute(
            viewModel = presetsViewModel,
            onNavigate = navigateTo
        )
        AppScreen.SETTINGS -> SettingsRoute(
            viewModel = settingsViewModel,
            settings = settings,
            onNavigate = navigateTo
        )
        AppScreen.HOME -> HomeScreen(
            preset = selectedPreset,
            notationSystem = settings.notationSystem,
            onNavigate = navigateTo,
            onStartTuning = { mode ->
                tuningMode = mode
                navigateTo(AppScreen.TUNE)
            }
        )
        AppScreen.TUNE -> TunerRoute(
            viewModel = tunerViewModel,
            preset = selectedPreset,
            mode = tuningMode,
            settings = settings,
            onNavigate = navigateTo
        )
    }
}
