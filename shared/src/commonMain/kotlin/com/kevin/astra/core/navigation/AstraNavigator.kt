package com.kevin.astra.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AstraNavigator(
    initialDestination: AstraDestination = AstraDestination.Splash,
) {
    private val _currentDestination = MutableStateFlow(initialDestination)
    val currentDestination: StateFlow<AstraDestination> = _currentDestination.asStateFlow()

    fun navigateTo(destination: AstraDestination) {
        _currentDestination.value = destination
    }
}
