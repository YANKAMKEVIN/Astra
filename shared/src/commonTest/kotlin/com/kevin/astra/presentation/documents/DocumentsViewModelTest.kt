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
import com.kevin.astra.data.documents.DocumentIndexFile
import com.kevin.astra.data.documents.DocumentIndexStore
import com.kevin.astra.data.documents.NoOpDocumentIndexFile
import com.kevin.astra.data.documents.SmartTextChunker
import com.kevin.astra.data.documents.TfIdfContextRetriever
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.assistant.StreamEvent
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.EmailExtractor
import com.kevin.astra.domain.documents.FetchGmailUseCase
import com.kevin.astra.domain.documents.IndexEmailFileUseCase
import com.kevin.astra.domain.documents.IndexedSource
import com.kevin.astra.domain.documents.LoadedEmailDocument
import com.kevin.astra.domain.documents.LoadedPdfDocument
import com.kevin.astra.domain.documents.PdfExtractor
import com.kevin.astra.domain.gmail.GmailApiException
import com.kevin.astra.domain.gmail.GmailController
import com.kevin.astra.domain.gmail.GmailIntegration
import com.kevin.astra.domain.gmail.GmailMessageSource
import com.kevin.astra.domain.gmail.GmailTokenProvider
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.kevin.astra.test.awaitCondition
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.Volatile
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertContains
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
    fun followsTheModelSelectedElsewhereInsteadOfTheLaunchTimeOne() {
        val catalog = DefaultModelCatalog()
        val viewModel = testViewModel(modelCatalog = catalog)
        assertNull(viewModel.state.value.sessionModel, "Docs must not pin a model by default")

        // The user downloads and activates a model from the Models screen…
        catalog.updateModelStatus("qwen3-0-6b", com.kevin.astra.core.ai.ModelStatus.Installed)
        assertTrue(catalog.selectModel("qwen3-0-6b"))
        // …then comes back to Docs.
        viewModel.dispatch(DocumentsIntent.RefreshModels)

        val state = viewModel.state.value
        assertEquals("qwen3-0-6b", state.activeModel?.id)
        assertTrue(state.availableModels.any { it.id == "qwen3-0-6b" })
        assertNull(state.sessionModel)
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
    fun loadingEmailExtractsIndexesAndMarksSourceAsEmail() = runBlocking {
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext))

        val fakeText = "Subject: Pump maintenance report. Quarterly service is required. ".repeat(60)
        viewModel.dispatch(DocumentsIntent.EmailLoaded(fakeText.encodeToByteArray(), "inbox.mbox"))
        yield()
        delay(200)

        val state = viewModel.state.value
        assertEquals("inbox.mbox", state.loadedFileName)
        assertEquals(DocumentSourceType.Email, state.sourceType)
        assertEquals(DocumentStatus.Indexed, state.documentStatus)
        assertTrue(state.indexedChunks.isNotEmpty())
        assertTrue(state.emailCount >= 1)
        assertFalse(state.isIndexing)
    }

    @Test
    fun fetchingGmailRecentIndexesResultAsEmailSource() = runBlocking {
        val gmailDoc = LoadedEmailDocument(
            fileName = "Gmail (latest 20)",
            emailCount = 3,
            rawText = "Subject: Invoice. Payment is due next week. ".repeat(60),
        )
        val viewModel = testViewModel(
            workScope = CoroutineScope(coroutineContext),
            gmailSource = FakeGmailSource(gmailDoc),
        )

        viewModel.dispatch(DocumentsIntent.FetchGmailRecent)
        yield()
        delay(200)

        val state = viewModel.state.value
        assertEquals("Gmail (latest 20)", state.loadedFileName)
        assertEquals(DocumentSourceType.Email, state.sourceType)
        assertEquals(DocumentStatus.Indexed, state.documentStatus)
        assertTrue(state.indexedChunks.isNotEmpty())
        assertEquals(3, state.emailCount)
        assertFalse(state.isFetchingGmail)
    }

    @Test
    fun loadMoreRefetchesTheSameGmailRequestWithTwentyMoreMessages() = runBlocking {
        val calls = mutableListOf<Pair<String?, Int>>()
        val source = GmailMessageSource { query, maxResults, _ ->
            calls += query to maxResults
            LoadedEmailDocument(
                fileName = "Gmail",
                emailCount = maxResults,
                rawText = "Subject: Invoice. Payment is due next week. ".repeat(60),
                hasMore = maxResults < 40,
            )
        }
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext), gmailSource = source)

        viewModel.dispatch(DocumentsIntent.UpdateGmailQuery("from:bank"))
        viewModel.dispatch(DocumentsIntent.FetchGmailSearch)
        yield()
        delay(200)
        assertEquals(GmailFetch(query = "from:bank", count = 20, hasMore = true), viewModel.state.value.gmailFetch)

        viewModel.dispatch(DocumentsIntent.FetchGmailMore)
        yield()
        delay(200)

        assertEquals(listOf<Pair<String?, Int>>("from:bank" to 20, "from:bank" to 40), calls)
        assertEquals(40, viewModel.state.value.emailCount)
        assertEquals(false, viewModel.state.value.gmailFetch?.hasMore)

        // Nothing more to load: the intent is ignored.
        viewModel.dispatch(DocumentsIntent.FetchGmailMore)
        yield()
        delay(100)
        assertEquals(2, calls.size)
    }

    @Test
    fun anExpiredGmailTokenShowsAReconnectMessageNotAnEmptyInbox() = runBlocking {
        val controller = FakeGmailController(connected = true)
        GmailIntegration.controller = controller
        try {
            val source = GmailMessageSource { _, _, _ -> throw GmailApiException(HttpStatusCode.Unauthorized) }
            val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext), gmailSource = source)

            viewModel.dispatch(DocumentsIntent.FetchGmailRecent)
            yield()
            delay(200)

            val state = viewModel.state.value
            assertContains(state.error.orEmpty(), "Reconnect your Gmail account")
            assertFalse(state.isFetchingGmail)
            assertFalse(state.gmailConnected)
            assertTrue(controller.disconnected)
        } finally {
            GmailIntegration.controller = null
        }
    }

    @Test
    fun aGmailServerErrorKeepsTheAccountConnected() = runBlocking {
        val controller = FakeGmailController(connected = true)
        GmailIntegration.controller = controller
        try {
            val source = GmailMessageSource { _, _, _ -> throw GmailApiException(HttpStatusCode.ServiceUnavailable) }
            val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext), gmailSource = source)

            viewModel.dispatch(DocumentsIntent.FetchGmailRecent)
            yield()
            delay(200)

            assertContains(viewModel.state.value.error.orEmpty(), "503")
            assertTrue(viewModel.state.value.gmailConnected)
            assertFalse(controller.disconnected)
        } finally {
            GmailIntegration.controller = null
        }
    }

    @Test
    fun aSecondDocumentJoinsTheLibraryAndQuestionsSearchBoth() = runBlocking {
        var capturedRequest: PromptRequest? = null
        val viewModel = testViewModel(
            workScope = CoroutineScope(coroutineContext),
            inferenceEngine = recordingEngine { capturedRequest = it },
        )

        viewModel.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
        awaitState(viewModel) { library.size == 1 && !isSummarizing }
        viewModel.dispatch(DocumentsIntent.PdfLoaded(invoiceText.encodeToByteArray(), "invoice.pdf"))
        awaitState(viewModel) { library.size == 2 && !isSummarizing }

        assertEquals(listOf("pump.pdf", "invoice.pdf"), viewModel.state.value.library.map { it.name })
        assertEquals("invoice.pdf", viewModel.state.value.loadedFileName)

        viewModel.dispatch(DocumentsIntent.UpdateQuestion("How do I restart the pump?"))
        viewModel.dispatch(DocumentsIntent.AskDocument)
        awaitState(viewModel) { answer != null && !isGenerating }

        // The pump guide is no longer the current document, but it is still searched.
        val context = viewModel.state.value.extractedContext?.text.orEmpty()
        assertContains(context, "Pump restart procedure")
        assertContains(capturedRequest?.prompt.orEmpty(), "Pump restart procedure")
    }

    @Test
    fun withSeveralDocumentsASummaryQuestionIsAnsweredFromThemAll() = runBlocking {
        var generations = 0
        val viewModel = testViewModel(
            workScope = CoroutineScope(coroutineContext),
            inferenceEngine = recordingEngine { generations++ },
        )
        viewModel.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
        awaitState(viewModel) { library.size == 1 && !isSummarizing }
        viewModel.dispatch(DocumentsIntent.PdfLoaded(invoiceText.encodeToByteArray(), "invoice.pdf"))
        awaitState(viewModel) { library.size == 2 && !isSummarizing && documentSummary != null }
        val before = generations

        viewModel.dispatch(DocumentsIntent.UpdateQuestion("Résume ces documents"))
        viewModel.dispatch(DocumentsIntent.AskDocument)
        awaitState(viewModel) { answer != null && !isGenerating }

        assertEquals(before + 1, generations)
        assertTrue(viewModel.state.value.answer?.title != "Document Summary")
    }

    @Test
    fun withOneDocumentASummaryQuestionUsesTheSavedSummary() = runBlocking {
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext))
        viewModel.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
        awaitState(viewModel) { documentSummary != null && !isSummarizing }

        viewModel.dispatch(DocumentsIntent.UpdateQuestion("De quoi parle ce document ?"))
        viewModel.dispatch(DocumentsIntent.AskDocument)

        assertEquals("Document Summary", viewModel.state.value.answer?.title)
    }

    @Test
    fun removingTheCurrentSourceShowsThePreviousOne() = runBlocking {
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext))
        viewModel.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
        awaitState(viewModel) { library.size == 1 && !isSummarizing }
        viewModel.dispatch(DocumentsIntent.EmailLoaded(invoiceText.encodeToByteArray(), "invoice.eml"))
        awaitState(viewModel) { library.size == 2 && !isSummarizing }

        viewModel.dispatch(DocumentsIntent.RemoveSource(IndexedSource.emailId("invoice.eml")))

        val state = viewModel.state.value
        assertEquals(listOf("pump.pdf"), state.library.map { it.name })
        assertEquals("pump.pdf", state.loadedFileName)
        assertEquals(DocumentSourceType.Pdf, state.sourceType)
        assertTrue(state.indexedChunks.isNotEmpty())

        viewModel.dispatch(DocumentsIntent.RemoveSource(IndexedSource.pdfId("pump.pdf")))
        assertNull(viewModel.state.value.loadedFileName)
        assertFalse(viewModel.state.value.hasIndexedContent)
    }

    @Test
    fun theIndexIsSavedAndRestoredByTheNextViewModel() = runBlocking {
        val file = InMemoryDocumentIndexFile()
        val first = testViewModel(workScope = CoroutineScope(coroutineContext), indexFile = file)
        first.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
        awaitState(first) { documentSummary != null && !isSummarizing }
        awaitCondition { file.text?.contains("Answer") == true } // summary saved too

        val restored = testViewModel(workScope = CoroutineScope(coroutineContext), indexFile = file)
        awaitState(restored) { library.isNotEmpty() }

        val state = restored.state.value
        assertEquals("pump.pdf", state.loadedFileName)
        assertEquals(DocumentStatus.Indexed, state.documentStatus)
        assertEquals(first.state.value.indexedChunks, state.indexedChunks)
        assertEquals(first.state.value.documentSummary, state.documentSummary)
        restored.dispatch(DocumentsIntent.UpdateQuestion("How do I restart the pump?"))
        assertTrue(restored.state.value.canAsk)
    }

    @Test
    fun clearingEverythingDeletesTheSavedIndex() = runBlocking {
        val file = InMemoryDocumentIndexFile()
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext), indexFile = file)
        viewModel.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
        awaitState(viewModel) { library.size == 1 && !isSummarizing }
        awaitCondition { file.text != null }

        viewModel.dispatch(DocumentsIntent.ClearDocument)

        awaitCondition { file.text == null }
        assertTrue(viewModel.state.value.library.isEmpty())
    }

    @Test
    fun disconnectingGmailForgetsTheFetchedMail() = runBlocking {
        val controller = FakeGmailController(connected = true)
        GmailIntegration.controller = controller
        try {
            val gmailDoc = LoadedEmailDocument(fileName = "Gmail (latest 20)", emailCount = 2, rawText = invoiceText)
            val viewModel = testViewModel(
                workScope = CoroutineScope(coroutineContext),
                gmailSource = FakeGmailSource(gmailDoc),
            )
            viewModel.dispatch(DocumentsIntent.PdfLoaded(pumpText.encodeToByteArray(), "pump.pdf"))
            awaitState(viewModel) { library.size == 1 && !isSummarizing }
            viewModel.dispatch(DocumentsIntent.FetchGmailRecent)
            awaitState(viewModel) { library.size == 2 && !isSummarizing }

            viewModel.dispatch(DocumentsIntent.DisconnectGmail)

            assertEquals(listOf("pump.pdf"), viewModel.state.value.library.map { it.name })
            assertEquals("pump.pdf", viewModel.state.value.loadedFileName)
        } finally {
            GmailIntegration.controller = null
        }
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
        assertTrue(state.extractedContext?.chunks.orEmpty().isNotEmpty())
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

    @Test
    fun loadingNewPdfClearsPreviousDocumentState() = runBlocking {
        val viewModel = testViewModel(workScope = CoroutineScope(coroutineContext))

        viewModel.dispatch(DocumentsIntent.PdfLoaded("first doc content".repeat(50).encodeToByteArray(), "first.pdf"))
        awaitState(viewModel) { loadedFileName == "first.pdf" && documentStatus == DocumentStatus.Indexed }

        // Record every state: the extraction runs on another thread and can finish before the
        // test looks, so a single read after yield() raced with it (it failed on a busy machine).
        val states = mutableListOf<DocumentsState>()
        val recorder = launch(Dispatchers.Unconfined) { viewModel.state.collect { states += it } }
        viewModel.dispatch(DocumentsIntent.PdfLoaded("second doc content".repeat(50).encodeToByteArray(), "second.pdf"))
        awaitState(viewModel) { loadedFileName == "second.pdf" }
        recorder.cancel()

        // During loading transition: previous file metadata must be cleared immediately
        val transitioning = states.firstOrNull { it.loadedFileName == null }
        assertNotNull(transitioning, "no cleared state between the two documents")
        assertNull(transitioning.answer)
        assertNull(transitioning.extractedContext)
        assertTrue(states.indexOf(transitioning) < states.indexOfFirst { it.loadedFileName == "second.pdf" })
    }

    @Test
    fun clearingDuringGenerationCancelsJobAndLeavesCleanState() = runBlocking {
        val scope = CoroutineScope(coroutineContext)
        val viewModel = testViewModel(
            workScope = scope,
            inferenceEngine = object : InferenceEngine {
                override fun generateStream(request: PromptRequest): Flow<StreamEvent> = emptyFlow()
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    delay(5_000) // simulate long-running generation
                    return GenerationResult(
                        text = "Stale answer\n\nShould never appear",
                        metrics = GenerationMetrics(1_200, 320, 80, 18, 384),
                        model = AiModel.Mock,
                        backend = InferenceBackend.Mock,
                        generatedAt = "ts",
                    )
                }
            },
        )

        // Index a document so canAsk becomes true
        viewModel.dispatch(DocumentsIntent.PdfLoaded("pump guide content".repeat(50).encodeToByteArray(), "pump.pdf"))
        delay(200)

        // Start generation then immediately clear — races the in-flight job
        viewModel.dispatch(DocumentsIntent.UpdateQuestion("What is the restart procedure?"))
        viewModel.dispatch(DocumentsIntent.AskDocument)
        viewModel.dispatch(DocumentsIntent.ClearDocument)
        // Give the cancelled coroutine time to settle
        delay(200)

        val state = viewModel.state.value
        assertNull(state.loadedFileName)
        assertNull(state.answer)
        assertFalse(state.isGenerating)
        assertEquals(DocumentStatus.NotIndexed, state.documentStatus)
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
        gmailSource: GmailMessageSource? = null,
        modelCatalog: DefaultModelCatalog = DefaultModelCatalog(),
        indexFile: DocumentIndexFile = NoOpDocumentIndexFile,
    ): DocumentsViewModel {
        val chunker = SmartTextChunker()
        return DocumentsViewModel(
            pdfExtractor = FakePdfExtractor(),
            indexEmailFile = IndexEmailFileUseCase(FakeEmailExtractor(), chunker),
            fetchGmailUseCase = FetchGmailUseCase(gmailSource, chunker),
            chunker = chunker,
            contextRetriever = TfIdfContextRetriever(),
            askLocalAssistant = AskLocalAssistantUseCase(inferenceEngine),
            aiConfigurationRepository = testAiConfigurationRepository(),
            modelCatalog = modelCatalog,
            backendCatalog = DefaultBackendCatalog(),
            promptPipeline = DefaultPromptPipeline(DefaultPromptBuilder()),
            notificationService = NoOpNotificationService(),
            documentIndexStore = DocumentIndexStore(indexFile),
            workScope = workScope,
        )
    }

    private val pumpText = "Pump restart procedure: turn off power, wait 30 seconds, then restart. ".repeat(40)
    private val invoiceText = "Invoice 2026-114: the payment of 420 euros is due on 30 November. ".repeat(40)

    private fun recordingEngine(onGenerate: (PromptRequest) -> Unit) = object : InferenceEngine {
        override fun generateStream(request: PromptRequest): Flow<StreamEvent> = emptyFlow()
        override suspend fun generate(request: PromptRequest): GenerationResult {
            onGenerate(request)
            return GenerationResult(
                text = "Answer\n\nContent",
                metrics = GenerationMetrics(1_200, 320, 80, 18, 384),
                model = request.model,
                backend = request.backend,
                generatedAt = "ts",
            )
        }
    }

    // Waits for the state instead of a fixed delay, so a busy machine does not fail the test.
    private suspend fun awaitState(viewModel: DocumentsViewModel, condition: DocumentsState.() -> Boolean) =
        awaitCondition { viewModel.state.value.condition() }
}

