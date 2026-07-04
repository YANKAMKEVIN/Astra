package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.LiteRtLmBridge

actual fun createBackendCatalog(): BackendCatalog =
    DefaultBackendCatalog(
        statusOverrides = mapOf(
            // Report LiteRT-LM as installed only while the Swift bridge is actually registered
            // (see ContentView.swift / IosLiteRtLmRuntime.kt). If registration ever fails the
            // backend drops to Unsupported instead of falsely claiming a runtime that isn't there.
            "litert-lm" to {
                if (LiteRtLmBridge.engine != null) BackendStatus.Installed else BackendStatus.Unsupported
            },
        ),
    )
