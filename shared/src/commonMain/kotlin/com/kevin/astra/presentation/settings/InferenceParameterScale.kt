package com.kevin.astra.presentation.settings

import kotlin.math.roundToInt

/**
 * Slider scales for the inference parameters. The ranges are the ones the settings
 * repository keeps (see PersistentAiConfigurationRepository), so a slider never shows a value
 * that would not survive a restart.
 */
internal object InferenceParameterScale {
    const val TemperatureMin = 0.0
    const val TemperatureMax = 1.0
    const val TemperatureStep = 0.1

    const val MaxTokensMin = 128
    const val MaxTokensMax = 2_048
    const val MaxTokensStep = 128

    const val ContextWindowMin = 1_024
    const val ContextWindowMax = 16_384
    const val ContextWindowStep = 1_024

    /** Named temperatures shown under the slider. */
    val TemperaturePresets: List<Pair<String, Double>> = listOf(
        "Precise" to 0.2,
        "Balanced" to 0.5,
        "Creative" to 0.8,
    )

    /** Common response lengths shown under the Max tokens slider. */
    val MaxTokensPresets: List<Int> = listOf(512, 1_024, 2_048)

    /** Name of the temperature band [temperature] falls in. */
    fun temperatureLabel(temperature: Double): String = when {
        temperature < 0.35 -> "Precise"
        temperature < 0.65 -> "Balanced"
        else -> "Creative"
    }

    /** Number of intermediate stops for a Material slider covering [min]..[max] by [step]. */
    fun sliderSteps(min: Int, max: Int, step: Int): Int = ((max - min) / step - 1).coerceAtLeast(0)

    /** Rounds a slider position to the nearest 0.1. */
    fun snapTemperature(raw: Float): Double =
        ((raw * 10).roundToInt() / 10.0).coerceIn(TemperatureMin, TemperatureMax)

    fun snap(raw: Float, min: Int, max: Int, step: Int): Int =
        (min + ((raw - min) / step).roundToInt() * step).coerceIn(min, max)
}