private class FakeGmailSource(private val doc: LoadedEmailDocument) : GmailMessageSource {
    var lastQuery: String? = null
        private set

    override suspend fun fetchAsSingleDocument(query: String?, maxResults: Int, label: String): LoadedEmailDocument {
        lastQuery = query
        return doc
    }
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

private class FakeEmailExtractor : EmailExtractor {
    override fun extractEml(bytes: ByteArray, fileName: String): LoadedEmailDocument =
        LoadedEmailDocument(fileName = fileName, emailCount = 1, rawText = bytes.decodeToString())

    override fun extractMbox(bytes: ByteArray, fileName: String): LoadedEmailDocument =
        LoadedEmailDocument(fileName = fileName, emailCount = 1, rawText = bytes.decodeToString())
}

private class FakeGmailController(private var connected: Boolean) : GmailController {
    var disconnected = false
        private set
    override val isSupported: Boolean = true
    override fun isConnected(): Boolean = connected
    override fun connect() { connected = true }
    override fun disconnect() { connected = false; disconnected = true }
    override fun tokenProvider(): GmailTokenProvider? = null
}

private class InMemoryDocumentIndexFile : DocumentIndexFile {
    @Volatile
    var text: String? = null
        private set
    override fun read(): String? = text
    override fun write(text: String) { this.text = text }
    override fun delete() { text = null }
}
