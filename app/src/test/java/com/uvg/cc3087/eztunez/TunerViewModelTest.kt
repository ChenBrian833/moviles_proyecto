package com.uvg.cc3087.eztunez

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val captures = mutableListOf<FakeCapture>()
    private var now = 0L
    private lateinit var viewModel: TunerViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ViewModelProvider(
            store,
            TunerViewModel.Factory(
                PitchCaptureFactory { threshold, reference, onPitch, onState ->
                    FakeCapture(threshold, reference, onPitch, onState).also(captures::add)
                },
                nowMs = { now }
            )
        )[TunerViewModel::class.java]
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun configuringOrDenyingPermissionDoesNotOpenTheMicrophone() {
        viewModel.configure(TuningMode.AUTO, 440)
        viewModel.setMicrophoneState(MicrophoneState.PERMISSION_PERMANENTLY_DENIED)
        assertTrue(captures.isEmpty())
        assertEquals(MicrophoneState.PERMISSION_PERMANENTLY_DENIED, viewModel.uiState.value.microphone)
    }

    @Test fun automaticTuningStillFollowsTheDetectedString() {
        start(TuningMode.AUTO)
        play(110f)
        assertEquals("A2", viewModel.uiState.value.tuning.selectedString.note)
        assertTrue(viewModel.uiState.value.tuning.isInTune)
        assertEquals(MicrophoneState.LISTENING, viewModel.uiState.value.microphone)
    }

    @Test fun manualTargetDoesNotFollowThePlayedStringAndSelectionClearsReading() {
        start(TuningMode.MANUAL)
        viewModel.selectString(6)
        play(110f)
        assertEquals("E4", viewModel.uiState.value.tuning.selectedString.note)
        assertFalse(viewModel.uiState.value.tuning.isInTune)
        assertNotNull(viewModel.uiState.value.tuning.pitch)

        viewModel.selectString(2)
        assertEquals("A2", viewModel.uiState.value.tuning.selectedString.note)
        assertNull(viewModel.uiState.value.tuning.pitch)
        play(110f)
        assertTrue(viewModel.uiState.value.tuning.isInTune)
    }

    @Test fun sensitivityChangeReplacesCaptureButPreservesManualSelection() {
        start(TuningMode.MANUAL)
        viewModel.selectString(4)
        val previous = captures.single()
        viewModel.stopListening()
        viewModel.configure(TuningMode.MANUAL, 440)
        viewModel.startListening(0.25f)

        assertEquals(1, previous.stops)
        assertEquals(1, captures.last().starts)
        assertEquals(sensitivityToRmsThreshold(0.25f), captures.last().threshold, 0f)
        assertEquals(4, viewModel.uiState.value.tuning.selectedString.id)
    }

    @Test fun changedReferenceReconfiguresBothTargetsAndCapture() {
        start(TuningMode.MANUAL)
        viewModel.selectString(6)
        viewModel.stopListening()
        viewModel.configure(TuningMode.MANUAL, 432)
        viewModel.startListening(DEFAULT_MIC_SENSITIVITY)

        assertEquals(432, captures.last().reference)
        assertEquals(1, viewModel.uiState.value.tuning.selectedString.id)
        viewModel.selectString(2)
        play(108f)
        assertEquals(108f, viewModel.uiState.value.tuning.selectedString.frequency, 0.001f)
        assertTrue(viewModel.uiState.value.tuning.isInTune)
    }

    @Test fun changedModeStartsWithFreshTracker() {
        start(TuningMode.MANUAL)
        viewModel.selectString(6)
        viewModel.configure(TuningMode.AUTO, 440)
        viewModel.startListening(DEFAULT_MIC_SENSITIVITY)
        assertEquals(1, captures.first().stops)
        assertEquals(1, viewModel.uiState.value.tuning.selectedString.id)
        play(110f)
        assertEquals(2, viewModel.uiState.value.tuning.selectedString.id)
    }

    @Test fun retryIgnoresBothQueuedAndLateCallbacksFromOldCapture() {
        start(TuningMode.MANUAL)
        viewModel.selectString(2)
        play(110f)
        val previous = captures.single()
        val before = viewModel.uiState.value.tuning
        now += 1_000L
        previous.onPitch(null) // Queued before the old capture is stopped.
        previous.onState(MicrophoneState.CAPTURE_INTERRUPTED)
        viewModel.startListening(DEFAULT_MIC_SENSITIVITY)
        previous.onState(MicrophoneState.INITIALIZATION_FAILED) // Late callback after replacement.
        dispatcher.scheduler.runCurrent()

        assertEquals(1, previous.stops)
        assertEquals(before, viewModel.uiState.value.tuning)
        assertEquals(MicrophoneState.LISTENING, viewModel.uiState.value.microphone)
    }

    @Test fun stopInvalidatesCallbacksAndIsSafeToRepeat() {
        start(TuningMode.AUTO)
        val previous = captures.single()
        viewModel.stopListening()
        val before = viewModel.uiState.value
        previous.onPitch(convertFrequency(110f, 0.99f))
        previous.onState(MicrophoneState.CAPTURE_INTERRUPTED)
        viewModel.stopListening()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, previous.stops)
        assertEquals(before, viewModel.uiState.value)
    }

    @Test fun recreatingTheUiKeepsManualTargetAndReplacesOnlyTheCapture() {
        start(TuningMode.MANUAL)
        viewModel.selectString(6)
        play(329.62756f)
        val previousCapture = captures.single()
        val before = viewModel.uiState.value.tuning

        // The old route disposes its capture. The retained ViewModel is then
        // reattached by the new route with the same mode/settings.
        viewModel.stopListening()
        viewModel.configure(TuningMode.MANUAL, 440)
        viewModel.startListening(DEFAULT_MIC_SENSITIVITY)
        previousCapture.onState(MicrophoneState.CAPTURE_INTERRUPTED)
        dispatcher.scheduler.runCurrent()

        assertEquals(before, viewModel.uiState.value.tuning)
        assertEquals(1, previousCapture.stops)
        assertEquals(2, captures.size)
        assertEquals(1, captures.last().starts)
        assertEquals(MicrophoneState.LISTENING, viewModel.uiState.value.microphone)
        play(329.62756f)
        assertEquals(6, viewModel.uiState.value.tuning.selectedString.id)
        assertTrue(viewModel.uiState.value.tuning.isInTune)
    }

    @Test fun leavingAndReturningKeepsTheExistingFreshSessionBehavior() {
        start(TuningMode.MANUAL)
        viewModel.selectString(6)
        val previous = captures.single()
        viewModel.endSession()
        previous.onState(MicrophoneState.CAPTURE_INTERRUPTED)
        dispatcher.scheduler.runCurrent()
        assertEquals(TunerUiState(), viewModel.uiState.value)

        start(TuningMode.MANUAL)
        assertEquals(1, previous.stops)
        assertEquals(2, captures.size)
        assertEquals(1, viewModel.uiState.value.tuning.selectedString.id)
        assertNull(viewModel.uiState.value.tuning.pitch)
    }

    @Test fun clearingViewModelReleasesCaptureAndDiscardsPendingCallbacks() {
        start(TuningMode.AUTO)
        val previous = captures.single()
        val before = viewModel.uiState.value
        previous.onState(MicrophoneState.CAPTURE_INTERRUPTED)
        store.clear()
        previous.onState(MicrophoneState.INITIALIZATION_FAILED)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, previous.stops)
        assertEquals(before, viewModel.uiState.value)
    }

    @Test fun microphoneErrorsAreExposedForTheExistingRetryUi() {
        start(TuningMode.AUTO)
        captures.single().onState(MicrophoneState.MICROPHONE_UNAVAILABLE)
        dispatcher.scheduler.runCurrent()
        assertEquals(MicrophoneState.MICROPHONE_UNAVAILABLE, viewModel.uiState.value.microphone)
        assertTrue(viewModel.uiState.value.microphone.canRetry)
    }

    @Test fun quietPcmNotesProduceTheSameStatesAsThePreRefactorTrackerPath() {
        // Before MVVM, TuneScreen passed each analyzed frame directly to TuningTracker
        // on the main thread. Compare that path to the ViewModel after every callback,
        // including audio below the RMS gate and quiet notes above it.
        for (mode in TuningMode.entries) {
            for (reference in listOf(440, 432)) {
                for (target in guitarStringsForReference(reference)) {
                    for (amplitude in listOf(0.0002f, 0.0006f, 0.001f)) {
                        viewModel.endSession()
                        viewModel.configure(mode, reference)
                        viewModel.startListening(DEFAULT_MIC_SENSITIVITY)
                        dispatcher.scheduler.runCurrent()
                        val capture = captures.last()
                        assertEquals(DEFAULT_RMS_THRESHOLD, capture.threshold, 0.000000001f)
                        assertEquals(reference, capture.reference)
                        val analyzer = GuitarSignalAnalyzer(capture.threshold, capture.reference)
                        val previousTracker = TuningTracker(mode, reference)
                        if (mode == TuningMode.MANUAL) {
                            previousTracker.selectString(target.id)
                            viewModel.selectString(target.id)
                        }

                        repeat(16) { frame ->
                            val block = FloatArray(GuitarSignalAnalyzer.HOP_SIZE) { index ->
                                val sample = frame * GuitarSignalAnalyzer.HOP_SIZE + index
                                val phase = 2.0 * PI * target.frequency * sample /
                                    GuitarSignalAnalyzer.SAMPLE_RATE
                                // Apply the same PCM16 quantization as microphone input.
                                (amplitude * sin(phase) * 32768).toInt() / 32768f
                            }
                            now = frame * GuitarSignalAnalyzer.HOP_SIZE * 1000L /
                                GuitarSignalAnalyzer.SAMPLE_RATE
                            val pitch = analyzer.analyze(block)
                            val expected = previousTracker.update(pitch, now)
                            capture.onPitch(pitch)
                            dispatcher.scheduler.runCurrent()
                            assertEquals(
                                "$mode A4=$reference ${target.note} amplitude=$amplitude frame=$frame",
                                expected,
                                viewModel.uiState.value.tuning
                            )
                        }
                        if (amplitude >= 0.0006f) {
                            assertEquals(target.id, viewModel.uiState.value.tuning.selectedString.id)
                            assertTrue(viewModel.uiState.value.tuning.isInTune)
                        } else {
                            assertNull(viewModel.uiState.value.tuning.pitch)
                        }
                    }
                }
            }
        }
    }

    private fun start(mode: TuningMode) {
        viewModel.configure(mode, 440)
        viewModel.startListening(DEFAULT_MIC_SENSITIVITY)
        dispatcher.scheduler.runCurrent()
    }

    private fun play(frequency: Float) {
        repeat(6) {
            now += 93L
            captures.last().onPitch(convertFrequency(frequency, 0.99f))
            dispatcher.scheduler.runCurrent()
        }
    }

    private class FakeCapture(
        val threshold: Float,
        val reference: Int,
        val onPitch: (DetectedPitch?) -> Unit,
        val onState: (MicrophoneState) -> Unit
    ) : PitchCapture {
        var starts = 0
        var stops = 0
        override fun start() {
            starts++
            onState(MicrophoneState.LISTENING)
        }
        override fun stop() {
            stops++
            // Some implementations can report interruption during shutdown.
            onState(MicrophoneState.CAPTURE_INTERRUPTED)
        }
    }
}
