package com.kevin.astra.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AstraNavigatorTest {
    @Test
    fun startsOnSplash() {
        val navigator = AstraNavigator()

        assertEquals(AstraDestination.Splash, navigator.currentDestination.value)
    }

    @Test
    fun navigatesToEveryPrimaryDestination() {
        val navigator = AstraNavigator()

        AstraDestination.primaryDestinations.forEach { destination ->
            navigator.navigateTo(destination)
            assertEquals(destination, navigator.currentDestination.value)
        }
    }

    @Test
    fun exposesPrimaryDestinations() {
        val destinations = AstraDestination.primaryDestinations
        assertTrue(destinations.contains(AstraDestination.ProjectOverview))
        assertTrue(destinations.contains(AstraDestination.Assistant))
        assertTrue(destinations.contains(AstraDestination.Benchmark))
        assertTrue(destinations.contains(AstraDestination.Settings))
        assertTrue(destinations.none { !it.showsNavigationBar })
    }
}
