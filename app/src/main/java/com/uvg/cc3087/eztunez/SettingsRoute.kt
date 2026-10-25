package com.uvg.cc3087.eztunez

import androidx.compose.runtime.Composable

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    settings: TunerSettings,
    onNavigate: (AppScreen) -> Unit
) {
    // App already collects these settings for its theme and the tuner.
    SettingsScreen(
        settings = settings,
        onMicSensitivityChange = viewModel::updateMicSensitivity,
        onReferencePitchChange = viewModel::updateReferencePitch,
        onDarkThemeChange = viewModel::updateDarkTheme,
        onShowRealTimeHzChange = viewModel::updateShowRealTimeHz,
        onNotationSystemChange = viewModel::updateNotationSystem,
        onNavigate = onNavigate
    )
}
