package com.uvg.cc3087.eztunez

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

// Built-in text comes from resources; user-created preset text stays verbatim.
@Composable
internal fun TuningPreset.localizedName(): String = nameRes?.let { stringResource(it) } ?: name

@Composable
internal fun TuningPreset.localizedDescription(): String =
    descriptionRes?.let { stringResource(it) } ?: description

@get:StringRes
internal val MicrophoneState.messageRes: Int
    get() = when (this) {
        MicrophoneState.PERMISSION_PENDING -> R.string.microphone_permission_pending
        MicrophoneState.PERMISSION_GRANTED -> R.string.microphone_permission_granted
        MicrophoneState.PERMISSION_DENIED -> R.string.microphone_permission_denied
        MicrophoneState.PERMISSION_PERMANENTLY_DENIED -> R.string.microphone_permission_blocked
        MicrophoneState.MICROPHONE_UNAVAILABLE -> R.string.microphone_unavailable
        MicrophoneState.INITIALIZATION_FAILED -> R.string.microphone_initialization_failed
        MicrophoneState.CAPTURE_INTERRUPTED -> R.string.microphone_interrupted
        MicrophoneState.LISTENING -> R.string.microphone_listening
    }
