package com.uvg.cc3087.eztunez

/** One microphone capture. Call start/stop from the owning UI thread. */
interface PitchCapture {
    fun start()
    fun stop()
}

fun interface PitchCaptureFactory {
    fun create(
        rmsThreshold: Float,
        referencePitch: Int,
        onPitchDetected: (DetectedPitch?) -> Unit,
        onStateChanged: (MicrophoneState) -> Unit
    ): PitchCapture
}
