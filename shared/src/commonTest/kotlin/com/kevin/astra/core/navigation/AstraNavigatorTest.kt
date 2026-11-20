package com.kevin.astra.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun backFromASecondaryScreenReturnsToWhereItWasOpened() {
        val navigator = AstraNavigator(AstraDestination.ProjectOverview)
        navigator.navigateTo(AstraDestination.Assistant)
        navigator.navigateTo(AstraDestination.VoiceAssistant)

        assertTrue(navigator.goBack())
        assertEquals(AstraDestination.Assistant, navigator.currentDestination.value)
    }

    @Test
    fun backFromATabReturnsHomeThenStops() {
        val navigator = AstraNavigator(AstraDestination.ProjectOverview)
        navigator.navigateTo(AstraDestination.Settings)
        navigator.navigateTo(AstraDestination.Models)

        assertTrue(navigator.goBack())
        assertEquals(AstraDestination.Settings, navigator.currentDestination.value)
        assertTrue(navigator.goBack())
        assertEquals(AstraDestination.ProjectOverview, navigator.currentDestination.value)
        assertFalse(navigator.canGoBack)
        assertFalse(navigator.goBack())
        assertEquals(AstraDestination.ProjectOverview, navigator.currentDestination.value)
    }

    @Test
    fun switchingTabsDoesNotPileUpHistory() {
        val navigator = AstraNavigator(AstraDestination.ProjectOverview)
        navigator.navigateTo(AstraDestination.Assistant)
        navigator.navigateTo(AstraDestination.History)
        navigator.navigateTo(AstraDestination.Benchmark)
        navigator.navigateTo(AstraDestination.Documents)

        assertEquals(
            listOf(AstraDestination.ProjectOverview, AstraDestination.Documents),
            navigator.backStack.value,
        )
    }

    @Test
    fun launchFlowIsNotKeptInHistory() {
        val navigator = AstraNavigator()
        navigator.navigateTo(AstraDestination.Onboarding)
        navigator.navigateTo(AstraDestination.ProjectOverview)

        assertFalse(navigator.canGoBack)
    }

    @Test
    fun reopeningAScreenAlreadyInTheStackMovesItOnTop() {
        val navigator = AstraNavigator(AstraDestination.ProjectOverview)
        navigator.navigateTo(AstraDestination.History)
        navigator.navigateTo(AstraDestination.VoiceAssistant)
        navigator.navigateTo(AstraDestination.History)

        assertEquals(
            listOf(AstraDestination.ProjectOverview, AstraDestination.VoiceAssistant, AstraDestination.History),
            navigator.backStack.value,
        )
    }
}
