package com.uvg.cc3087.eztunez

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = DelayedSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ViewModelProvider(
            store, SettingsViewModel.Factory(repository)
        )[SettingsViewModel::class.java]
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun loadingEndsWhenSettingsArriveWithoutAnArtificialDelay() = runTest {
        backgroundScope.launch { viewModel.settings.collect {} }
        runCurrent()
        assertNull(viewModel.settings.value)

        val persisted = TunerSettings(referencePitch = 432, darkTheme = false)
        repository.settings.emit(persisted)
        runCurrent()
        assertEquals(persisted, viewModel.settings.value)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun recreatingTheCollectorKeepsLoadedSettingsEvenAfterTheSharingTimeout() = runTest {
        val firstCollector = backgroundScope.launch { viewModel.settings.collect {} }
        val persisted = TunerSettings(micSensitivity = 0.9f, referencePitch = 432)
        repository.settings.emit(persisted)
        runCurrent()
        assertEquals(persisted, viewModel.settings.value)

        firstCollector.cancel()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(persisted, viewModel.settings.value)

        backgroundScope.launch { viewModel.settings.collect {} }
        runCurrent()
        assertEquals(persisted, viewModel.settings.value)
    }

    @Test fun sliderUpdatesImmediatelyAndOnlySavesWhenCommitted() = runTest {
        repository.settings.emit(TunerSettings())
        runCurrent()
        repeat(100) { viewModel.updateMicSensitivity(it / 100f) }
        runCurrent()
        assertEquals(0.99f, viewModel.settings.value!!.micSensitivity)
        assertTrue(repository.writes.isEmpty())

        viewModel.commitMicSensitivity()
        viewModel.commitMicSensitivity()
        runCurrent()
        assertEquals(listOf(TunerSettings(micSensitivity = 0.99f)), repository.writes)
    }

    @Test fun rapidTapsAccumulateAndNoOpChangesDoNotWrite() = runTest {
        repository.settings.emit(TunerSettings())
        runCurrent()
        viewModel.updateDarkTheme(true)
        viewModel.updateMicSensitivity(DEFAULT_MIC_SENSITIVITY)
        viewModel.commitMicSensitivity()
        runCurrent()
        assertTrue(repository.writes.isEmpty())

        repeat(10) { viewModel.adjustReferencePitch(1) }
        assertEquals(450, viewModel.settings.value!!.referencePitch)
        runCurrent()
        assertEquals(listOf(TunerSettings(referencePitch = 450)), repository.writes)
    }

    @Test fun failedSaveRetainsChoicesAndRetrySavesLatestValues() = runTest {
        repository.settings.emit(TunerSettings())
        runCurrent()
        repository.failWrites = true
        viewModel.updateDarkTheme(false)
        runCurrent()
        assertTrue(viewModel.saveError.value)
        assertFalse(viewModel.settings.value!!.darkTheme)

        viewModel.adjustReferencePitch(1)
        viewModel.updateMicSensitivity(0.4f)
        viewModel.commitMicSensitivity()
        runCurrent()
        assertTrue(viewModel.saveError.value)
        repository.failWrites = false
        viewModel.retrySave()
        runCurrent()
        assertFalse(viewModel.saveError.value)
        assertEquals(TunerSettings(darkTheme = false, referencePitch = 441, micSensitivity = 0.4f), repository.writes.last())
        assertEquals(repository.writes.last(), viewModel.settings.value)
    }

    @Test fun changesDuringSlowSaveStayVisibleAndSaveAfterwards() = runTest {
        repository.settings.emit(TunerSettings())
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        repository.writeGate = gate
        viewModel.adjustReferencePitch(1)
        runCurrent()
        repeat(4) { viewModel.adjustReferencePitch(1) }
        viewModel.updateMicSensitivity(0.3f)
        viewModel.updateDarkTheme(false)
        runCurrent()
        assertEquals(1, repository.writes.size)
        assertEquals(445, viewModel.settings.value!!.referencePitch)

        gate.complete(Unit)
        runCurrent()
        assertEquals(2, repository.writes.size)
        assertEquals(TunerSettings(referencePitch = 445, darkTheme = false), repository.writes.last())
        assertEquals(0.3f, viewModel.settings.value!!.micSensitivity)
        viewModel.commitMicSensitivity()
        runCurrent()
        assertEquals(viewModel.settings.value, repository.writes.last())
    }

    private class DelayedSettingsRepository : SettingsRepository {
        override val settings = MutableSharedFlow<TunerSettings>(replay = 1)
        val writes = mutableListOf<TunerSettings>()
        var failWrites = false
        var writeGate: CompletableDeferred<Unit>? = null
        override suspend fun saveSettings(settings: TunerSettings) {
            writes += settings
            writeGate?.await()
            if (failWrites) throw IOException("Disk full")
            this.settings.emit(settings)
        }
        override suspend fun setMicSensitivity(value: Float) = Unit
        override suspend fun setReferencePitch(value: Int) = Unit
        override suspend fun setShowRealTimeHz(value: Boolean) = Unit
        override suspend fun setNotationSystem(value: NotationSystem) = Unit
        override suspend fun setDarkTheme(value: Boolean) = Unit
    }
}
