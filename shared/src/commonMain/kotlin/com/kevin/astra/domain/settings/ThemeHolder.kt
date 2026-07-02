package com.kevin.astra.domain.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeHolder {
    private val _isLight = MutableStateFlow(false)
    val isLight: StateFlow<Boolean> = _isLight.asStateFlow()

    fun set(light: Boolean) { _isLight.value = light }
    fun isLight(): Boolean = _isLight.value
}
