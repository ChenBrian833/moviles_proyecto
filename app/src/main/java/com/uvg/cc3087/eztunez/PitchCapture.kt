package com.uvg.cc3087.eztunez

/** One capture. UI callers use QueuedPitchCaptureFactory to dispatch blocking drivers. */
interface PitchCapture {
    fun start()
    fun stop()
}

fun interface PitchCaptureFactory {
    fun create(
        rmsThreshold: Float,
        referencePitch: Int,
        preset: TuningPreset,
        onPitchDetected: (DetectedPitch?) -> Unit,
        onStateChanged: (MicrophoneState) -> Unit
    ): PitchCapture
}
