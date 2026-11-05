package com.uvg.cc3087.eztunez

import kotlin.math.roundToInt
import java.util.Locale

enum class NotationSystem { STANDARD, SOLFEGE }

data class TunerSettings(
    val micSensitivity: Float = DEFAULT_MIC_SENSITIVITY,
    val referencePitch: Int = DEFAULT_REFERENCE_PITCH,
    val showRealTimeHz: Boolean = true,
    val notationSystem: NotationSystem = NotationSystem.STANDARD,
    val darkTheme: Boolean = true
)

const val DEFAULT_MIC_SENSITIVITY = 0.78f
const val DEFAULT_REFERENCE_PITCH = 440
const val MIN_REFERENCE_PITCH = 400
const val MAX_REFERENCE_PITCH = 480
const val DEFAULT_RMS_THRESHOLD = 0.00025f

fun normalizeSensitivity(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else DEFAULT_MIC_SENSITIVITY

fun normalizeReferencePitch(value: Int): Int =
    value.coerceIn(MIN_REFERENCE_PITCH, MAX_REFERENCE_PITCH)

/** Higher sensitivity accepts quieter audio; the current 78% default retains the old RMS gate. */
fun sensitivityToRmsThreshold(value: Float): Float {
    val sensitivity = normalizeSensitivity(value)
    return if (sensitivity <= DEFAULT_MIC_SENSITIVITY) {
        val progress = sensitivity / DEFAULT_MIC_SENSITIVITY
        0.002f + progress * (DEFAULT_RMS_THRESHOLD - 0.002f)
    } else {
        val progress = (sensitivity - DEFAULT_MIC_SENSITIVITY) /
            (1f - DEFAULT_MIC_SENSITIVITY)
        DEFAULT_RMS_THRESHOLD + progress * (0.0001f - DEFAULT_RMS_THRESHOLD)
    }
}

fun formatNote(note: String, notationSystem: NotationSystem): String {
    if (notationSystem == NotationSystem.STANDARD || note.isEmpty()) return note
    val names = mapOf(
        "C" to "Do", "D" to "Re", "E" to "Mi", "F" to "Fa",
        "G" to "Sol", "A" to "La", "B" to "Si"
    )
    val letter = note.first().toString()
    return names.getValue(letter) + note.drop(1)
}

fun formatPitchDisplay(
    pitch: DetectedPitch?,
    showRealTimeHz: Boolean,
    notationSystem: NotationSystem,
    frequencyPattern: String,
    noSignal: String,
    noSignalHz: String,
    locale: Locale
): String {
    if (pitch == null) return if (showRealTimeHz) noSignalHz else noSignal
    val note = formatNote(pitch.note, notationSystem)
    return if (showRealTimeHz) String.format(locale, frequencyPattern, note, pitch.frequency) else note
}

internal fun TunerSettings.normalized(): TunerSettings = copy(
    micSensitivity = normalizeSensitivity(micSensitivity),
    referencePitch = normalizeReferencePitch(referencePitch)
)

internal fun Float.asStoredPercent(): Int = (normalizeSensitivity(this) * 100f).roundToInt()
