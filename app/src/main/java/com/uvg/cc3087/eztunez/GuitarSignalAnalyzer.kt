package com.uvg.cc3087.eztunez

import be.tarsos.dsp.pitch.Yin
import be.tarsos.dsp.pitch.PitchDetector as TarsosPitchDetector
import kotlin.math.abs
import kotlin.math.sqrt

/** Standard or FFT-accelerated YIN on the same consecutive microphone samples. */
internal class GuitarSignalAnalyzer(
    private val rmsThreshold: Float = DEFAULT_RMS_THRESHOLD,
    private val referencePitch: Int = DEFAULT_REFERENCE_PITCH,
    private val preset: TuningPreset = BuiltInTuningPresets.standard,
    private val algorithm: DetectionAlgorithm = DetectionAlgorithm.YIN,
    private val mode: TuningMode = TuningMode.AUTO
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val WINDOW_SIZE = 4096
        const val HOP_SIZE = WINDOW_SIZE / 2
        const val MIN_PROBABILITY = 0.75f
    }

    private val window = FloatArray(WINDOW_SIZE)
    private val targetStrings = preset.guitarStrings(referencePitch)
    private val acceptedFrequencies = if (mode == TuningMode.CHROMATIC)
        ChromaticScale.frequencyRange else preset.captureFrequencyRange(referencePitch)
    private var hasPreviousBlock = false
    // Allocate only the chosen backend, once per capture. Both use the established
    // thresholds, confidence checks, and sub-sample period interpolation.
    private val standardDetector = detector()
    private val preciseDetector = detector(0.10)
    private val tolerantDetector = detector(0.25)

    private fun detector(threshold: Double? = null): TarsosPitchDetector = when (algorithm) {
        DetectionAlgorithm.YIN -> if (threshold == null) Yin(SAMPLE_RATE.toFloat(), WINDOW_SIZE)
            else Yin(SAMPLE_RATE.toFloat(), WINDOW_SIZE, threshold)
        DetectionAlgorithm.FFT -> FftYinDetector(SAMPLE_RATE.toFloat(), WINDOW_SIZE, threshold ?: 0.20)
    }

    fun analyze(block: FloatArray): DetectedPitch? {
        require(block.size == HOP_SIZE)
        // Keep the input untouched. Removing a different mean from every short block
        // creates discontinuities, especially with the longer periods of E2 and A2.
        window.copyInto(window, 0, HOP_SIZE, WINDOW_SIZE)
        block.copyInto(window, HOP_SIZE)
        if (!hasPreviousBlock) {
            hasPreviousBlock = true
            return null
        }
        val mean = window.average()
        var energy = 0.0
        for (sample in window) {
            val centered = sample - mean
            energy += centered * centered
        }
        if (sqrt(energy / WINDOW_SIZE) < rmsThreshold.coerceAtLeast(0f)) return null

        val primary = detect(standardDetector)
        if (primary != null) {
            // Accept normal readings immediately. Only investigate a possible harmonic
            // when the detected pitch is far from every open-string reference.
            if (mode != TuningMode.CHROMATIC && distanceToOpenString(primary.frequency) > 100f) {
                val precise = detect(preciseDetector)
                if (precise != null &&
                    abs(centsFrom(primary.frequency, precise.frequency) - 1200f) < 35f &&
                    distanceToOpenString(precise.frequency) + 80f <
                    distanceToOpenString(primary.frequency)
                ) return precise
            }
            return primary
        }
        // A pitched result outside the guitar range must not block a usable fallback.
        return detect(preciseDetector) ?: detect(tolerantDetector)
    }

    private fun detect(detector: TarsosPitchDetector): DetectedPitch? {
        val result = detector.getPitch(window)
        val frequency = result.pitch
        if (!result.isPitched || !frequency.isFinite() || frequency !in acceptedFrequencies ||
            !result.probability.isFinite() || result.probability < MIN_PROBABILITY
        ) return null
        return convertFrequency(frequency, result.probability, referencePitch, mode != TuningMode.CHROMATIC && preset.preferFlats)
    }

    private fun distanceToOpenString(frequency: Float): Float =
        targetStrings.minOf {
            abs(centsFrom(frequency, it.frequency))
        }
}
