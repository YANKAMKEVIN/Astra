package com.kevin.astra.presentation.assistant

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.GenerationMetrics
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.settings.InMemoryAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantViewModelTest {
    @Test
    fun startsWithIndustrialMaintenanceAndMockMetrics() {
        val viewModel = AssistantViewModel(
            askLocalAssistant = testUseCase(),
            aiConfigurationRepository = testConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            promptPipeline = testPromptPipeline(),
        )

        val state = viewModel.state.value

        assertEquals(AssistantIndustry.IndustrialMaintenance, state.selectedIndustry)
        assertEquals("Mock Model", state.metrics.model)
        assertEquals("Mock Engine", state.metrics.backend)
        assertEquals("1.2 s", state.metrics.latency)
        assertEquals("18", state.metrics.tokensPerSecond)
        assertFalse(state.canAsk)
    }

    @Test
    fun updatesQuestionAndIndustry() {
        val viewModel = AssistantViewModel(
            askLocalAssistant = testUseCase(),
            aiConfigurationRepository = testConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            promptPipeline = testPromptPipeline(),
        )

        viewModel.dispatch(AssistantIntent.SelectIndustry(AssistantIndustry.Energy))
        viewModel.dispatch(AssistantIntent.UpdateQuestion("Restart Pump A"))

        val state = viewModel.state.value
        assertEquals(AssistantIndustry.Energy, state.selectedIndustry)
        assertEquals("Restart Pump A", state.question)
        assertTrue(state.canAsk)
    }

    @Test
    fun askQuestionShowsLoadingThenUseCaseResponse() = runBlocking {
        val viewModel = AssistantViewModel(
            askLocalAssistant = testUseCase(),
            aiConfigurationRepository = testConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            promptPipeline = testPromptPipeline(),
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.SelectIndustry(AssistantIndustry.Aerospace))
        viewModel.dispatch(AssistantIntent.UpdateQuestion("How do we restart Pump A safely?"))
        viewModel.dispatch(AssistantIntent.AskQuestion)

        yield()

        assertTrue(viewModel.state.value.isGenerating)

        delay(20)

        val state = viewModel.state.value
        assertFalse(state.isGenerating)
        assertEquals("Aerospace checklist assistance", state.response?.title)
        assertTrue(state.response?.body.orEmpty().contains("Aerospace"))
        assertEquals("2026-06-26T10:15:30Z", state.generationTimestamp)
        assertEquals("1.2 s", state.metrics.latency)
        assertEquals("18", state.metrics.tokensPerSecond)
        assertEquals("128", state.metrics.tokensGenerated)
    }

    @Test
    fun clearConversationResetsPromptResponseAndTimestamp() = runBlocking {
        val viewModel = AssistantViewModel(
            askLocalAssistant = testUseCase(),
            aiConfigurationRepository = testConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            promptPipeline = testPromptPipeline(),
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.UpdateQuestion("Restart Pump A"))
        viewModel.dispatch(AssistantIntent.AskQuestion)
        delay(20)

        assertNotNull(viewModel.state.value.response)

        viewModel.dispatch(AssistantIntent.ClearConversation)

        val state = viewModel.state.value
        assertEquals("", state.question)
        assertNull(state.response)
        assertNull(state.generationTimestamp)
        assertFalse(state.isGenerating)
    }

    @Test
    fun askQuestionUsesCurrentAiConfiguration() = runBlocking {
        var capturedRequest: PromptRequest? = null
        val repository = testConfigurationRepository().apply {
            updateTemperature(0.8)
            updateMaxTokens(1_024)
            updateContextWindow(8_192)
            updateQuantization("8-bit")
        }
        val useCase = AskLocalAssistantUseCase(
            inferenceEngine = object : InferenceEngine {
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    capturedRequest = request
                    return testGenerationResult(request)
                }
            },
        )
        val viewModel = AssistantViewModel(
            askLocalAssistant = useCase,
            aiConfigurationRepository = repository,
            modelCatalog = DefaultModelCatalog(),
            promptPipeline = testPromptPipeline(),
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.UpdateQuestion("Diagnose alarm"))
        viewModel.dispatch(AssistantIntent.AskQuestion)
        yield()
        delay(10)

        assertEquals(AiModel.Mock, capturedRequest?.model)
        assertEquals(InferenceBackend.Mock, capturedRequest?.backend)
        assertEquals(1_024, capturedRequest?.maxTokens)
        assertEquals(0.8, capturedRequest?.temperature)
        assertTrue(capturedRequest?.prompt.orEmpty().contains("System role"))
        assertTrue(capturedRequest?.prompt.orEmpty().contains("User request"))
        assertTrue(capturedRequest?.prompt.orEmpty().contains("Diagnose alarm"))
    }

    private fun testUseCase(): AskLocalAssistantUseCase =
        AskLocalAssistantUseCase(
            inferenceEngine = object : InferenceEngine {
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    delay(10)
                    return testGenerationResult(request)
                }
            },
        )

    private fun testConfigurationRepository(): AiConfigurationRepository =
        InMemoryAiConfigurationRepository()

    private fun testPromptPipeline(): PromptPipeline =
        DefaultPromptPipeline(DefaultPromptBuilder())

    private fun testGenerationResult(request: PromptRequest): GenerationResult =
        GenerationResult(
            text = "${request.industry.label} checklist assistance\n\nGenerated response",
            metrics = GenerationMetrics(
                latencyMillis = 1_200,
                timeToFirstTokenMillis = 320,
                tokensGenerated = 128,
                tokensPerSecond = 18,
                memoryUsageMb = 384,
            ),
            model = request.model,
            backend = request.backend,
            generatedAt = "2026-06-26T10:15:30Z",
        )
}
