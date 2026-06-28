package com.kevin.astra.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(
            listOf(
                AstraDestination.Dashboard,
                AstraDestination.Demo,
                AstraDestination.ProjectOverview,
                AstraDestination.Assistant,
                AstraDestination.Documents,
                AstraDestination.Benchmark,
                AstraDestination.Settings,
            ),
            AstraDestination.primaryDestinations,
        )
    }
}
