package com.kevin.astra.presentation.dashboard

import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState

data class DashboardState(
    val capabilities: DeviceCapabilities? = null,
    val isLoadingCapabilities: Boolean = true,
    val error: String? = null,
) : AstraState

sealed interface DashboardIntent : AstraIntent {
    data object RefreshCapabilities : DashboardIntent
}

sealed interface DashboardEffect : AstraEffect
