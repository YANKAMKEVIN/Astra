package com.kevin.astra.core.ai

import com.kevin.astra.domain.assistant.StreamEvent
import kotlinx.coroutines.flow.Flow

class RoutingInferenceEngine(
    private val mockEngine: InferenceEngine,
    private val liteRtEngine: InferenceEngine,
    private val liteRtLmEngine: InferenceEngine,
    private val isDemoMode: () -> Boolean = { com.kevin.astra.domain.settings.DemoModeHolder.isEnabled() },
) : InferenceEngine {
    override suspend fun generate(request: PromptRequest): GenerationResult =
        engineFor(request).generate(request)

    override fun generateStream(request: PromptRequest): Flow<StreamEvent> =
        engineFor(request).generateStream(request)

    private fun engineFor(request: PromptRequest): InferenceEngine {
        if (isDemoMode()) return mockEngine
        return when (request.backend) {
            InferenceBackend.Mock -> mockEngine
            InferenceBackend.LiteRt -> liteRtEngine
            InferenceBackend.LiteRtLm -> liteRtLmEngine
            InferenceBackend.OnnxRuntime,
            InferenceBackend.CoreMl,
            InferenceBackend.LlamaCpp,
            -> mockEngine
        }
    }
}

