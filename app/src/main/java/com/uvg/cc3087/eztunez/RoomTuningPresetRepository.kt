package com.uvg.cc3087.eztunez

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map

class RoomTuningPresetRepository(
    private val database: PresetDatabase,
    builtInNames: List<String>
) : TuningPresetRepository {
    private val dao = database.customPresets()
    private val reservedNames = builtInNames.map(::presetNameKey).toSet()
    override fun getPresets() = BuiltInTuningPresets.all
    override val presets = dao.observeAll().map { rows -> getPresets() + rows.map { it.toPreset() } }

    override suspend fun saveCustom(draft: CustomPresetDraft): Int {
        validateCustomPreset(draft)
        val key = presetNameKey(draft.name)
        return database.withTransaction {
            val id = draft.id?.let { -it } ?: 0
            if (key in reservedNames || dao.nameExists(key, id)) {
                throw PresetException(PresetProblem.DUPLICATE_NAME)
            }
            val notes = draft.midiNotes
            val entity = CustomPresetEntity(id, cleanPresetName(draft.name), key,
                draft.description.trim(), notes[0], notes[1], notes[2], notes[3], notes[4], notes[5])
            if (draft.id == null) {
                val generatedId = dao.insert(entity)
                check(generatedId in 1..Int.MAX_VALUE.toLong())
                -generatedId.toInt()
            } else {
                if (dao.update(entity) != 1) throw PresetException(PresetProblem.NOT_FOUND)
                draft.id
            }
        }
    }

    override suspend fun deleteCustom(id: Int) {
        if (id >= 0 || id == Int.MIN_VALUE) throw PresetException(PresetProblem.READ_ONLY)
        if (dao.delete(-id) != 1) throw PresetException(PresetProblem.NOT_FOUND)
    }
}
