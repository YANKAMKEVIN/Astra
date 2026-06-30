package com.kevin.astra.domain.benchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Co2EstimatorTest {

    @Test
    fun zeroBatteryDrainProducesZeroCo2() {
        assertEquals(0.0, Co2Estimator.onDeviceMg(0))
    }

    @Test
    fun onePercentDrainApproximates71mg() {
        val mg = Co2Estimator.onDeviceMg(1)
        assertTrue(mg in 70.0..73.0, "Expected ~71 mg but got $mg")
    }

    @Test
    fun fullBatteryDrainApproximates7125mg() {
        val mg = Co2Estimator.onDeviceMg(100)
        assertTrue(mg in 7_000.0..7_300.0, "Expected ~7125 mg but got $mg")
    }

    @Test
    fun cloudEquivalentFor1kTokensIs2000mg() {
        assertEquals(2_000.0, Co2Estimator.cloudEquivalentMg(1_000))
    }

    @Test
    fun displayShowsMgForSmallValues() {
        val label = Co2Estimator.display(500.0)
        assertTrue(label.contains("mg"), "Expected mg label but got: $label")
    }

    @Test
    fun displayShowsGramsForLargeValues() {
        val label = Co2Estimator.display(2_000.0)
        assertTrue(label.contains("g CO"), "Expected grams label but got: $label")
    }

    @Test
    fun savingsPercentIsHighWhenOnDeviceIsLow() {
        val savings = Co2Estimator.savingsPercent(onDeviceMg = 71.0, cloudMg = 2_000.0)
        assertTrue(savings > 90, "Expected >90% savings but got $savings%")
    }

    @Test
    fun savingsPercentIsZeroWhenCloudIsZero() {
        assertEquals(0, Co2Estimator.savingsPercent(onDeviceMg = 100.0, cloudMg = 0.0))
    }

    @Test
    fun savingsPercentClampedAt100() {
        val savings = Co2Estimator.savingsPercent(onDeviceMg = 0.0, cloudMg = 1_000.0)
        assertEquals(100, savings)
    }
}
