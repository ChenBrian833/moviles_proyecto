package com.uvg.cc3087.eztunez

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PresetFilter { ALL, GUITAR, CUSTOM }

data class TuningPresetsUiState(
    val presets: List<TuningPreset>,
    val selectedFilter: PresetFilter = PresetFilter.ALL,
    val selectedPreset: TuningPreset,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val problem: PresetProblem? = null,
    val selectionSaveFailed: Boolean = false,
    val draft: CustomPresetDraft? = null,
    val draftProblem: PresetProblem? = null,
    val isSaving: Boolean = false,
    val deleteTarget: TuningPreset? = null,
    val isDeleting: Boolean = false
) {
    val selectedPresetId: Int get() = selectedPreset.id
}

@MainThread
class TuningPresetsViewModel(
    private val repository: TuningPresetRepository,
    private val selection: PresetSelectionRepository = MemoryPresetSelectionRepository()
) : ViewModel() {
    private var allPresets = repository.getPresets().toList()
    private val mutableUiState = MutableStateFlow(TuningPresetsUiState(
        allPresets, selectedPreset = BuiltInTuningPresets.standard
    ))
    val uiState = mutableUiState.asStateFlow()
    private var restored = false
    private var desiredId = 1
    private var observation: Job? = null
    private val selectionWrites = Channel<Int>(Channel.CONFLATED)

    init {
        // Serialize preference writes so a slower previous selection cannot win.
        viewModelScope.launch {
            for (id in selectionWrites) {
                try {
                    selection.select(id)
                    mutableUiState.value = mutableUiState.value.copy(selectionSaveFailed = false)
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) {
                    mutableUiState.value = mutableUiState.value.copy(selectionSaveFailed = true)
                }
            }
        }
        reload()
    }

    fun reload() {
        observation?.cancel()
        mutableUiState.value = mutableUiState.value.copy(isLoading = true, loadFailed = false)
        observation = viewModelScope.launch {
            try {
                combine(repository.presets, selection.selectedPresetId) { presets, savedId -> presets to savedId }
                    .collect { (presets, savedId) ->
                        allPresets = presets
                        if (!restored) { desiredId = savedId; restored = true }
                        val selected = presets.firstOrNull { it.id == desiredId } ?: BuiltInTuningPresets.standard
                        if (desiredId != selected.id) {
                            desiredId = selected.id
                            selectionWrites.trySend(desiredId)
                        }
                        mutableUiState.value = mutableUiState.value.copy(
                            isLoading = false, loadFailed = false, selectedPreset = selected,
                            presets = filtered(mutableUiState.value.selectedFilter)
                        )
                    }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                mutableUiState.value = mutableUiState.value.copy(isLoading = false, loadFailed = true)
            }
        }
    }

    private fun filtered(filter: PresetFilter) = when (filter) {
        PresetFilter.ALL -> allPresets
        PresetFilter.GUITAR -> allPresets.filter { it.category == PresetCategory.GUITAR }
        PresetFilter.CUSTOM -> allPresets.filter { it.category == PresetCategory.CUSTOM }
    }

    fun selectFilter(filter: PresetFilter) {
        mutableUiState.value = mutableUiState.value.copy(presets = filtered(filter), selectedFilter = filter)
    }

    fun selectPreset(id: Int) {
        if (uiState.value.isLoading || uiState.value.loadFailed) return
        val preset = allPresets.firstOrNull { it.id == id } ?: return
        desiredId = id
        mutableUiState.value = mutableUiState.value.copy(selectedPreset = preset)
        selectionWrites.trySend(id)
    }

    fun retrySelectionSave() { selectionWrites.trySend(desiredId) }

    fun createCustom() {
        if (!uiState.value.isLoading && !uiState.value.isSaving) {
            mutableUiState.value = mutableUiState.value.copy(draft = CustomPresetDraft(), draftProblem = null)
        }
    }

    fun editCustom(id: Int) {
        val preset = allPresets.firstOrNull { it.id == id && it.category == PresetCategory.CUSTOM && it.id < 0 } ?: return
        mutableUiState.value = mutableUiState.value.copy(
            draft = CustomPresetDraft(preset.id, preset.name, preset.description, preset.midiNotes), draftProblem = null
        )
    }

    fun updateDraft(draft: CustomPresetDraft) {
        if (!uiState.value.isSaving && uiState.value.draft != null && uiState.value.draft?.id == draft.id) {
            mutableUiState.value = mutableUiState.value.copy(draft = draft, draftProblem = null)
        }
    }

    fun cancelEdit() {
        if (!uiState.value.isSaving) mutableUiState.value = mutableUiState.value.copy(draft = null, draftProblem = null)
    }

    fun saveCustom() {
        if (uiState.value.isSaving) return
        val draft = uiState.value.draft ?: return
        try { validateCustomPreset(draft) }
        catch (error: PresetException) {
            mutableUiState.value = mutableUiState.value.copy(draftProblem = error.problem)
            return
        }
        mutableUiState.value = mutableUiState.value.copy(isSaving = true, draftProblem = null)
        viewModelScope.launch {
            try {
                repository.saveCustom(draft)
                mutableUiState.value = mutableUiState.value.copy(isSaving = false, draft = null)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isSaving = false, draftProblem = (error as? PresetException)?.problem ?: PresetProblem.STORAGE
                )
            }
        }
    }

    fun requestDelete(id: Int) {
        val preset = allPresets.firstOrNull { it.id == id && it.category == PresetCategory.CUSTOM && it.id < 0 } ?: return
        mutableUiState.value = mutableUiState.value.copy(deleteTarget = preset, problem = null)
    }

    fun cancelDelete() {
        if (!uiState.value.isDeleting) mutableUiState.value = mutableUiState.value.copy(deleteTarget = null, problem = null)
    }

    fun confirmDelete() {
        if (uiState.value.isDeleting) return
        val preset = uiState.value.deleteTarget ?: return
        mutableUiState.value = mutableUiState.value.copy(isDeleting = true, problem = null)
        viewModelScope.launch {
            try {
                repository.deleteCustom(preset.id)
                // The Room flow also handles deletion by another repository instance.
                if (desiredId == preset.id) {
                    desiredId = 1
                    mutableUiState.value = mutableUiState.value.copy(selectedPreset = BuiltInTuningPresets.standard)
                    selectionWrites.trySend(1)
                }
                mutableUiState.value = mutableUiState.value.copy(isDeleting = false, deleteTarget = null)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isDeleting = false, problem = (error as? PresetException)?.problem ?: PresetProblem.STORAGE
                )
            }
        }
    }

    fun endSession() { selectFilter(PresetFilter.ALL) }

    class Factory(
        private val repository: TuningPresetRepository,
        private val selection: PresetSelectionRepository = MemoryPresetSelectionRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TuningPresetsViewModel::class.java))
            return TuningPresetsViewModel(repository, selection) as T
        }
    }
}
