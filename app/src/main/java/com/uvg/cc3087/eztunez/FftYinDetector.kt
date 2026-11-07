package com.uvg.cc3087.eztunez

import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchDetector
import be.tarsos.dsp.util.fft.FloatFFT

/** FFT autocorrelation followed by a YIN-style normalized difference search.
 * Zero padding prevents circular correlation from contaminating the lags.
 * Scratch buffers and the FFT plan are reused on the microphone's reader thread.
 */
internal class FftYinDetector(
    private val sampleRate: Float,
    private val windowSize: Int,
    private val threshold: Double = 0.20
) : PitchDetector {
    private val fftSize = windowSize * 2
    private val fft = FloatFFT(fftSize)
    private val spectrum = FloatArray(fftSize * 2)
    private val energy = DoubleArray(windowSize + 1)
    private val difference = DoubleArray(windowSize / 2)
    private val result = PitchDetectionResult()

    override fun getPitch(audioBuffer: FloatArray): PitchDetectionResult {
        require(audioBuffer.size == windowSize)
        result.setPitch(-1f)
        result.setProbability(0f)
        result.setPitched(false)

        // Center the complete analysis window, never individual input hops.
        // This improves FFT precision with DC offsets without altering the input
        // or introducing discontinuities into the rolling microphone history.
        val mean = audioBuffer.average()
        spectrum.fill(0f)
        energy[0] = 0.0
        for (i in audioBuffer.indices) {
            val sample = (audioBuffer[i] - mean).toFloat()
            spectrum[2 * i] = sample
            energy[i + 1] = energy[i] + sample.toDouble() * sample
        }
        if (!energy[windowSize].isFinite() || energy[windowSize] <= 0) return result

        fft.complexForward(spectrum)
        for (bin in 0 until fftSize) {
            val real = spectrum[2 * bin]
            val imaginary = spectrum[2 * bin + 1]
            spectrum[2 * bin] = real * real + imaginary * imaginary
            spectrum[2 * bin + 1] = 0f
        }
        fft.complexInverse(spectrum, true)

        // d(tau) = sum(x[i]^2) + sum(x[i+tau]^2) - 2 * correlation(tau).
        // Prefix sums use double precision and matching endpoints. Normalize
        // by overlap length before the cumulative mean, since fewer samples
        // contribute at larger lags. Clamp only impossible negative roundoff.
        var runningSum = 0.0
        difference[0] = 1.0
        for (lag in 1 until difference.size) {
            val squaredDifference = (
                energy[windowSize - lag] + energy[windowSize] - energy[lag] -
                    2.0 * spectrum[2 * lag]
                ).coerceAtLeast(0.0) / (windowSize - lag)
            runningSum += squaredDifference
            difference[lag] = if (runningSum > 0) squaredDifference * lag / runningSum else 1.0
        }

        var lag = 2
        while (lag < difference.size) {
            if (difference[lag] < threshold) {
                while (lag + 1 < difference.size && difference[lag + 1] < difference[lag]) lag++
                val period = interpolatedPeriod(lag)
                val frequency = (sampleRate / period).toFloat()
                if (frequency.isFinite() && frequency > 0) {
                    result.setPitch(frequency)
                    result.setProbability((1.0 - difference[lag]).coerceIn(0.0, 1.0).toFloat())
                    result.setPitched(true)
                }
                return result
            }
            lag++
        }
        return result
    }

    private fun interpolatedPeriod(lag: Int): Double {
        if (lag + 1 >= difference.size) return lag.toDouble()
        val left = difference[lag - 1]
        val center = difference[lag]
        val right = difference[lag + 1]
        val curvature = left - 2 * center + right
        val offset = if (curvature > 1e-12) 0.5 * (left - right) / curvature else 0.0
        return lag + offset.coerceIn(-1.0, 1.0)
    }
}
