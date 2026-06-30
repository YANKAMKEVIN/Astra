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
        mockAwareEngine(request).generate(mockAwareRequest(request))

    override fun generateStream(request: PromptRequest): Flow<StreamEvent> =
        mockAwareEngine(request).generateStream(mockAwareRequest(request))

    private fun mockAwareEngine(request: PromptRequest): InferenceEngine =
        if (isDemoMode()) mockEngine else engineFor(request)

    private fun mockAwareRequest(request: PromptRequest): PromptRequest =
        if (isDemoMode() && request.backend != InferenceBackend.Mock) {
            request.copy(backend = InferenceBackend.Mock)
        } else request

    private fun engineFor(request: PromptRequest): InferenceEngine {
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

