package com.uvg.cc3087.eztunez

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface PresetSelectionRepository {
    val selectedPresetId: Flow<Int>
    suspend fun select(id: Int)
}

/** In-memory dependency for previews/tests; production uses DataStore. */
class MemoryPresetSelectionRepository(initialId: Int = 1) : PresetSelectionRepository {
    override val selectedPresetId = MutableStateFlow(initialId)
    override suspend fun select(id: Int) { selectedPresetId.value = id }
}
