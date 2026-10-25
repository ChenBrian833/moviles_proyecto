package com.uvg.cc3087.eztunez

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Native recorder operations share one queue, including cleanup after a ViewModel is cleared. */
class QueuedPitchCaptureFactory(
    private val delegateFactory: PitchCaptureFactory,
    private val executor: Executor = audioControlExecutor
) : PitchCaptureFactory {
    override fun create(
        rmsThreshold: Float,
        referencePitch: Int,
        preset: TuningPreset,
        onPitchDetected: (DetectedPitch?) -> Unit,
        onStateChanged: (MicrophoneState) -> Unit
    ): PitchCapture = object : PitchCapture {
        private val started = AtomicBoolean(false)
        private val stopped = AtomicBoolean(false)
        // Accessed only by the serial worker, never by the UI thread.
        private var delegate: PitchCapture? = null

        override fun start() {
            if (stopped.get() || !started.compareAndSet(false, true)) return
            executor.execute {
                if (stopped.get()) return@execute
                try {
                    delegate = delegateFactory.create(
                        rmsThreshold, referencePitch, preset,
                        onPitchDetected = { if (!stopped.get()) onPitchDetected(it) },
                        onStateChanged = { if (!stopped.get()) onStateChanged(it) }
                    )
                    if (!stopped.get()) delegate?.start()
                } catch (_: SecurityException) {
                    fail(MicrophoneState.PERMISSION_DENIED)
                } catch (_: RuntimeException) {
                    fail(MicrophoneState.INITIALIZATION_FAILED)
                }
            }
        }

        override fun stop() {
            // Suppress callbacks now, even when native initialization is still in progress.
            if (!stopped.compareAndSet(false, true)) return
            executor.execute { releaseDelegate() }
        }

        private fun fail(state: MicrophoneState) {
            val notify = stopped.compareAndSet(false, true)
            releaseDelegate()
            if (notify) onStateChanged(state)
        }

        private fun releaseDelegate() {
            val previous = delegate
            delegate = null
            try {
                previous?.stop()
            } catch (_: RuntimeException) {
                // Cleanup must not crash the process or strand the control queue.
            }
        }
    }

    companion object {
        // Shared across detector instances: an old stop finishes before the next start.
        private val audioControlExecutor: Executor = Executors.newSingleThreadExecutor { task ->
            Thread(task, "EzTunez-AudioControl").apply { isDaemon = true }
        }
    }
}
