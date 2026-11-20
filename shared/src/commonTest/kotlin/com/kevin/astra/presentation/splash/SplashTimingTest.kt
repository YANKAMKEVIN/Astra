package com.kevin.astra.presentation.splash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplashTimingTest {
    @Test
    fun firstLaunchKeepsTheFullAnimation() {
        assertEquals(2_400L, SplashTiming.durationMillis(isFirstLaunch = true))
    }

    @Test
    fun laterLaunchesAreShort() {
        val returning = SplashTiming.durationMillis(isFirstLaunch = false)
        assertEquals(800L, returning)
        assertTrue(returning < SplashTiming.durationMillis(isFirstLaunch = true))
    }
}
