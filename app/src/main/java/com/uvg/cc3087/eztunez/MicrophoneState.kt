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

fun MicrophoneState.message(): String = when (this) {
    MicrophoneState.PERMISSION_PENDING ->
        "Microphone access is needed to hear and tune your guitar."
    MicrophoneState.PERMISSION_GRANTED -> "Microphone permission granted. Starting…"
    MicrophoneState.PERMISSION_DENIED ->
        "Microphone permission was denied. Allow it to start tuning."
    MicrophoneState.PERMISSION_PERMANENTLY_DENIED ->
        "Microphone permission is blocked. Enable it in app settings."
    MicrophoneState.MICROPHONE_UNAVAILABLE -> "The microphone is not available."
    MicrophoneState.INITIALIZATION_FAILED -> "The microphone could not be initialized."
    MicrophoneState.CAPTURE_INTERRUPTED -> "Microphone capture was interrupted."
    MicrophoneState.LISTENING -> "Listening…"
}
