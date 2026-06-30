package com.kevin.astra.domain.assistant

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.GenerationMetrics
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.domain.assistant.StreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class AskLocalAssistantUseCaseTest {
    @Test
    fun delegatesPromptRequestToInferenceEngine() = runBlocking {
        var capturedRequest: PromptRequest? = null
        val useCase = AskLocalAssistantUseCase(
            inferenceEngine = object : InferenceEngine {
                override fun generateStream(request: PromptRequest): Flow<StreamEvent> = emptyFlow()
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    capturedRequest = request
                    return GenerationResult(
                        text = "Generated",
                        metrics = GenerationMetrics(
                            latencyMillis = 1_200,
                            timeToFirstTokenMillis = 300,
                            tokensGenerated = 42,
                            tokensPerSecond = 18,
                            memoryUsageMb = 256,
                        ),
                        model = request.model,
                        backend = request.backend,
                        generatedAt = "timestamp",
                    )
                }
            },
        )

        val request = PromptRequest(
            prompt = "Diagnose site alarm",
            industry = PromptIndustry.Energy,
            model = AiModel.Mock,
            backend = InferenceBackend.Mock,
        )

        val result = useCase(request)

        assertEquals(request, capturedRequest)
        assertEquals("Generated", result.text)
    }
}
