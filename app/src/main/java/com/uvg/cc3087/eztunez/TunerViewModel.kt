package com.uvg.cc3087.eztunez

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TunerUiState(
    val tuning: TuningState = TuningState(),
    val microphone: MicrophoneState = MicrophoneState.PERMISSION_PENDING
)

/** Owns the tuning session; Android permission requests remain in the route. */
@MainThread
class TunerViewModel(
    private val captureFactory: PitchCaptureFactory,
    private val nowMs: () -> Long
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = mutableUiState.asStateFlow()

    private var tracker: TuningTracker? = null
    private var mode = TuningMode.AUTO
    private var referencePitch = DEFAULT_REFERENCE_PITCH
    private var capture: PitchCapture? = null
    private var captureGeneration = 0L

    fun configure(mode: TuningMode, referencePitch: Int) {
        val normalizedReference = normalizeReferencePitch(referencePitch)
        if (tracker != null && this.mode == mode && this.referencePitch == normalizedReference) {
            return
        }
        stopListening()
        this.mode = mode
        this.referencePitch = normalizedReference
        val newTracker = TuningTracker(mode, normalizedReference)
        tracker = newTracker
        mutableUiState.value = mutableUiState.value.copy(tuning = newTracker.state)
    }

    /** Called only after the route has verified microphone permission. */
    fun startListening(micSensitivity: Float) {
        val currentTracker = tracker ?: return
        stopListening()
        val generation = captureGeneration
        capture = captureFactory.create(
            rmsThreshold = sensitivityToRmsThreshold(micSensitivity),
            referencePitch = referencePitch,
            onPitchDetected = { pitch ->
                viewModelScope.launch {
                    if (generation == captureGeneration) {
                        mutableUiState.value = mutableUiState.value.copy(
                            tuning = currentTracker.update(pitch, nowMs())
                        )
                    }
                }
            },
            onStateChanged = { state ->
                viewModelScope.launch {
                    if (generation == captureGeneration) setMicrophoneState(state)
                }
            }
        ).also { it.start() }
    }

    fun stopListening() {
        // Invalidate callbacks already queued by an old capture before stopping it.
        captureGeneration++
        val previousCapture = capture
        capture = null
        previousCapture?.stop()
    }

    fun selectString(id: Int) {
        val currentTracker = tracker ?: return
        mutableUiState.value = mutableUiState.value.copy(tuning = currentTracker.selectString(id))
    }

    fun setMicrophoneState(state: MicrophoneState) {
        mutableUiState.value = mutableUiState.value.copy(microphone = state)
    }

    fun endSession() {
        stopListening()
        tracker = null
        // Called by navigation when leaving Tune, never just because its UI is recreated.
        mutableUiState.value = TunerUiState()
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    class Factory(
        private val captureFactory: PitchCaptureFactory,
        private val nowMs: () -> Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TunerViewModel::class.java))
            return TunerViewModel(captureFactory, nowMs) as T
        }
    }
}
