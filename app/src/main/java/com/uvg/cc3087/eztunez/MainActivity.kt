package com.uvg.cc3087.eztunez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import kotlinx.coroutines.delay
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(DataStoreSettingsRepository(applicationContext))
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
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            EztunezTheme(
                darkTheme = settings.darkTheme,
                dynamicColor = false
            ) {
                App(settings = settings, settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun App(
    settings: TunerSettings,
    settingsViewModel: SettingsViewModel
) {

    var showLoadingScreen by remember {
        mutableStateOf(true)
    }

    var currentScreen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    var tuningMode by remember { mutableStateOf(TuningMode.AUTO) }

    LaunchedEffect(Unit) {
        delay(1500)
        showLoadingScreen = false
    }

    if (showLoadingScreen) {

        LoadingScreen()

    } else {

        when (currentScreen) {

            AppScreen.PRESETS -> {
                TuningPresetsScreen(
                    onNavigate = {
                        currentScreen = it
                    }
                )
            }

            AppScreen.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    onMicSensitivityChange = settingsViewModel::updateMicSensitivity,
                    onReferencePitchChange = settingsViewModel::updateReferencePitch,
                    onDarkThemeChange = settingsViewModel::updateDarkTheme,
                    onShowRealTimeHzChange = settingsViewModel::updateShowRealTimeHz,
                    onNotationSystemChange = settingsViewModel::updateNotationSystem,
                    onNavigate = {
                        currentScreen = it
                    }
                )
            }


            AppScreen.HOME -> {
                HomeScreen(
                    onNavigate = {
                        currentScreen = it
                    },
                    onStartTuning = { mode ->
                        tuningMode = mode
                        currentScreen = AppScreen.TUNE
                    }
                )
            }

            AppScreen.TUNE -> {
                TuneScreen(
                    mode = tuningMode,
                    settings = settings,
                    onNavigate = {
                        currentScreen = it
                    }
                )
            }
        }
    }
}

//
