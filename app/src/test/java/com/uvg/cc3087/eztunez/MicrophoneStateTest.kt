package com.uvg.cc3087.eztunez

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrophoneStateTest {
    @Test fun recoverableMicrophoneFailuresOfferRetry() {
        assertTrue(MicrophoneState.PERMISSION_DENIED.canRetry)
        assertTrue(MicrophoneState.MICROPHONE_UNAVAILABLE.canRetry)
        assertTrue(MicrophoneState.INITIALIZATION_FAILED.canRetry)
        assertTrue(MicrophoneState.CAPTURE_INTERRUPTED.canRetry)
    }

    @Test fun permanentDenialRequiresSettingsInsteadOfRetry() {
        assertFalse(MicrophoneState.PERMISSION_PERMANENTLY_DENIED.canRetry)
        assertTrue(MicrophoneState.PERMISSION_PERMANENTLY_DENIED.message().contains("settings"))
    }
}
