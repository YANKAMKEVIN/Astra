package com.kevin.astra.presentation.dashboard

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.mvi.AstraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val dashboardScope: CoroutineScope? = null,
) : AstraViewModel<DashboardState, DashboardIntent, DashboardEffect>(
    initialState = DashboardState(),
) {
    init {
        loadCapabilities()
    }

    override fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.RefreshCapabilities -> loadCapabilities()
        }
    }

    private fun loadCapabilities() {
        (dashboardScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isLoadingCapabilities = true,
                    error = null,
                )
            }

            runCatching { deviceCapabilityProvider.getCapabilities() }
                .onSuccess { capabilities ->
                    updateState {
                        copy(
                            capabilities = capabilities,
                            isLoadingCapabilities = false,
                            error = null,
                        )
                    }
                }
                .onFailure {
                    updateState {
                        copy(
                            isLoadingCapabilities = false,
                            error = "Unable to read device capabilities.",
                        )
                    }
                }
        }
    }
}
