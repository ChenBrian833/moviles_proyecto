package com.uvg.cc3087.eztunez

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Runs the real route/effects with controlled lifecycle, permissions and audio.
 * No system permissions or phone settings are changed by these tests.
 */
class TunerRouteLifecycleTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val captures = mutableListOf<FakeCapture>()
    private lateinit var tuner: TunerViewModel
    private lateinit var owner: TestLifecycleOwner
    private lateinit var permissions: PermissionContext
    private val permissionResults = TestPermissionRegistry()

    @After fun tearDown() {
        compose.runOnUiThread {
            if (::owner.isInitialized) owner.registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }

    @Test fun enteringAnAlreadyResumedScreenStartsExactlyOneCapture() {
        launch()
        assertCaptures(total = 1, active = true)
    }

    @Test fun enteringWhilePausedWaitsForResume() {
        launch(state = Lifecycle.State.STARTED)
        assertCaptures(total = 0, active = false)
        moveTo(Lifecycle.State.RESUMED)
        assertCaptures(total = 1, active = true)
    }

    @Test fun backgroundAndReturnReplaceCaptureOnceAndKeepManualTarget() {
        launch()
        compose.runOnIdle { tuner.selectString(6) }
        moveTo(Lifecycle.State.CREATED)
        assertCaptures(total = 1, active = false)
        moveTo(Lifecycle.State.RESUMED)
        assertCaptures(total = 2, active = true)
        compose.runOnIdle { assertEquals(6, tuner.uiState.value.tuning.selectedString.id) }
    }

    @Test fun pauseAndResumeBetweenFramesStillReplaceCaptureOnlyOnce() {
        launch()
        assertCaptures(total = 1, active = true)
        compose.runOnIdle {
            owner.registry.currentState = Lifecycle.State.STARTED
            owner.registry.currentState = Lifecycle.State.RESUMED
        }
        assertCaptures(total = 2, active = true)
    }

    @Test fun retryAfterAnAudioErrorStartsOneReplacementAndKeepsManualTarget() {
        launch()
        compose.runOnIdle {
            tuner.selectString(6)
            captures.single().fail()
        }
        assertCaptures(total = 1, active = false)
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        assertCaptures(total = 2, active = true)
        compose.runOnIdle { assertEquals(6, tuner.uiState.value.tuning.selectedString.id) }
    }

    @Test fun grantingPermissionWhileResumedStartsOnlyOneCapture() {
        launch(granted = false)
        assertCaptures(total = 0, active = false)
        answerPermission(granted = true)
        assertCaptures(total = 1, active = true)
    }

    @Test fun grantingPermissionWhilePausedWaitsUntilTheScreenResumes() {
        launch(granted = false)
        assertCaptures(total = 0, active = false)
        moveTo(Lifecycle.State.STARTED)
        answerPermission(granted = true)
        assertCaptures(total = 0, active = false)
        moveTo(Lifecycle.State.RESUMED)
        assertCaptures(total = 1, active = true)
    }

    @Test fun denyingPermissionDoesNotCaptureAndRecoveryOnResumeStartsOnce() {
        launch(granted = false)
        assertCaptures(total = 0, active = false)
        answerPermission(granted = false)
        assertCaptures(total = 0, active = false)
        compose.runOnIdle {
            assertTrue(tuner.uiState.value.microphone in setOf(
                MicrophoneState.PERMISSION_DENIED,
                MicrophoneState.PERMISSION_PERMANENTLY_DENIED
            ))
        }
        moveTo(Lifecycle.State.STARTED)
        compose.runOnIdle { permissions.granted = true }
        moveTo(Lifecycle.State.RESUMED)
        assertCaptures(total = 1, active = true)
    }

    @Test fun permissionRevokedInBackgroundPreventsCaptureUntilRecovered() {
        launch()
        assertCaptures(total = 1, active = true)
        moveTo(Lifecycle.State.STARTED)
        compose.runOnIdle { permissions.granted = false }
        moveTo(Lifecycle.State.RESUMED)
        assertCaptures(total = 1, active = false)
        compose.runOnIdle {
            assertEquals(MicrophoneState.PERMISSION_DENIED, tuner.uiState.value.microphone)
        }
        moveTo(Lifecycle.State.STARTED)
        compose.runOnIdle { permissions.granted = true }
        moveTo(Lifecycle.State.RESUMED)
        assertCaptures(total = 2, active = true)
    }

    private fun launch(
        state: Lifecycle.State = Lifecycle.State.RESUMED,
        granted: Boolean = true
    ) {
        compose.runOnUiThread {
            owner = TestLifecycleOwner().apply { registry.currentState = state }
            tuner = ViewModelProvider(
                store,
                TunerViewModel.Factory(
                    PitchCaptureFactory { _, _, _, _, onState ->
                        FakeCapture(onState).also(captures::add)
                    },
                    nowMs = { 0L }
                )
            )[TunerViewModel::class.java]
        }
        compose.setContent {
            // Keep the real Activity as the base context so activity-result and
            // permission-rationale integration still has an Activity to find.
            val context = LocalContext.current
            val permissionContext = remember(context) {
                PermissionContext(context, granted).also { permissions = it }
            }
            CompositionLocalProvider(
                LocalContext provides permissionContext,
                LocalLifecycleOwner provides owner,
                LocalActivityResultRegistryOwner provides permissionResults
            ) {
                EztunezTheme(dynamicColor = false) {
                    TunerRoute(
                        viewModel = tuner,
                        mode = TuningMode.MANUAL,
                        settings = TunerSettings(),
                        onNavigate = {}
                    )
                }
            }
        }
    }

    private fun moveTo(state: Lifecycle.State) {
        compose.runOnIdle { owner.registry.currentState = state }
    }

    private fun answerPermission(granted: Boolean) {
        compose.runOnIdle {
            assertEquals(1, permissionResults.requests)
            permissions.granted = granted
            assertTrue(permissionResults.registry.dispatchResult(
                requireNotNull(permissionResults.pendingRequest), granted
            ))
            permissionResults.pendingRequest = null
        }
    }

    private fun assertCaptures(total: Int, active: Boolean) {
        compose.runOnIdle {
            assertEquals("Unexpected number of capture requests", total, captures.size)
            captures.forEachIndexed { index, capture ->
                assertEquals("Each capture starts once", 1, capture.starts)
                val shouldBeActive = active && index == captures.lastIndex
                assertEquals("Only the latest capture may remain active",
                    if (shouldBeActive) 0 else 1, capture.stops)
            }
            assertEquals(if (active) 1 else 0, captures.count { it.stops == 0 })
            if (active) assertEquals(MicrophoneState.LISTENING, tuner.uiState.value.microphone)
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private class PermissionContext(context: Context, var granted: Boolean) : ContextWrapper(context) {
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
            if (permission == Manifest.permission.RECORD_AUDIO) {
                if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            } else super.checkPermission(permission, pid, uid)
    }

    private class TestPermissionRegistry : ActivityResultRegistryOwner {
        var requests = 0
        var pendingRequest: Int? = null
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                requests++
                pendingRequest = requestCode
            }
        }
        override val activityResultRegistry: ActivityResultRegistry get() = registry
    }

    private class FakeCapture(private val onState: (MicrophoneState) -> Unit) : PitchCapture {
        var starts = 0
        var stops = 0
        override fun start() {
            starts++
            onState(MicrophoneState.LISTENING)
        }
        override fun stop() { stops++ }
        fun fail() { onState(MicrophoneState.INITIALIZATION_FAILED) }
    }
}
