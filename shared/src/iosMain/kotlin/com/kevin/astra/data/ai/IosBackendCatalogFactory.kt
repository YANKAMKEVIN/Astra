package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendStatus

actual fun createBackendCatalog(): BackendCatalog =
    DefaultBackendCatalog(
        statusOverrides = mapOf(
            "litert" to BackendStatus.Unsupported,
            "onnx-runtime" to BackendStatus.Unsupported,
            "core-ml" to BackendStatus.Available,
        ),
    )

