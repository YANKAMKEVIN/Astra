package com.kevin.astra.presentation.documents

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.GenerationMetrics
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.data.documents.KeywordDocumentContextRetriever
import com.kevin.astra.data.documents.SimpleDocumentIndexer
import com.kevin.astra.data.settings.InMemoryAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.documents.DocumentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentsViewModelTest {
    @Test
    fun startsWithEmbeddedDocumentNotIndexed() {
        val viewModel = testViewModel()

        val state = viewModel.state.value

        assertEquals(1, state.availableDocuments.size)
        assertEquals("Industrial Pump Maintenance Guide", state.selectedDocument?.title)
        assertEquals(DocumentStatus.NotIndexed, state.documentStatus)
        assertTrue(state.canIndex)
        assertFalse(state.canAsk)
    }

    @Test
    fun indexesSelectedDocument() = runBlocking {
        val viewModel = testViewModel(scope = CoroutineScope(coroutineContext))

        viewModel.dispatch(DocumentsIntent.IndexSelectedDocument)
        yield()

        assertTrue(viewModel.state.value.isIndexing)

        delay(700)

        val state = viewModel.state.value
        assertFalse(state.isIndexing)
        assertEquals(DocumentStatus.Indexed, state.documentStatus)
        assertTrue(state.indexedChunks.isNotEmpty())
    }

    @Test
    fun asksDocumentWithExtractedContextAndMetrics() = runBlocking {
        var capturedRequest: PromptRequest? = null
        val viewModel = testViewModel(
            scope = CoroutineScope(coroutineContext),
            inferenceEngine = object : InferenceEngine {
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    capturedRequest = request
                    delay(10)
                    return GenerationResult(
                        text = "Pump restart answer\n\nUse the extracted guide context.",
                        metrics = GenerationMetrics(
                            latencyMillis = 1_200,
                            timeToFirstTokenMillis = 320,
                            tokensGenerated = 96,
                            tokensPerSecond = 18,
                            memoryUsageMb = 384,
                        ),
                        model = AiModel.Mock,
                        backend = InferenceBackend.Mock,
                        generatedAt = "timestamp",
                    )
                }
            },
        )

        viewModel.dispatch(DocumentsIntent.IndexSelectedDocument)
        delay(700)
        viewModel.dispatch(DocumentsIntent.UpdateQuestion("How should I restart Pump A?"))
        viewModel.dispatch(DocumentsIntent.AskDocument)
        yield()

        assertTrue(viewModel.state.value.isGenerating)

        delay(20)

        val state = viewModel.state.value
        assertFalse(state.isGenerating)
        assertNotNull(state.extractedContext)
        assertTrue(state.extractedContext.text.contains("Pump Restart Procedure"))
        assertEquals("Pump restart answer", state.answer?.title)
        assertEquals("1.2 s", state.metrics.latency)
        assertEquals("18", state.metrics.tokensPerSecond)
        assertTrue(capturedRequest?.prompt.orEmpty().contains("Extracted context:"))
    }

    private fun testViewModel(
        scope: CoroutineScope? = null,
        inferenceEngine: InferenceEngine = object : InferenceEngine {
            override suspend fun generate(request: PromptRequest): GenerationResult =
                GenerationResult(
                    text = "Document answer\n\nGenerated response",
                    metrics = GenerationMetrics(
                        latencyMillis = 1_200,
                        timeToFirstTokenMillis = 320,
                        tokensGenerated = 80,
                        tokensPerSecond = 18,
                        memoryUsageMb = 384,
                    ),
                    model = request.model,
                    backend = request.backend,
                    generatedAt = "timestamp",
                )
        },
    ): DocumentsViewModel =
        DocumentsViewModel(
            documentIndexer = SimpleDocumentIndexer(),
            contextRetriever = KeywordDocumentContextRetriever(),
            askLocalAssistant = AskLocalAssistantUseCase(inferenceEngine),
            aiConfigurationRepository = InMemoryAiConfigurationRepository(),
            documentsScope = scope,
        )
}
