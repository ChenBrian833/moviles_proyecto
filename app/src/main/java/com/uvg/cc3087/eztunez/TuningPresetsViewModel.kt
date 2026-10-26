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
    val selectedPreset: TuningPreset
) {
    val selectedPresetId: Int get() = selectedPreset.id
}

@MainThread
class TuningPresetsViewModel(repository: TuningPresetRepository) : ViewModel() {
    private val allPresets = repository.getPresets().toList()
    private val mutableUiState = MutableStateFlow(
        TuningPresetsUiState(allPresets, selectedPreset = allPresets.first())
    )
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
        val preset = allPresets.firstOrNull { it.id == id } ?: return
        mutableUiState.value = mutableUiState.value.copy(selectedPreset = preset)
    }

    fun endSession() {
        // Reset only the picker filter. The active tuning continues on Home/Tune.
        mutableUiState.value = mutableUiState.value.copy(presets = allPresets, selectedFilter = PresetFilter.ALL)
    }

    class Factory(private val repository: TuningPresetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TuningPresetsViewModel::class.java))
            return TuningPresetsViewModel(repository) as T
        }
    }
}
