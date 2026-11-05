package com.uvg.cc3087.eztunez

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

enum class TuningMode { AUTO, MANUAL }

data class GuitarString(val id: Int, val note: String, val frequency: Float)

// Equal temperament, A4 = 440 Hz. Ordered from the thickest to the thinnest string.
val standardGuitarStrings = BuiltInTuningPresets.standard.guitarStrings(DEFAULT_REFERENCE_PITCH)

fun guitarStringsForReference(referencePitch: Int): List<GuitarString> {
    return BuiltInTuningPresets.standard.guitarStrings(referencePitch)
}

data class DetectedPitch(
    val frequency: Float,
    val note: String,
    val cents: Float,
    val probability: Float
)

internal fun centsFrom(frequency: Float, reference: Float): Float =
    (1200.0 * ln(frequency.toDouble() / reference) / ln(2.0)).toFloat()

internal fun convertFrequency(
    frequency: Float,
    probability: Float,
    referencePitch: Int = DEFAULT_REFERENCE_PITCH,
    preferFlats: Boolean = false
): DetectedPitch {
    val reference = normalizeReferencePitch(referencePitch).toDouble()
    val exactMidi = 69.0 + 12.0 * ln(frequency / reference) / ln(2.0)
    val nearestMidi = exactMidi.roundToInt()
    return DetectedPitch(
        frequency,
        noteNameForMidi(nearestMidi, preferFlats),
        ((exactMidi - nearestMidi) * 100.0).toFloat(),
        probability
    )
}

data class TuningState(
    val selectedString: GuitarString = standardGuitarStrings.first(),
    val pitch: DetectedPitch? = null,
    val cents: Float = 0f
) {
    val isInTune: Boolean get() = pitch != null && abs(cents) <= 5f
}

/** Filters Tarsos readings only; Tarsos YIN remains the sole pitch detector. */
class TuningTracker(
    private val mode: TuningMode,
    referencePitch: Int = DEFAULT_REFERENCE_PITCH,
    private val preset: TuningPreset = BuiltInTuningPresets.standard
) {
    private val referencePitch = normalizeReferencePitch(referencePitch)
    private val guitarStrings = preset.guitarStrings(this.referencePitch)

    var state = TuningState(selectedString = guitarStrings.first())
        private set

    private val recentPitches = ArrayDeque<Float>()
    private var smoothedPitch: Float? = null
    private var lastAcceptedAt: Long? = null
    private var pendingPitch: Float? = null
    private var pendingCount = 0
    private var lastPendingAt: Long? = null
    private var candidateStringId: Int? = null
    private var candidateCount = 0

    fun selectString(id: Int): TuningState {
        if (mode != TuningMode.MANUAL) return state
        val target = guitarStrings.first { it.id == id }
        clearTracking()
        state = TuningState(selectedString = target)
        return state
    }

    /** A stopped capture has no valid measurement, but its tuning target is still selected. */
    fun clearMeasurement(): TuningState {
        clearTracking()
        state = TuningState(selectedString = state.selectedString)
        return state
    }

    fun update(pitch: DetectedPitch?, nowMs: Long): TuningState {
        // A brief invalid frame must not repeatedly erase a quiet note's acquisition.
        if (lastPendingAt?.let { nowMs - it > 230L } == true) {
            pendingPitch = null
            pendingCount = 0
            lastPendingAt = null
        }
        // Do not let old samples bias a new pluck after silence.
        if (lastAcceptedAt?.let { nowMs - it >= 550L } == true) {
            clearTracking()
            state = state.copy(pitch = null)
        }

        if (pitch == null || !pitch.frequency.isFinite() ||
            pitch.frequency !in 60f..400f || !pitch.probability.isFinite() ||
            pitch.probability < GuitarSignalAnalyzer.MIN_PROBABILITY
        ) {
            candidateStringId = null
            candidateCount = 0
            return state // Hold the needle during brief gaps, never snap to zero.
        }

        // Filter in cents, so smoothing has the same response on all six strings.
        val raw = centsFrom(pitch.frequency, referencePitch.toFloat())
        val previous = smoothedPitch
        var confirmedJump = false
        if (previous == null || abs(raw - previous) > 80f) {
            val pending = pendingPitch
            if (pending != null && abs(raw - pending) <= 40f) {
                pendingCount++
            } else {
                pendingCount = 1
            }
            pendingPitch = raw
            lastPendingAt = nowMs
            // Confirm a new note; a single attack/harmonic must not move the needle.
            if (pendingCount < 3) return state
            recentPitches.clear()
            smoothedPitch = raw
            confirmedJump = true
        }
        pendingPitch = null
        pendingCount = 0
        lastPendingAt = null

        recentPitches.addLast(raw)
        if (recentPitches.size > 3) recentPitches.removeFirst()
        val ordered = recentPitches.sorted()
        val median = ordered[ordered.size / 2]
        val smooth = smoothedPitch?.let { it + 0.35f * (median - it) } ?: median
        smoothedPitch = smooth
        lastAcceptedAt = nowMs
        val frequency = (referencePitch * 2.0.pow(smooth / 1200.0)).toFloat()

        var target = state.selectedString
        if (mode == TuningMode.AUTO) {
            val nearest = guitarStrings.minByOrNull {
                abs(centsFrom(frequency, it.frequency))
            }!!
            val improvement = abs(centsFrom(frequency, target.frequency)) -
                abs(centsFrom(frequency, nearest.frequency))
            // Hysteresis prevents alternating strings around their midpoint.
            if (nearest.id != target.id && improvement > 35f) {
                candidateCount = if (candidateStringId == nearest.id) candidateCount + 1 else 1
                candidateStringId = nearest.id
                if (confirmedJump || candidateCount >= 3) {
                    target = nearest
                    candidateStringId = null
                    candidateCount = 0
                }
            } else {
                candidateStringId = null
                candidateCount = 0
            }
        }

        state = TuningState(
            selectedString = target,
            pitch = convertFrequency(frequency, pitch.probability, referencePitch, preset.preferFlats),
            // Always compare to the target string, not to the nearest chromatic note.
            cents = centsFrom(frequency, target.frequency)
        )
        return state
    }

    private fun clearTracking() {
        recentPitches.clear()
        smoothedPitch = null
        lastAcceptedAt = null
        pendingPitch = null
        pendingCount = 0
        lastPendingAt = null
        candidateStringId = null
        candidateCount = 0
    }
}
