package com.uvg.cc3087.eztunez

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    settings: TunerSettings,
    onNavigate: (AppScreen) -> Unit
) {
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.commitMicSensitivity()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // A navigation or configuration change can interrupt a slider gesture.
            viewModel.commitMicSensitivity()
        }
    }
    // App already collects these settings for its theme and the tuner.
    SettingsScreen(
        settings = settings,
        onMicSensitivityChange = viewModel::updateMicSensitivity,
        onMicSensitivityChangeFinished = viewModel::commitMicSensitivity,
        onReferencePitchAdjust = viewModel::adjustReferencePitch,
        onDarkThemeChange = viewModel::updateDarkTheme,
        onShowRealTimeHzChange = viewModel::updateShowRealTimeHz,
        onNotationSystemChange = viewModel::updateNotationSystem,
        saveError = saveError,
        onRetrySave = viewModel::retrySave,
        onNavigate = onNavigate
    )
}
