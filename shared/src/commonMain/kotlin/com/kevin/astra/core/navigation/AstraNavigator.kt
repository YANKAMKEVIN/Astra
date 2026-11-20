package com.kevin.astra.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps a small back stack of destinations.
 *
 * - The dock tabs are roots: selecting Home leaves only Home, selecting another tab leaves
 *   Home underneath it, so Back from a tab returns Home before leaving the app.
 * - Leaving Splash or Onboarding clears them: Back never returns to the launch flow.
 * - Any other destination (Voice, Vision, History, Demo, Models) is pushed on top of the
 *   screen it was opened from, and Back returns to that screen.
 */
class AstraNavigator(
    initialDestination: AstraDestination = AstraDestination.Splash,
) {
    private val _backStack = MutableStateFlow(listOf(initialDestination))
    val backStack: StateFlow<List<AstraDestination>> = _backStack.asStateFlow()

    private val _currentDestination = MutableStateFlow(initialDestination)
    val currentDestination: StateFlow<AstraDestination> = _currentDestination.asStateFlow()

    val canGoBack: Boolean
        get() = _backStack.value.size > 1

    fun navigateTo(destination: AstraDestination) {
        val stack = _backStack.value
        val current = stack.last()
        if (destination == current) return
        val next = when {
            destination.isPrimaryNav || current.isLaunchFlow -> rootStackFor(destination)
            else -> stack.filterNot { it == destination } + destination
        }
        update(next)
    }

    /** Returns to the previous destination. Returns false when there is nothing to go back to. */
    fun goBack(): Boolean {
        val stack = _backStack.value
        if (stack.size <= 1) return false
        update(stack.dropLast(1))
        return true
    }

    private fun rootStackFor(destination: AstraDestination): List<AstraDestination> = when {
        destination == AstraDestination.ProjectOverview || destination.isLaunchFlow -> listOf(destination)
        destination.isPrimaryNav -> listOf(AstraDestination.ProjectOverview, destination)
        // Secondary screen opened straight from the launch flow (e.g. a notification).
        else -> listOf(AstraDestination.ProjectOverview, destination)
    }

    private fun update(stack: List<AstraDestination>) {
        _backStack.value = stack
        _currentDestination.value = stack.last()
    }

    private val AstraDestination.isLaunchFlow: Boolean
        get() = this == AstraDestination.Splash || this == AstraDestination.Onboarding
}
