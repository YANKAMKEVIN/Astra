package com.kevin.astra.presentation.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class InferenceParameterScaleTest {
    private val scale = InferenceParameterScale

    @Test
    fun temperatureSnapsToTenthsWithinTheRuntimeRange() {
        assertEquals(0.3, scale.snapTemperature(0.27f))
        assertEquals(0.0, scale.snapTemperature(-0.2f))
        assertEquals(1.0, scale.snapTemperature(1.4f))
    }

    @Test
    fun temperatureBandsAreNamed() {
        assertEquals("Precise", scale.temperatureLabel(0.2))
        assertEquals("Balanced", scale.temperatureLabel(0.5))
        assertEquals("Creative", scale.temperatureLabel(0.8))
        scale.TemperaturePresets.forEach { (name, value) ->
            assertEquals(name, scale.temperatureLabel(value))
        }
    }

    @Test
    fun tokenValuesSnapToTheirStep() {
        assertEquals(512, scale.snap(530f, scale.MaxTokensMin, scale.MaxTokensMax, scale.MaxTokensStep))
        assertEquals(2_048, scale.snap(5_000f, scale.MaxTokensMin, scale.MaxTokensMax, scale.MaxTokensStep))
        assertEquals(4_096, scale.snap(4_500f, scale.ContextWindowMin, scale.ContextWindowMax, scale.ContextWindowStep))
    }

    @Test
    fun sliderStepsLandOnEveryAllowedValue() {
        // 128..2048 by 128 = 16 positions = 14 intermediate stops.
        assertEquals(14, scale.sliderSteps(scale.MaxTokensMin, scale.MaxTokensMax, scale.MaxTokensStep))
        assertEquals(14, scale.sliderSteps(scale.ContextWindowMin, scale.ContextWindowMax, scale.ContextWindowStep))
    }
}
