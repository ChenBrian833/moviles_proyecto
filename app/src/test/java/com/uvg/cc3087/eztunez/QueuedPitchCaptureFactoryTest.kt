package com.uvg.cc3087.eztunez

import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

class QueuedPitchCaptureFactoryTest {
    @Test fun startAndStopRunOffCallerAndStopDoesNotWaitForBlockedInitialization() {
        val executor = Executors.newSingleThreadExecutor()
        val enteredStart = CountDownLatch(1)
        val finishStart = CountDownLatch(1)
        val finishedStop = CountDownLatch(1)
        val startThread = AtomicReference<Thread>()
        val stopThread = AtomicReference<Thread>()
        val notifications = AtomicInteger()
        val caller = Thread.currentThread()
        val factory = QueuedPitchCaptureFactory(
            PitchCaptureFactory { _, _, _, _, onState ->
                assertNotSame(caller, Thread.currentThread())
                object : PitchCapture {
                    override fun start() {
                        startThread.set(Thread.currentThread())
                        enteredStart.countDown()
                        check(finishStart.await(5, TimeUnit.SECONDS))
                        onState(MicrophoneState.LISTENING)
                    }
                    override fun stop() {
                        stopThread.set(Thread.currentThread())
                        finishedStop.countDown()
                    }
                }
            },
            executor
        )
        val capture = create(factory, onState = { notifications.incrementAndGet() })
        try {
            capture.start()
            assertTrue(enteredStart.await(5, TimeUnit.SECONDS))
            capture.stop() // Must return even though native initialization has not finished.
            assertEquals(1L, finishedStop.count)
            finishStart.countDown()
            assertTrue(finishedStop.await(5, TimeUnit.SECONDS))
            assertNotSame(caller, startThread.get())
            assertSame(startThread.get(), stopThread.get())
            assertEquals(0, notifications.get())
        } finally {
            finishStart.countDown()
            capture.stop()
            executor.shutdown()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test fun stoppingBeforeTheWorkerRunsSkipsRecorderCreation() {
        val worker = ManualExecutor()
        var creations = 0
        val factory = QueuedPitchCaptureFactory(PitchCaptureFactory { _, _, _, _, _ ->
            creations++
            error("A cancelled start must never open the microphone")
        }, worker)
        val capture = create(factory)
        capture.start()
        capture.start()
        capture.stop()
        capture.stop()
        worker.runAll()
        assertEquals(0, creations)
    }

    @Test fun replacementWaitsForOldCleanupAndLateReadingsAreSuppressedImmediately() {
        val worker = ManualExecutor()
        val events = mutableListOf<String>()
        var nextId = 0
        var firstPitch: ((DetectedPitch?) -> Unit)? = null
        val received = mutableListOf<DetectedPitch?>()
        val factory = QueuedPitchCaptureFactory(PitchCaptureFactory { _, _, _, onPitch, _ ->
            val id = ++nextId
            if (id == 1) firstPitch = onPitch
            object : PitchCapture {
                override fun start() { events.add("start$id") }
                override fun stop() { events.add("stop$id") }
            }
        }, worker)
        val first = create(factory, onPitch = received::add)
        first.start()
        worker.runAll()
        first.stop()
        firstPitch?.invoke(convertFrequency(110f, 0.99f))
        val second = create(factory)
        second.start()
        assertTrue(received.isEmpty())
        assertEquals(listOf("start1"), events)
        worker.runAll()
        assertEquals(listOf("start1", "stop1", "start2"), events)
        first.stop()
        second.stop()
        worker.runAll()
        assertEquals(listOf("start1", "stop1", "start2", "stop2"), events)
    }

    @Test fun failedStartsReleaseTheirDriverAndReportTheFailureOnce() {
        for ((exception, expected) in listOf(
            IllegalStateException() to MicrophoneState.INITIALIZATION_FAILED,
            SecurityException() to MicrophoneState.PERMISSION_DENIED
        )) {
            val worker = ManualExecutor()
            var stops = 0
            var lateState: ((MicrophoneState) -> Unit)? = null
            val states = mutableListOf<MicrophoneState>()
            val factory = QueuedPitchCaptureFactory(PitchCaptureFactory { _, _, _, _, onState ->
                lateState = onState
                object : PitchCapture {
                    override fun start() { throw exception }
                    override fun stop() { stops++ }
                }
            }, worker)
            val capture = create(factory, onState = states::add)
            capture.start()
            worker.runAll()
            capture.stop()
            lateState?.invoke(MicrophoneState.LISTENING)
            worker.runAll()
            assertEquals(1, stops)
            assertEquals(listOf(expected), states)
        }
    }

    private fun create(
        factory: PitchCaptureFactory,
        onPitch: (DetectedPitch?) -> Unit = {},
        onState: (MicrophoneState) -> Unit = {}
    ) = factory.create(DEFAULT_RMS_THRESHOLD, 440, BuiltInTuningPresets.standard, onPitch, onState)

    private class ManualExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }
}
