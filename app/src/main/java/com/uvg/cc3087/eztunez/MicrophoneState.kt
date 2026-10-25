package com.uvg.cc3087.eztunez

enum class MicrophoneState {
    PERMISSION_PENDING,
    PERMISSION_GRANTED,
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    MICROPHONE_UNAVAILABLE,
    INITIALIZATION_FAILED,
    CAPTURE_INTERRUPTED,
    LISTENING
}

val MicrophoneState.canRetry: Boolean
    get() = this == MicrophoneState.PERMISSION_DENIED ||
        this == MicrophoneState.MICROPHONE_UNAVAILABLE ||
        this == MicrophoneState.INITIALIZATION_FAILED ||
        this == MicrophoneState.CAPTURE_INTERRUPTED
