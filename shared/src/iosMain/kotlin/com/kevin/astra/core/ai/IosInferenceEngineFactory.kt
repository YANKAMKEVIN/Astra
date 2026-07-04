package com.kevin.astra.core.ai

actual fun createInferenceEngine(): InferenceEngine =
    MockInferenceEngine().let { mockEngine ->
        RoutingInferenceEngine(
            mockEngine = mockEngine,
            liteRtEngine = LiteRtInferenceEngine(
                modelLoader = UnavailableLocalModelLoader("Plain LiteRT (non-LM) is Android-only; iOS uses Mock fallback until Core ML integration."),
                runtimeSession = UnavailableEdgeRuntimeSession("LiteRT tensor runtime is not supported on iOS."),
                fallbackEngine = mockEngine,
            ),
            liteRtLmEngine = LiteRtLmInferenceEngine(
                modelLoader = IosLiteRtLmModelLoader(),
                runtimeSession = IosLiteRtLmRuntimeSession(),
                fallbackEngine = mockEngine,
            ),
        )
    }
