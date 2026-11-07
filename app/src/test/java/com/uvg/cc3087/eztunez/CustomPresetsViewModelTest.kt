package com.uvg.cc3087.eztunez

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CustomPresetsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val custom = TuningPreset(-1, "Custom", listOf(38,45,50,55,59,64), category = PresetCategory.CUSTOM)
    private val repo = FakePresets()
    private val selection = MemoryPresetSelectionRepository(-1)

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun close() { store.clear(); Dispatchers.resetMain() }
    private fun model(repository: TuningPresetRepository = repo): TuningPresetsViewModel =
        TuningPresetsViewModel(repository, selection).also { store.put("presets", it) }
    private fun settle() = dispatcher.scheduler.runCurrent()

    @Test fun waitsForDatabaseBeforeRestoringSelection() {
        val loaded = MutableSharedFlow<List<TuningPreset>>(replay = 1)
        val vm = model(object : TuningPresetRepository {
            override fun getPresets() = BuiltInTuningPresets.all
            override val presets = loaded
        })
        settle()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(-1, selection.selectedPresetId.value)
        loaded.tryEmit(BuiltInTuningPresets.all + custom); settle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(-1, vm.uiState.value.selectedPresetId)
    }

    @Test fun nonexistentSavedSelectionFallsBackToStandardAndPersistsIt() {
        val vm = model(); settle()
        assertEquals(1, vm.uiState.value.selectedPresetId)
        assertEquals(1, selection.selectedPresetId.value)
    }

    @Test fun creationAppearsInAllAndCustomAndSelectionSurvivesNewViewModel() {
        val vm = model(); settle()
        vm.createCustom()
        vm.updateDraft(vm.uiState.value.draft!!.copy(name = "Mine"))
        vm.saveCustom(); settle()
        assertNull(vm.uiState.value.draft)
        assertEquals(6, vm.uiState.value.presets.size)
        vm.selectFilter(PresetFilter.CUSTOM)
        assertEquals(listOf(-1), vm.uiState.value.presets.map { it.id })
        vm.selectPreset(-1); settle()
        val recreated = model(); settle()
        assertEquals(-1, recreated.uiState.value.selectedPresetId)
    }

    @Test fun failedSaveKeepsDraftAndRepeatedClicksDoNotWriteTwice() {
        val vm = model(); settle()
        vm.createCustom(); vm.updateDraft(vm.uiState.value.draft!!.copy(name = "Mine"))
        repo.gate = CompletableDeferred()
        repo.fail = true
        vm.saveCustom(); vm.saveCustom(); settle()
        assertEquals(1, repo.saves)
        assertTrue(vm.uiState.value.isSaving)
        repo.gate!!.complete(Unit); settle()
        assertEquals("Mine", vm.uiState.value.draft!!.name)
        assertEquals(PresetProblem.STORAGE, vm.uiState.value.draftProblem)
        repo.fail = false
        vm.saveCustom(); settle()
        assertEquals(2, repo.saves)
        assertNull(vm.uiState.value.draft)
    }

    @Test fun editPreservesIdentityAndDeleteRequiresConfirmationAndResetsSelection() {
        repo.rows.value += custom
        val vm = model(); settle()
        vm.editCustom(-1)
        vm.updateDraft(vm.uiState.value.draft!!.copy(name = "Edited"))
        vm.saveCustom(); settle()
        assertEquals("Edited", vm.uiState.value.selectedPreset.name)
        vm.requestDelete(-1); settle()
        assertEquals(0, repo.deletes)
        vm.cancelDelete(); assertEquals(0, repo.deletes)
        vm.requestDelete(-1); vm.confirmDelete(); vm.confirmDelete(); settle()
        assertEquals(1, repo.deletes)
        assertEquals(1, vm.uiState.value.selectedPresetId)
        assertEquals(1, selection.selectedPresetId.value)
    }

    @Test fun builtInsCannotBeEditedOrDeletedAndBlankNameCannotBeSaved() {
        val vm = model(); settle()
        vm.editCustom(1); vm.requestDelete(1)
        assertNull(vm.uiState.value.draft); assertNull(vm.uiState.value.deleteTarget)
        vm.createCustom(); vm.saveCustom(); settle()
        assertEquals(PresetProblem.NAME_REQUIRED, vm.uiState.value.draftProblem)
        assertEquals(0, repo.saves)
    }

    @Test fun deletedSelectionFromAnotherWriterFallsBackToStandard() {
        repo.rows.value += custom
        val vm = model(); settle()
        repo.rows.value = BuiltInTuningPresets.all; settle()
        assertEquals(1, vm.uiState.value.selectedPresetId)
        assertEquals(1, selection.selectedPresetId.value)
    }

    @Test fun failedSelectionWriteCanBeRetriedWithoutLosingTheActivePreset() {
        var fail = true
        val stored = MutableStateFlow(1)
        val prefs = object : PresetSelectionRepository {
            override val selectedPresetId = stored
            override suspend fun select(id: Int) {
                if (fail) throw IOException("Test preference failure")
                stored.value = id
            }
        }
        repo.rows.value += custom
        val vm = TuningPresetsViewModel(repo, prefs).also { store.put("presets", it) }
        settle(); vm.selectPreset(-1); settle()
        assertEquals(-1, vm.uiState.value.selectedPresetId)
        assertTrue(vm.uiState.value.selectionSaveFailed)
        fail = false; vm.retrySelectionSave(); settle()
        assertEquals(-1, stored.value)
        assertFalse(vm.uiState.value.selectionSaveFailed)
    }

    @Test fun failedLoadDoesNotEraseSavedSelectionAndCanBeRetried() {
        var fail = true
        val vm = model(object : TuningPresetRepository {
            override fun getPresets() = BuiltInTuningPresets.all
            override val presets: Flow<List<TuningPreset>> get() = flow {
                if (fail) throw IOException("Test read failure")
                emit(BuiltInTuningPresets.all + custom)
            }
        })
        settle()
        assertTrue(vm.uiState.value.loadFailed)
        assertEquals(-1, selection.selectedPresetId.value)
        fail = false; vm.reload(); settle()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals(-1, vm.uiState.value.selectedPresetId)
    }

    private class FakePresets : TuningPresetRepository {
        val rows = MutableStateFlow(BuiltInTuningPresets.all)
        override val presets = rows
        override fun getPresets() = BuiltInTuningPresets.all
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        var saves = 0
        var deletes = 0
        override suspend fun saveCustom(draft: CustomPresetDraft): Int {
            saves++; gate?.await()
            if (fail) throw IOException("Test write failure")
            val id = draft.id ?: -1
            rows.value = rows.value.filterNot { it.id == id } + TuningPreset(
                id, draft.name, draft.midiNotes, draft.description, PresetCategory.CUSTOM
            )
            return id
        }
        override suspend fun deleteCustom(id: Int) { deletes++; rows.value = rows.value.filterNot { it.id == id } }
    }
}
