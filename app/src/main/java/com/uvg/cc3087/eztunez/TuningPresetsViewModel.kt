package com.uvg.cc3087.eztunez

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PresetFilter { ALL, GUITAR, CUSTOM }

data class TuningPresetsUiState(
    val presets: List<TuningPreset>,
    val selectedFilter: PresetFilter = PresetFilter.ALL,
    val selectedPresetId: Int = 1
)

@MainThread
class TuningPresetsViewModel(repository: TuningPresetRepository) : ViewModel() {
    private val allPresets = repository.getPresets().toList()
    private val mutableUiState = MutableStateFlow(TuningPresetsUiState(allPresets))
    val uiState: StateFlow<TuningPresetsUiState> = mutableUiState.asStateFlow()

    fun selectFilter(filter: PresetFilter) {
        val filtered = when (filter) {
            PresetFilter.ALL -> allPresets
            PresetFilter.GUITAR -> allPresets.filter { it.category == PresetCategory.GUITAR }
            PresetFilter.CUSTOM -> allPresets.filter { it.category == PresetCategory.CUSTOM }
        }
        mutableUiState.value = mutableUiState.value.copy(presets = filtered, selectedFilter = filter)
    }

    fun selectPreset(id: Int) {
        if (allPresets.any { it.id == id }) {
            mutableUiState.value = mutableUiState.value.copy(selectedPresetId = id)
        }
    }

    fun endSession() {
        // Called by navigation when leaving Presets, not by configuration-change disposal.
        mutableUiState.value = TuningPresetsUiState(allPresets)
    }

    class Factory(private val repository: TuningPresetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TuningPresetsViewModel::class.java))
            return TuningPresetsViewModel(repository) as T
        }
    }
}
