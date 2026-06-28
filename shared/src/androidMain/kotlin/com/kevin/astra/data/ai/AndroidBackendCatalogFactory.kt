package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendStatus

actual fun createBackendCatalog(): BackendCatalog =
    DefaultBackendCatalog(
        statusOverrides = mapOf(
            "litert" to BackendStatus.Installed,
            "litert-lm" to BackendStatus.Installed,
            "core-ml" to BackendStatus.Unsupported,
        ),
    )
