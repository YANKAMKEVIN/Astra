package com.kevin.astra.core.ai

actual fun createInferenceEngine(): InferenceEngine =
    MockInferenceEngine().let { mockEngine ->
        RoutingInferenceEngine(
            mockEngine = mockEngine,
            liteRtEngine = LiteRtInferenceEngine(
                modelLoader = UnavailableLocalModelLoader("LiteRT is Android-only in Sprint 3; iOS uses Mock fallback until Core ML integration."),
                runtimeSession = UnavailableEdgeRuntimeSession("LiteRT runtime is not supported on iOS."),
                fallbackEngine = mockEngine,
            ),
            liteRtLmEngine = LiteRtLmInferenceEngine(
                modelLoader = UnsupportedLiteRtLmModelLoader("LiteRT-LM is Android-only in this Sprint 3 evaluation."),
                fallbackEngine = mockEngine,
            ),
        )
    }
