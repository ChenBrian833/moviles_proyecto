package com.uvg.cc3087.eztunez

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import be.tarsos.dsp.pitch.PitchDetector
import be.tarsos.dsp.pitch.Yin
import kotlin.concurrent.thread
import kotlin.math.ln
import kotlin.math.roundToInt

data class DetectedPitch(
    val frequency: Float,
    val note: String,
    val cents: Float,
    val probability: Float
)

class GuitarPitchDetector(
    private val context: Context,
    private val onPitchDetected: (DetectedPitch?) -> Unit
) {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var isRunning = false

    /*
     * TarsosDSP YIN detector.
     */
    private val pitchDetector: PitchDetector =
        Yin(
            SAMPLE_RATE.toFloat(),
            BUFFER_SIZE
        )

    fun start() {

        if (isRunning) {
            return
        }

        /*
         * Never access the microphone unless
         * Android has granted permission.
         */
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val minimumBufferSize =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

        if (minimumBufferSize <= 0) {
            return
        }

        /*
         * AudioRecord works with bytes while our
         * processing buffer contains 16-bit samples.
         */
        val audioBufferSize =
            maxOf(
                minimumBufferSize,
                BUFFER_SIZE * 2
            )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSize
        )

        if (
            audioRecord?.state !=
            AudioRecord.STATE_INITIALIZED
        ) {
            audioRecord?.release()
            audioRecord = null
            return
        }

        isRunning = true

        audioRecord?.startRecording()

        recordingThread = thread(
            start = true,
            name = "EzTunez-Microphone"
        ) {

            readMicrophone()
        }
    }

    private fun readMicrophone() {

        val shortBuffer =
            ShortArray(BUFFER_SIZE)

        val floatBuffer =
            FloatArray(BUFFER_SIZE)

        while (isRunning) {

            val record =
                audioRecord ?: break

            val samplesRead =
                record.read(
                    shortBuffer,
                    0,
                    BUFFER_SIZE
                )

            if (samplesRead <= 0) {
                continue
            }

            /*
             * Ignore partial buffers for now.
             *
             * YIN expects the buffer size it was
             * configured with.
             */
            if (samplesRead < BUFFER_SIZE) {
                continue
            }

            /*
             * Convert Android PCM 16-bit samples:
             *
             * -32768 ... 32767
             *
             * into floats approximately:
             *
             * -1.0 ... 1.0
             */
            for (i in 0 until BUFFER_SIZE) {

                floatBuffer[i] =
                    shortBuffer[i] / 32768f
            }

            analyzeBuffer(floatBuffer)
        }
    }

    private fun analyzeBuffer(
        buffer: FloatArray
    ) {

        val result =
            pitchDetector.getPitch(buffer)

        val frequency =
            result.pitch

        /*
         * -1 means no usable pitch was found.
         *
         * We also reject weak detections.
         */
        if (
            !result.isPitched ||
            frequency <= 0f ||
            result.probability < 0.80f
        ) {

            onPitchDetected(null)
            return
        }

        /*
         * Guitar range with some margin.
         *
         * Standard guitar is roughly
         * E2 (82 Hz) through E4 (330 Hz).
         */
        if (
            frequency < 60f ||
            frequency > 400f
        ) {
            return
        }

        onPitchDetected(
            convertFrequency(
                frequency = frequency,
                probability = result.probability
            )
        )
    }

    fun stop() {

        isRunning = false

        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }

        audioRecord?.release()
        audioRecord = null

        recordingThread = null
    }
}


/*
 * Convert frequency into:
 *
 * 82.41 Hz
 *      ↓
 * E2
 *      ↓
 * cents away from E2
 */
private fun convertFrequency(
    frequency: Float,
    probability: Float
): DetectedPitch {

    val exactMidi =
        69.0 +
                12.0 *
                (
                        ln(frequency / 440.0) /
                                ln(2.0)
                        )

    val nearestMidi =
        exactMidi.roundToInt()

    val cents =
        (
                (exactMidi - nearestMidi) *
                        100.0
                ).toFloat()

    val noteNames = arrayOf(
        "C",
        "C#",
        "D",
        "D#",
        "E",
        "F",
        "F#",
        "G",
        "G#",
        "A",
        "A#",
        "B"
    )

    val noteIndex =
        ((nearestMidi % 12) + 12) % 12

    val octave =
        nearestMidi / 12 - 1

    return DetectedPitch(
        frequency = frequency,
        note = "${noteNames[noteIndex]}$octave",
        cents = cents,
        probability = probability
    )
}