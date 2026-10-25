package com.uvg.cc3087.eztunez

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
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

    private class DelayedSettingsRepository : SettingsRepository {
        override val settings = MutableSharedFlow<TunerSettings>(replay = 1)
        override suspend fun setMicSensitivity(value: Float) = Unit
        override suspend fun setReferencePitch(value: Int) = Unit
        override suspend fun setShowRealTimeHz(value: Boolean) = Unit
        override suspend fun setNotationSystem(value: NotationSystem) = Unit
        override suspend fun setDarkTheme(value: Boolean) = Unit
    }
}
