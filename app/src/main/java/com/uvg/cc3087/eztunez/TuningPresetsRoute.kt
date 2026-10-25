package com.uvg.cc3087.eztunez

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TuningPresetsRoute(
    viewModel: TuningPresetsViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TuningPresetsScreen(
        uiState = uiState,
        onSelectFilter = viewModel::selectFilter,
        onSelectPreset = viewModel::selectPreset,
        onNavigate = onNavigate
    )
}
