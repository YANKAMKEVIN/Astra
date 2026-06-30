package com.kevin.astra.presentation.documents

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.GenerationMetrics
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.documents.SmartTextChunker
import com.kevin.astra.data.documents.TfIdfContextRetriever
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.assistant.StreamEvent
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.LoadedPdfDocument
import com.kevin.astra.domain.documents.PdfExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentsViewModelTest {

    @Test
    fun startsWithNoDocumentLoaded() {
        val viewModel = testViewModel()
        val state = viewModel.state.value

        assertNull(state.loadedFileName)
        assertEquals(DocumentStatus.NotIndexed, state.documentStatus)
        assertFalse(state.canIndex)
        assertFalse(state.canAsk)
    }

    @Test
    fun loadingPdfExtractsAndAutoIndexes() = runBlocking {
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext))

        val fakeText = "This document describes pump maintenance. " .repeat(100)
        val fakeBytes = fakeText.encodeToByteArray()
        viewModel.dispatch(DocumentsIntent.PdfLoaded(fakeBytes, "pump-guide.pdf"))
        yield()
        delay(200)

        val state = viewModel.state.value
        assertEquals("pump-guide.pdf", state.loadedFileName)
        assertEquals(DocumentStatus.Indexed, state.documentStatus)
        assertTrue(state.indexedChunks.isNotEmpty())
        assertFalse(state.isIndexing)
    }

    @Test
    fun asksDocumentAfterIndexing() = runBlocking {
        var capturedRequest: PromptRequest? = null
        val viewModel = testViewModel(
            workScope = CoroutineScope(coroutineContext),
            inferenceEngine = object : InferenceEngine {
                override fun generateStream(request: PromptRequest): Flow<StreamEvent> = emptyFlow()
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    capturedRequest = request
                    delay(10)
                    return GenerationResult(
                        text = "Pump answer\n\nBased on the document.",
                        metrics = GenerationMetrics(
                            latencyMillis = 1_200,
                            timeToFirstTokenMillis = 320,
                            tokensGenerated = 60,
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

        val fakeText = "Pump restart procedure: turn off power, wait 30 seconds, then restart. ".repeat(80)
        viewModel.dispatch(DocumentsIntent.PdfLoaded(fakeText.encodeToByteArray(), "guide.pdf"))
        delay(200)

        viewModel.dispatch(DocumentsIntent.UpdateQuestion("How do I restart the pump?"))
        viewModel.dispatch(DocumentsIntent.AskDocument)
        yield()
        delay(200)

        val state = viewModel.state.value
        assertFalse(state.isGenerating)
        assertNotNull(state.extractedContext)
        assertTrue(state.extractedContext!!.chunks.isNotEmpty())
        assertEquals("Pump answer", state.answer?.title)
        assertEquals("1.2 s", state.metrics.latency)
        assertTrue(capturedRequest != null)
    }

    @Test
    fun clearDocumentResetsAllState() = runBlocking {
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext))
        viewModel.dispatch(DocumentsIntent.PdfLoaded("content".repeat(50).encodeToByteArray(), "doc.pdf"))
        delay(200)
        viewModel.dispatch(DocumentsIntent.ClearDocument)

        val state = viewModel.state.value
        assertNull(state.loadedFileName)
        assertEquals(DocumentStatus.NotIndexed, state.documentStatus)
        assertTrue(state.indexedChunks.isEmpty())
    }

    private fun testViewModel(
        inferenceEngine: InferenceEngine = object : InferenceEngine {
            override fun generateStream(request: PromptRequest): Flow<StreamEvent> = emptyFlow()
            override suspend fun generate(request: PromptRequest): GenerationResult =
                GenerationResult(
                    text = "Answer\n\nContent",
                    metrics = GenerationMetrics(1_200, 320, 80, 18, 384),
                    model = request.model,
                    backend = request.backend,
                    generatedAt = "ts",
                )
        },
        workScope: CoroutineScope? = null,
    ): DocumentsViewModel =
        DocumentsViewModel(
            pdfExtractor = FakePdfExtractor(),
            chunker = SmartTextChunker(),
            contextRetriever = TfIdfContextRetriever(),
            askLocalAssistant = AskLocalAssistantUseCase(inferenceEngine),
            aiConfigurationRepository = testAiConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            promptPipeline = DefaultPromptPipeline(DefaultPromptBuilder()),
            notificationService = NoOpNotificationService(),
            workScope = workScope,
        )
}

private class FakePdfExtractor : PdfExtractor {
    override fun extract(pdfBytes: ByteArray, fileName: String): LoadedPdfDocument =
        LoadedPdfDocument(
            fileName = fileName,
            rawText = pdfBytes.decodeToString(),
            pageCount = 1,
        )
}

private class NoOpNotificationService : NotificationService {
    override fun showNotification(title: String, message: String, targetDestination: AstraDestination?) = Unit
}
