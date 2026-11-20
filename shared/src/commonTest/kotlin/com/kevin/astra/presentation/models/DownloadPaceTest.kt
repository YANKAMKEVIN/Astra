package com.kevin.astra.presentation.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DownloadPaceTest {
    @Test
    fun measuresUntilThereIsASecondOfSamples() {
        val estimator = DownloadRateEstimator()
        estimator.record(0, 100f)
        estimator.record(500, 106f)

        assertEquals(DownloadPace.Measuring, estimator.pace(500, 2_950f))
    }

    @Test
    fun givesRateAndTimeLeft() {
        val estimator = DownloadRateEstimator()
        estimator.record(0, 800f)
        estimator.record(1_000, 812f)
        estimator.record(2_000, 824f)

        val pace = assertIs<DownloadPace.Active>(estimator.pace(2_000, 3_000f))
        assertEquals(12f, pace.mbPerSecond)
        assertEquals(181L, pace.secondsLeft) // (3000 - 824) / 12
        assertEquals("12 MB/s · about 3 min left", formatDownloadPace(pace))
    }

    @Test
    fun noProgressForAWhileReadsAsStalled() {
        val estimator = DownloadRateEstimator()
        estimator.record(0, 100f)
        estimator.record(1_000, 110f)
        estimator.record(4_000, 110f)

        assertEquals(DownloadPace.Stalled, estimator.pace(6_500, 2_950f))
        assertEquals("Paused — waiting for network", formatDownloadPace(DownloadPace.Stalled))
    }

    @Test
    fun formatsSlowRatesAndLongDurations() {
        assertEquals("450 KB/s", formatRate(0.45f))
        assertEquals("2.5 MB/s", formatRate(2.5f))
        assertEquals("less than a minute left", formatTimeLeft(42))
        assertEquals("about 1 h 5 min left", formatTimeLeft(3_900))
        assertEquals("about 2 h left", formatTimeLeft(7_200))
        assertNull(formatDownloadPace(DownloadPace.Measuring))
    }
}
