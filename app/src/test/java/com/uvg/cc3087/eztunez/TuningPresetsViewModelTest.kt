package com.uvg.cc3087.eztunez

import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TuningPresetsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun ready(model: TuningPresetsViewModel): TuningPresetsViewModel {
        store.put("presets", model)
        dispatcher.scheduler.runCurrent()
        return model
    }
    private val guitar = BuiltInTuningPresets.standard
    private val custom = TuningPreset(8, "My tuning", listOf(38, 45, 50, 55, 59, 64), "Custom", PresetCategory.CUSTOM)

    private fun viewModel() = ready(TuningPresetsViewModel(object : TuningPresetRepository {
        override fun getPresets() = listOf(guitar, custom)
    }))

    @Test fun filtersUseRepositoryCategoriesAndAllRestoresTheCatalog() {
        val viewModel = viewModel()
        viewModel.selectFilter(PresetFilter.GUITAR)
        assertEquals(listOf(guitar), viewModel.uiState.value.presets)
        viewModel.selectFilter(PresetFilter.CUSTOM)
        assertEquals(listOf(custom), viewModel.uiState.value.presets)
        assertEquals(PresetFilter.CUSTOM, viewModel.uiState.value.selectedFilter)
        viewModel.selectFilter(PresetFilter.ALL)
        assertEquals(listOf(guitar, custom), viewModel.uiState.value.presets)
    }

    @Test fun filteringDoesNotChangeTheSelectedPreset() {
        val viewModel = viewModel()
        viewModel.selectPreset(custom.id)
        viewModel.selectFilter(PresetFilter.GUITAR)
        assertEquals(custom.id, viewModel.uiState.value.selectedPresetId)
        viewModel.selectFilter(PresetFilter.ALL)
        assertEquals(custom.id, viewModel.uiState.value.selectedPresetId)
    }

    @Test fun unknownPresetCannotBecomeSelected() {
        val viewModel = viewModel()
        viewModel.selectPreset(custom.id)
        viewModel.selectPreset(999)
        assertEquals(custom.id, viewModel.uiState.value.selectedPresetId)
    }

    @Test fun leavingPresetsResetsTheFilterButKeepsTheActiveTuning() {
        val viewModel = viewModel()
        viewModel.selectPreset(custom.id)
        viewModel.selectFilter(PresetFilter.CUSTOM)
        viewModel.endSession()
        assertEquals(PresetFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(custom, viewModel.uiState.value.selectedPreset)
        assertEquals(listOf(guitar, custom), viewModel.uiState.value.presets)
    }

    @Test fun builtInCatalogStillStartsAtStandardAndHasNoCustomPresets() {
        val viewModel = ready(TuningPresetsViewModel(BuiltInTuningPresetRepository()))
        assertEquals(listOf(1, 2, 3, 4, 5), viewModel.uiState.value.presets.map { it.id })
        val selected = viewModel.uiState.value.presets.single {
            it.id == viewModel.uiState.value.selectedPresetId
        }
        assertEquals(R.string.preset_standard, selected.nameRes)
        assertEquals("E A D G B E", selected.tuning)
        viewModel.selectFilter(PresetFilter.CUSTOM)
        assertTrue(viewModel.uiState.value.presets.isEmpty())
    }
}
