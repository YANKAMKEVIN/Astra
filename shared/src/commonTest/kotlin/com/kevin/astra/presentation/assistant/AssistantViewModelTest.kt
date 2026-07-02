package com.kevin.astra.presentation.assistant

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.GenerationMetrics
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.data.demo.StaticDemoScenarioCatalog
import com.kevin.astra.data.documents.SmartTextChunker
import com.kevin.astra.data.documents.TfIdfContextRetriever
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.assistant.StreamEvent
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.LoadedPdfDocument
import com.kevin.astra.domain.documents.PdfExtractor
import com.kevin.astra.domain.documents.RetrievedDocumentContext
import com.kevin.astra.domain.export.ConversationShareHelper
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ConversationRepository
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.domain.vision.ImageClassificationResult
import com.kevin.astra.domain.vision.ImageClassifier
import com.kevin.astra.domain.vision.ImageLabel
import com.kevin.astra.domain.voice.SpeechRecognitionService
import com.kevin.astra.domain.voice.SpeechRecognitionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    // AssistantViewModel's init launches on viewModelScope (Dispatchers.Main); provide a test
    // dispatcher so construction works off the Android main thread.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithNoIndustryAndMockMetrics() {
        val viewModel = testViewModel()

        val state = viewModel.state.value

        // Industry persona is optional and unselected by default.
        assertNull(state.selectedIndustry)
        assertEquals("—", state.metrics.model)
        assertEquals("—", state.metrics.latency)
        assertEquals("—", state.metrics.tokensPerSecond)
        assertTrue(state.availableScenarios.isNotEmpty())
        assertFalse(state.canAsk)
    }

    @Test
    fun updatesQuestionAndIndustry() {
        val viewModel = testViewModel()

        viewModel.dispatch(AssistantIntent.SelectIndustry(AssistantIndustry.Energy))
        viewModel.dispatch(AssistantIntent.UpdateQuestion("Restart Pump A"))

        val state = viewModel.state.value
        assertEquals(AssistantIndustry.Energy, state.selectedIndustry)
        assertEquals("Restart Pump A", state.question)
        assertTrue(state.canAsk)
    }

    @Test
    fun selectingScenarioPopulatesQuestionAndUpdatesIndustry() {
        val catalog = StaticDemoScenarioCatalog()
        val scenario = catalog.scenarioById("hlth-01") ?: error("Missing healthcare demo scenario")
        val viewModel = testViewModel(demoScenarioCatalog = catalog)

        viewModel.dispatch(AssistantIntent.SelectScenario(scenario))

        val state = viewModel.state.value
        assertEquals(scenario.prompt, state.question)
        assertEquals(AssistantIndustry.Healthcare, state.selectedIndustry)
        assertTrue(state.canAsk)
        assertTrue(state.availableScenarios.isNotEmpty())
        assertTrue(state.availableScenarios.all { it.industry == PromptIndustry.Healthcare })
    }

    @Test
    fun askQuestionShowsLoadingThenAppendsAssistantMessage() = runBlocking {
        val viewModel = testViewModel(generationScope = CoroutineScope(coroutineContext))

        viewModel.dispatch(AssistantIntent.SelectIndustry(AssistantIndustry.Aerospace))
        viewModel.dispatch(AssistantIntent.UpdateQuestion("How do we restart Pump A safely?"))
        viewModel.dispatch(AssistantIntent.AskQuestion)

        yield()

        assertTrue(viewModel.state.value.isGenerating)

        delay(50)

        val state = viewModel.state.value
        assertFalse(state.isGenerating)
        // user bubble + assistant bubble
        assertEquals(2, state.messages.size)
        val assistant = state.messages.last()
        assertEquals(ChatRole.Assistant, assistant.role)
        assertTrue(assistant.text.contains("Aerospace checklist assistance"))
        assertEquals("1.2 s", state.metrics.latency)
        assertEquals("18", state.metrics.tokensPerSecond)
        assertEquals("128", state.metrics.tokensGenerated)
        assertEquals("Simulated Local Inference", state.metrics.runtimeMode)
    }

    @Test
    fun clearConversationResetsQuestionAndMessages() = runBlocking {
        val viewModel = testViewModel(generationScope = CoroutineScope(coroutineContext))

        viewModel.dispatch(AssistantIntent.UpdateQuestion("Restart Pump A"))
        viewModel.dispatch(AssistantIntent.AskQuestion)
        yield()
        delay(200)

        assertTrue(viewModel.state.value.messages.isNotEmpty())

        viewModel.dispatch(AssistantIntent.ClearConversation)

        val state = viewModel.state.value
        assertEquals("", state.question)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isGenerating)
    }

    @Test
    fun askQuestionUsesCurrentAiConfiguration() = runBlocking {
        var capturedRequest: PromptRequest? = null
        val repository = testAiConfigurationRepository()
        repository.apply {
            updateTemperature(0.8)
            updateMaxTokens(1_024)
            updateContextWindow(8_192)
            updateQuantization("8-bit")
        }
        val useCase = AskLocalAssistantUseCase(
            inferenceEngine = object : InferenceEngine {
                override fun generateStream(request: PromptRequest): Flow<StreamEvent> = flow {
                    capturedRequest = request
                    emit(StreamEvent.Complete(testGenerationResult(request)))
                }
                override suspend fun generate(request: PromptRequest): GenerationResult = testGenerationResult(request)
            },
        )
        val viewModel = testViewModel(
            useCase = useCase,
            configurationRepository = repository,
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.UpdateQuestion("Diagnose alarm"))
        viewModel.dispatch(AssistantIntent.AskQuestion)
        yield()
        delay(20)

        assertEquals(AiModel.Mock, capturedRequest?.model)
        assertEquals(InferenceBackend.Mock, capturedRequest?.backend)
        assertEquals(1_024, capturedRequest?.maxTokens)
        assertEquals(0.8, capturedRequest?.temperature)
        assertTrue(capturedRequest?.prompt.orEmpty().contains("System role"))
        assertTrue(capturedRequest?.prompt.orEmpty().contains("User request"))
        assertTrue(capturedRequest?.prompt.orEmpty().contains("Diagnose alarm"))
    }

    /**
     * Regression: attaching an image transitions the attachment from Indexing to Ready with the
     * SAME bytes. AttachedImage.equals must include status/classification, otherwise StateFlow
     * treats the Ready state as unchanged, suppresses the emission and leaves canAsk stuck false.
     */
    @Test
    fun attachingImageReachesReadyAndUnblocksAsk() = runBlocking {
        val classification = ImageClassificationResult(
            labels = listOf(ImageLabel("pump", 0.92f)),
            modelUsed = "test-classifier",
        )
        val viewModel = testViewModel(
            imageClassifier = FakeImageClassifier(classification),
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.UpdateQuestion("What is in this image?"))
        viewModel.dispatch(AssistantIntent.ImageAttached(byteArrayOf(1, 2, 3, 4)))
        yield()
        delay(50)

        val state = viewModel.state.value
        assertEquals(AttachmentStatus.Ready, state.attachedImage?.status)
        assertEquals(classification, state.attachedImage?.classification)
        assertTrue(state.canAsk)
    }

    /**
     * Regression: when a PDF is attached before the user types (or the prompt is later edited),
     * retrieval must be re-ranked against the FINAL question at ask-time — not reuse context
     * cached with the placeholder "Summarize this document." prompt.
     */
    @Test
    fun askQuestionReRanksAttachedPdfWithFinalQuestion() = runBlocking {
        val recordingRetriever = RecordingContextRetriever()
        val viewModel = testViewModel(
            contextRetriever = recordingRetriever,
            generationScope = CoroutineScope(coroutineContext),
        )

        // Attach the PDF first, while the question is still blank.
        viewModel.dispatch(AssistantIntent.PdfAttached("pump maintenance guide".repeat(40).encodeToByteArray(), "guide.pdf"))
        delay(100)
        // The initial ranking used the placeholder prompt.
        assertEquals("Summarize this document.", recordingRetriever.lastQuestion)

        // Now type the real question and ask.
        viewModel.dispatch(AssistantIntent.UpdateQuestion("How do I restart the pump safely?"))
        viewModel.dispatch(AssistantIntent.AskQuestion)
        yield()
        delay(100)

        // Retrieval was re-run with the final question, not the placeholder.
        assertEquals("How do I restart the pump safely?", recordingRetriever.lastQuestion)
    }

    private fun testViewModel(
        useCase: AskLocalAssistantUseCase = testUseCase(),
        configurationRepository: AiConfigurationRepository = testAiConfigurationRepository(),
        demoScenarioCatalog: DemoScenarioCatalog = StaticDemoScenarioCatalog(),
        imageClassifier: ImageClassifier = FakeImageClassifier(),
        contextRetriever: DocumentContextRetriever = TfIdfContextRetriever(),
        generationScope: CoroutineScope? = null,
    ): AssistantViewModel =
        AssistantViewModel(
            askLocalAssistant = useCase,
            aiConfigurationRepository = configurationRepository,
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            promptPipeline = testPromptPipeline(),
            demoScenarioCatalog = demoScenarioCatalog,
            notificationService = NoOpNotificationService(),
            conversationRepository = NoOpConversationRepository(),
            pdfExtractor = FakePdfExtractor(),
            chunker = SmartTextChunker(),
            contextRetriever = contextRetriever,
            imageClassifier = imageClassifier,
            speechRecognitionService = FakeSpeechRecognitionService(),
            shareHelper = NoOpConversationShareHelper(),
            generationScope = generationScope,
        )

    private fun testUseCase(): AskLocalAssistantUseCase =
        AskLocalAssistantUseCase(
            inferenceEngine = object : InferenceEngine {
                override fun generateStream(request: PromptRequest): Flow<StreamEvent> = flow {
                    delay(10)
                    emit(StreamEvent.Complete(testGenerationResult(request)))
                }
                override suspend fun generate(request: PromptRequest): GenerationResult {
                    delay(10)
                    return testGenerationResult(request)
                }
            },
        )

    private fun testPromptPipeline(): PromptPipeline =
        DefaultPromptPipeline(DefaultPromptBuilder())

    private fun testGenerationResult(request: PromptRequest): GenerationResult =
        GenerationResult(
            text = "${request.industry?.label ?: "General"} checklist assistance\n\nGenerated response",
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

private class NoOpNotificationService : NotificationService {
    override fun showNotification(title: String, message: String, targetDestination: AstraDestination?) = Unit
}

private class NoOpConversationRepository : ConversationRepository {
    override fun save(conversation: ChatConversation) = Unit
    override fun getAll(): List<ChatConversation> = emptyList()
    override fun getById(id: String): ChatConversation? = null
    override fun delete(id: String): Boolean = false
    override fun search(query: String): List<ChatConversation> = emptyList()
}

private class FakePdfExtractor : PdfExtractor {
    override fun extract(pdfBytes: ByteArray, fileName: String): LoadedPdfDocument =
        LoadedPdfDocument(fileName = fileName, rawText = pdfBytes.decodeToString(), pageCount = 1)
}

private class FakeImageClassifier(
    private val result: ImageClassificationResult = ImageClassificationResult(
        labels = listOf(ImageLabel("object", 0.5f)),
        modelUsed = "test-classifier",
    ),
) : ImageClassifier {
    override fun classify(imageBytes: ByteArray): ImageClassificationResult = result
    override val isAvailable: Boolean = true
}

private class FakeSpeechRecognitionService : SpeechRecognitionService {
    override val state: StateFlow<SpeechRecognitionState> = MutableStateFlow(SpeechRecognitionState.Idle)
    override fun startListening() = Unit
    override fun stopListening() = Unit
    override fun destroy() = Unit
}

private class NoOpConversationShareHelper : ConversationShareHelper {
    override fun share(conversation: ChatConversation, format: ExportFormat) = Unit
}

private class RecordingContextRetriever : DocumentContextRetriever {
    var lastQuestion: String? = null
        private set

    override fun retrieve(
        question: String,
        chunks: List<IndexedDocumentChunk>,
        maxChunks: Int,
    ): RetrievedDocumentContext {
        lastQuestion = question
        return RetrievedDocumentContext(chunks.take(maxChunks))
    }
}
