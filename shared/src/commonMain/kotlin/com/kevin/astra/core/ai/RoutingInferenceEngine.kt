package com.kevin.astra.core.ai

class RoutingInferenceEngine(
    private val mockEngine: InferenceEngine,
    private val liteRtEngine: InferenceEngine,
    private val liteRtLmEngine: InferenceEngine,
) : InferenceEngine {
    override suspend fun generate(request: PromptRequest): GenerationResult =
        when (request.backend) {
            InferenceBackend.Mock -> mockEngine.generate(request)
            InferenceBackend.LiteRt -> liteRtEngine.generate(request)
            InferenceBackend.LiteRtLm -> liteRtLmEngine.generate(request)
            InferenceBackend.OnnxRuntime,
            InferenceBackend.CoreMl,
            InferenceBackend.LlamaCpp,
            -> mockEngine.generate(request.copy(backend = InferenceBackend.Mock)).copy(
                runtimeInfo = GenerationRuntimeInfo(
                    mode = RuntimeMode.UnsupportedPlatform,
                    fallbackReason = "${request.backend.label} is not implemented in this ASTRA build.",
                ),
            )
        }
}

