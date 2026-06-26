package com.kevin.astra.core.ai

actual fun createInferenceEngine(): InferenceEngine =
    LiteRtInferenceEngine(
        modelLoader = UnavailableLocalModelLoader("LiteRT is Android-only in Sprint 3; iOS uses Mock fallback until Core ML integration."),
        runtimeSession = UnavailableEdgeRuntimeSession("LiteRT runtime is not supported on iOS."),
        fallbackEngine = MockInferenceEngine(),
    )

