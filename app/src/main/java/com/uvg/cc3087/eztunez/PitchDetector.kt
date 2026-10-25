package com.uvg.cc3087.eztunez

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

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

    override fun start() {
        if (isRunning || ContextCompat.checkSelfPermission(
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
        recordingThread = thread(start = true, name = "EzTunez-Microphone") {
            readMicrophone(record)
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
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
        val shorts = ShortArray(GuitarSignalAnalyzer.HOP_SIZE)
        val samples = FloatArray(GuitarSignalAnalyzer.HOP_SIZE)
        var analyzer = GuitarSignalAnalyzer(rmsThreshold, referencePitch, preset)
        var record = initialRecord
        var filled = 0
        var recoveries = 0
        while (isRunning) {
            val count = try {
                record.read(shorts, filled, shorts.size - filled)
            } catch (_: IllegalStateException) {
                break
            } catch (_: SecurityException) {
                break
            }
            if (!isRunning) break
            if (count < 0) {
                onPitchDetected(null)
                // A dead AudioRecord otherwise loops forever without hearing anything.
                if (count != AudioRecord.ERROR_DEAD_OBJECT || recoveries >= 2) break
                record.release()
                val replacementResult = openRecorder()
                val replacement = replacementResult.record
                if (replacement == null) {
                    break
                }
                // stop() can run while the replacement is being opened.
                audioRecord = replacement
                if (!isRunning) {
                    replacement.release()
                    audioRecord = null
                    return
                }
                record = replacement
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
        if (isRunning) {
            isRunning = false
            onPitchDetected(null)
            onStateChanged(MicrophoneState.CAPTURE_INTERRUPTED)
        }
    }

    override fun stop() {
        isRunning = false
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }
        try {
            recordingThread?.join(300)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
    }
}
