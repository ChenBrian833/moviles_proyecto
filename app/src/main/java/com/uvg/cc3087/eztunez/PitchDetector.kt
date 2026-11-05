package com.uvg.cc3087.eztunez

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

/** Blocking driver; production calls are serialized by QueuedPitchCaptureFactory. */
class GuitarPitchDetector(
    private val context: Context,
    private val rmsThreshold: Float = DEFAULT_RMS_THRESHOLD,
    private val referencePitch: Int = DEFAULT_REFERENCE_PITCH,
    private val onPitchDetected: (DetectedPitch?) -> Unit,
    private val onStateChanged: (MicrophoneState) -> Unit = {},
    private val preset: TuningPreset = BuiltInTuningPresets.standard
) : PitchCapture {
    @Volatile private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRunning = false

    @WorkerThread
    override fun start() {
        if (isRunning) return
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onStateChanged(MicrophoneState.PERMISSION_DENIED)
            return
        }

        val openResult = openRecorder()
        val record = openResult.record
        if (record == null) {
            onStateChanged(openResult.failure ?: MicrophoneState.INITIALIZATION_FAILED)
            return
        }
        audioRecord = record
        isRunning = true
        onStateChanged(MicrophoneState.LISTENING)
        var launched = false
        try {
            recordingThread = thread(start = true, name = "EzTunez-Microphone") {
                readMicrophone(record)
            }
            launched = true
        } finally {
            if (!launched) {
                isRunning = false
                audioRecord = null
                releaseRecorder(record)
            }
        }
    }

    private data class OpenResult(
        val record: AudioRecord? = null,
        val failure: MicrophoneState? = null
    )

    private fun openRecorder(): OpenResult {
        val minimum = AudioRecord.getMinBufferSize(
            GuitarSignalAnalyzer.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minimum <= 0) return OpenResult(failure = MicrophoneState.MICROPHONE_UNAVAILABLE)
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return OpenResult(failure = MicrophoneState.MICROPHONE_UNAVAILABLE)
        val supportsRaw = manager.getProperty(
            AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED
        ) == "true"
        // MIC is the capture path already verified on the user's device.
        // Alternative sources are only initialization fallbacks, never the default.
        val sources = buildList {
            add(MediaRecorder.AudioSource.MIC)
            if (supportsRaw) add(MediaRecorder.AudioSource.UNPROCESSED)
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        }

        for (source in sources) {
            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    source, GuitarSignalAnalyzer.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minimum, GuitarSignalAnalyzer.WINDOW_SIZE * 4)
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    record.startRecording()
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        return OpenResult(record = record)
                    }
                }
            } catch (_: IllegalArgumentException) {
                // Try the next source if this device does not support the preferred one.
            } catch (_: IllegalStateException) {
            } catch (_: SecurityException) {
                record?.release()
                return OpenResult(failure = MicrophoneState.PERMISSION_DENIED)
            }
            record?.release()
        }
        return OpenResult(failure = MicrophoneState.INITIALIZATION_FAILED)
    }

    private fun readMicrophone(initialRecord: AudioRecord) {
        var record: AudioRecord? = initialRecord
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val shorts = ShortArray(GuitarSignalAnalyzer.HOP_SIZE)
            val samples = FloatArray(GuitarSignalAnalyzer.HOP_SIZE)
            var analyzer = GuitarSignalAnalyzer(rmsThreshold, referencePitch, preset)
            var filled = 0
            var recoveries = 0
            while (isRunning) {
                val currentRecord = record ?: break
                val count = try {
                    currentRecord.read(shorts, filled, shorts.size - filled)
                } catch (_: IllegalStateException) {
                    break
                } catch (_: SecurityException) {
                    break
                }
                if (!isRunning) break
                if (count < 0) {
                    onPitchDetected(null)
                    if (count != AudioRecord.ERROR_DEAD_OBJECT || recoveries >= 2) break
                    // This thread owns release, including any replacement opened during stop().
                    audioRecord = null
                    record = null
                    releaseRecorder(currentRecord)
                    if (!isRunning) break
                    val replacement = openRecorder().record ?: break
                    record = replacement
                    audioRecord = replacement
                    if (!isRunning) break
                    analyzer = GuitarSignalAnalyzer(rmsThreshold, referencePitch, preset)
                    filled = 0
                    recoveries++
                    continue
                }
                if (count == 0) continue
                filled += count
                if (filled < shorts.size) continue
                filled = 0
                recoveries = 0
                for (i in shorts.indices) samples[i] = shorts[i] / 32768f
                onPitchDetected(analyzer.analyze(samples))
            }
        } catch (_: RuntimeException) {
            // Driver/DSP failures are reported as interruptions after releasing the recorder.
        } finally {
            val interrupted = isRunning
            isRunning = false
            audioRecord = null
            record?.let(::releaseRecorder)
            if (interrupted) {
                onPitchDetected(null)
                onStateChanged(MicrophoneState.CAPTURE_INTERRUPTED)
            }
        }
    }

    private fun releaseRecorder(record: AudioRecord) {
        try {
            record.stop()
        } catch (_: IllegalStateException) {
        } catch (_: SecurityException) {
        } finally {
            record.release()
        }
    }

    @WorkerThread
    override fun stop() {
        isRunning = false
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        } catch (_: SecurityException) {
        }
        val worker = recordingThread
        var interrupted = false
        if (worker != null && worker !== Thread.currentThread()) {
            // Wait on the control worker, so the next capture cannot overlap old cleanup.
            while (worker.isAlive) {
                try {
                    worker.join()
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        }
        recordingThread = null
        if (interrupted) Thread.currentThread().interrupt()
    }
}
