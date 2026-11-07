package com.uvg.cc3087.eztunez

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TuningPresetsRoute(viewModel: TuningPresetsViewModel, onNavigate: (AppScreen) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = state.draft
    if (draft != null) {
        CustomPresetEditor(draft, state.isSaving, state.draftProblem,
            viewModel::updateDraft, viewModel::saveCustom, viewModel::cancelEdit)
    } else {
        TuningPresetsScreen(state, viewModel::selectFilter, viewModel::selectPreset, onNavigate,
            viewModel::createCustom, viewModel::editCustom, viewModel::requestDelete, viewModel::retrySelectionSave)
    }
    state.deleteTarget?.let { preset ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.custom_delete_title)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(stringResource(R.string.custom_delete_message, preset.name))
                    state.problem?.let { Text(stringResource(it.messageRes), color = MaterialTheme.colorScheme.error) }
                    if (state.isDeleting) Text(stringResource(R.string.custom_deleting))
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete, enabled = !state.isDeleting) {
                Text(stringResource(R.string.custom_delete))
            } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete, enabled = !state.isDeleting) {
                Text(stringResource(R.string.custom_cancel))
            } }
        )
    }
}
