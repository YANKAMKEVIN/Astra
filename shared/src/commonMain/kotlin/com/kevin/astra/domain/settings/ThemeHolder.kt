package com.kevin.astra.domain.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which palette the app uses. [System] follows the phone's dark/light setting. */
enum class ThemeMode(val label: String) {
    System("System"),
    Dark("Dark"),
    Light("Light"),
    ;

    /** Resolves the mode against the system setting: true when the light palette applies. */
    fun isLight(systemInDarkTheme: Boolean): Boolean = when (this) {
        System -> !systemInDarkTheme
        Dark -> false
        Light -> true
    }
}

object ThemeHolder {
    private val _mode = MutableStateFlow(ThemeMode.Dark)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) { _mode.value = mode }
    fun mode(): ThemeMode = _mode.value
}
