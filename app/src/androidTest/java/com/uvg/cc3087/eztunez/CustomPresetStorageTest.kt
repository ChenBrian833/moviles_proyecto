package com.uvg.cc3087.eztunez

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CustomPresetStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fileName = "preset-test-${UUID.randomUUID()}.db"
    private lateinit var database: PresetDatabase
    private lateinit var repository: RoomTuningPresetRepository

    @Before fun open() {
        database = Room.databaseBuilder(context, PresetDatabase::class.java, fileName).build()
        repository = RoomTuningPresetRepository(database, listOf("Standard", "Drop D"))
    }
    @After fun close() { database.close(); context.deleteDatabase(fileName) }

    @Test fun createEditAndDeleteAreObservedWithoutChangingBuiltIns() = runBlocking {
        val id = repository.saveCustom(CustomPresetDraft(name = "  My  tuning "))
        assertTrue(id < 0)
        assertEquals(BuiltInTuningPresets.all, repository.presets.first().filter { it.id > 0 })
        assertEquals("My tuning", repository.presets.first().single { it.id == id }.name)
        val notes = listOf(38, 45, 50, 55, 59, 64)
        assertEquals(id, repository.saveCustom(CustomPresetDraft(id, "Edited", " Notes ", notes)))
        val edited = repository.presets.first().single { it.id == id }
        assertEquals(notes, edited.midiNotes)
        assertEquals("Notes", edited.description)
        repository.deleteCustom(id)
        assertEquals(BuiltInTuningPresets.all, repository.presets.first())
    }

    @Test fun closingAndReopeningDatabaseKeepsThePresetAndItsId() = runBlocking {
        val id = repository.saveCustom(CustomPresetDraft(name = "Persistent"))
        database.close()
        open()
        assertEquals("Persistent", repository.presets.first().single { it.id == id }.name)
    }

    @Test fun duplicatesAndBuiltInChangesAreRejected() = runBlocking {
        val id = repository.saveCustom(CustomPresetDraft(name = "My tuning"))
        expect(PresetProblem.DUPLICATE_NAME) {
            repository.saveCustom(CustomPresetDraft(name = " MY  TUNING "))
        }
        expect(PresetProblem.DUPLICATE_NAME) {
            repository.saveCustom(CustomPresetDraft(name = " standard "))
        }
        expect(PresetProblem.READ_ONLY) { repository.deleteCustom(1) }
        expect(PresetProblem.READ_ONLY) { repository.saveCustom(CustomPresetDraft(1, "Modified")) }
        assertEquals(id, repository.saveCustom(CustomPresetDraft(id, "my tuning")))
    }

    private suspend fun expect(problem: PresetProblem, action: suspend () -> Unit) {
        try { action(); fail("Expected $problem") }
        catch (error: PresetException) { assertEquals(problem, error.problem) }
    }
}
