package com.uvg.cc3087.eztunez

import be.tarsos.dsp.pitch.Yin
import kotlin.math.abs
import kotlin.math.sqrt

/** Tarsos YIN on consecutive microphone samples, without a mandatory audio filter. */
internal class GuitarSignalAnalyzer {
    companion object {
        const val SAMPLE_RATE = 44100
        const val WINDOW_SIZE = 4096
        const val HOP_SIZE = WINDOW_SIZE / 2
        const val MIN_PROBABILITY = 0.75f
        private const val MIN_RMS = 0.00025
    }

    private val window = FloatArray(WINDOW_SIZE)
    private var hasPreviousBlock = false
    // Start with the same YIN configuration as the version that detected bass strings.
    private val standardYin = Yin(SAMPLE_RATE.toFloat(), WINDOW_SIZE)
    private val preciseYin = Yin(SAMPLE_RATE.toFloat(), WINDOW_SIZE, 0.10)
    private val tolerantYin = Yin(SAMPLE_RATE.toFloat(), WINDOW_SIZE, 0.25)

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
        if (sqrt(energy / WINDOW_SIZE) < MIN_RMS) return null

        val primary = detect(standardYin)
        if (primary != null) {
            // Accept normal readings immediately. Only investigate a possible harmonic
            // when the detected pitch is far from every open-string reference.
            if (distanceToOpenString(primary.frequency) > 100f) {
                val precise = detect(preciseYin)
                if (precise != null &&
                    abs(centsFrom(primary.frequency, precise.frequency) - 1200f) < 35f &&
                    distanceToOpenString(precise.frequency) + 80f <
                    distanceToOpenString(primary.frequency)
                ) return precise
            }
            return primary
        }
        // A pitched result outside the guitar range must not block a usable fallback.
        return detect(preciseYin) ?: detect(tolerantYin)
    }

    private fun detect(detector: Yin): DetectedPitch? {
        val result = detector.getPitch(window)
        val frequency = result.pitch
        if (!result.isPitched || !frequency.isFinite() || frequency !in 60f..400f ||
            !result.probability.isFinite() || result.probability < MIN_PROBABILITY
        ) return null
        return convertFrequency(frequency, result.probability)
    }

    private fun distanceToOpenString(frequency: Float): Float =
        standardGuitarStrings.minOf { abs(centsFrom(frequency, it.frequency)) }
}
