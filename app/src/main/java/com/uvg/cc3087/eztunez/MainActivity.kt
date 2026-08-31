package com.uvg.cc3087.eztunez

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import kotlinx.coroutines.delay
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

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
            EztunezTheme(
                darkTheme = true,
                dynamicColor = false
            ) {
                App()
            }
        }
    }
}

@Composable
fun App() {

    var showLoadingScreen by remember {
        mutableStateOf(true)
    }

    var currentScreen by remember {
        mutableStateOf(AppScreen.HOME)
    }

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
                    onNavigate = {
                        currentScreen = it
                    }
                )
            }


            AppScreen.HOME -> {
                HomeScreen(
                    onNavigate = {
                        currentScreen = it
                    }
                )
            }

            AppScreen.TUNE -> {
                TuneScreen(
                    onNavigate = {
                        currentScreen = it
                    }
                )
            }
        }
    }
}

//