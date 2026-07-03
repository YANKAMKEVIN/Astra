package com.kevin.astra.presentation.assistant

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.data.documents.SmartTextChunker
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.assistant.StreamEvent
import com.kevin.astra.domain.assistant.StaticPromptTemplateCatalog
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.EmailExtractor
import com.kevin.astra.domain.documents.PdfExtractor
import com.kevin.astra.domain.gmail.GmailIntegration
import com.kevin.astra.domain.gmail.GmailMessageSource
import com.kevin.astra.domain.export.ConversationShareHelper
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage
import com.kevin.astra.domain.history.ConversationRepository
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.domain.voice.SpeechRecognitionService
import com.kevin.astra.domain.voice.SpeechRecognitionState
import com.kevin.astra.domain.vision.ImageClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class AssistantViewModel(
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val demoScenarioCatalog: DemoScenarioCatalog,
    private val notificationService: NotificationService,
    private val conversationRepository: ConversationRepository,
    private val pdfExtractor: PdfExtractor,
    private val emailExtractor: EmailExtractor,
    private val chunker: SmartTextChunker,
    private val contextRetriever: DocumentContextRetriever,
    private val imageClassifier: ImageClassifier,
    private val speechRecognitionService: SpeechRecognitionService,
    private val shareHelper: ConversationShareHelper,
    private val gmailSource: GmailMessageSource? = null,
    private val generationScope: CoroutineScope? = null,
) : AstraViewModel<AssistantState, AssistantIntent, AssistantEffect>(
    initialState = AssistantState(
        availableScenarios = demoScenarioCatalog.scenarios(),
        promptTemplates = StaticPromptTemplateCatalog.all,
        installedModels = modelCatalog.installedModels(),
        sessionModel = modelCatalog.currentModel(),
        gmailSupported = GmailIntegration.controller?.isSupported == true,
        gmailConnected = GmailIntegration.controller?.isConnected() == true,
    ),
) {
    private var generationJob: Job? = null

    init {
        updateState { copy(recentHistory = conversationRepository.getAll().takeLast(20).reversed()) }
        // Sync sessionModel with persisted configuration so that Settings changes are reflected
        viewModelScope.launch {
            val config = aiConfigurationRepository.getConfiguration()
            val configModel = modelCatalog.modelById(config.selectedModelId)
            if (configModel != null) {
                updateState { copy(sessionModel = configModel) }
            }
        }
        speechRecognitionService.state
            .onEach { sttState ->
                updateState { copy(voiceState = sttState) }
                when (sttState) {
                    is SpeechRecognitionState.Result ->
                        updateState { copy(question = sttState.text, error = null) }
                    is SpeechRecognitionState.Partial ->
                        updateState { copy(question = sttState.text) }
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionService.destroy()
    }

    override fun handleIntent(intent: AssistantIntent) {
        when (intent) {
            is AssistantIntent.UpdateQuestion -> updateState {
                copy(question = intent.question, error = null, activeTemplate = null)
            }

            is AssistantIntent.SelectIndustry -> updateState {
                copy(
                    selectedIndustry = intent.industry,
                    availableScenarios = intent.industry
                        ?.let { demoScenarioCatalog.scenariosForIndustry(it.toPromptIndustry()) }
                        ?: emptyList(),
                    error = null,
                )
            }

            is AssistantIntent.SelectScenario -> updateState {
                copy(
                    question = intent.scenario.prompt,
                    selectedIndustry = intent.scenario.industry.toAssistantIndustry(),
                    availableScenarios = demoScenarioCatalog.scenariosForIndustry(intent.scenario.industry),
                    activeTemplate = null,
                    error = null,
                )
            }

            is AssistantIntent.SelectTemplate -> updateState {
                copy(
                    question = intent.template.promptText,
                    activeTemplate = intent.template,
                    error = null,
                )
            }

            is AssistantIntent.SelectSessionModel -> {
                val model = modelCatalog.modelById(intent.modelId) ?: return
                updateState { copy(sessionModel = model) }
            }

            is AssistantIntent.PdfAttached -> attachPdf(intent.bytes, intent.fileName)
            is AssistantIntent.ImageAttached -> attachImage(intent.bytes)
            is AssistantIntent.EmailFileAttached -> attachEmailFile(intent.bytes, intent.fileName)
            AssistantIntent.AttachGmail -> attachGmail()
            AssistantIntent.RemovePdf -> updateState { copy(attachedPdf = null, error = null) }
            AssistantIntent.RemoveImage -> updateState { copy(attachedImage = null, error = null) }
            AssistantIntent.RemoveEmail -> updateState { copy(attachedEmail = null, error = null) }
            AssistantIntent.RefreshGmailState -> updateState {
                copy(
                    gmailSupported = GmailIntegration.controller?.isSupported == true,
                    gmailConnected = GmailIntegration.controller?.isConnected() == true,
                )
            }

            AssistantIntent.ToggleVoiceInput -> {
                if (state.value.isListening) speechRecognitionService.stopListening()
                else speechRecognitionService.startListening()
            }

            is AssistantIntent.RemoveMessage -> updateState {
                copy(messages = messages.filter { it.id != intent.bubbleId })
            }

            is AssistantIntent.LoadConversation -> loadConversation(intent.id)

            is AssistantIntent.ShareBubble -> shareBubble(intent.bubbleId, intent.format)

            AssistantIntent.AskQuestion -> askQuestion()

            AssistantIntent.CancelGeneration -> {
                generationJob?.cancel()
                generationJob = null
                updateState { copy(isGenerating = false, streamingText = "") }
            }

            AssistantIntent.ClearConversation -> {
                generationJob?.cancel()
                generationJob = null
                speechRecognitionService.stopListening()
                updateState {
                    copy(
                        question = "",
                        messages = emptyList(),
                        streamingText = "",
                        isGenerating = false,
                        metrics = AssistantMetrics(),
                        activeTemplate = null,
                        attachedPdf = null,
                        attachedImage = null,
                        attachedEmail = null,
                        error = null,
                    )
                }
            }
        }
    }

    private fun attachPdf(bytes: ByteArray, fileName: String) {
        (generationScope ?: viewModelScope).launch {
            updateState {
                copy(attachedPdf = AttachedPdf(fileName, 0, "", AttachmentStatus.Indexing), error = null)
            }
            runCatching {
                val pdf = withContext(Dispatchers.Default) { pdfExtractor.extract(bytes, fileName) }
                if (pdf.rawText.isBlank()) error("Could not extract text — the PDF may be image-based.")
                val chunks = withContext(Dispatchers.Default) { chunker.indexPdf(pdf) }
                val context = withContext(Dispatchers.Default) {
                    contextRetriever.retrieve(
                        question = state.value.question.ifBlank { "Summarize this document." },
                        chunks = chunks,
                        maxChunks = 6,
                    )
                }
                AttachedPdf(pdf.fileName, pdf.pageCount, context.text, AttachmentStatus.Ready, chunks)
            }.onSuccess { attached ->
                updateState { copy(attachedPdf = attached) }
            }.onFailure { e ->
                updateState { copy(attachedPdf = null, error = "PDF error: ${e.message}") }
            }
        }
    }

    private fun attachImage(bytes: ByteArray) {
        (generationScope ?: viewModelScope).launch {
            updateState {
                copy(attachedImage = AttachedImage(bytes, null, AttachmentStatus.Indexing), error = null)
            }
            runCatching {
                withContext(Dispatchers.Default) { imageClassifier.classify(bytes) }
            }.onSuccess { classification ->
                updateState {
                    copy(attachedImage = AttachedImage(bytes, classification, AttachmentStatus.Ready))
                }
            }.onFailure { e ->
                updateState { copy(attachedImage = null, error = "Image analysis failed: ${e.message}") }
            }
        }
    }

    private fun attachEmailFile(bytes: ByteArray, fileName: String) {
        (generationScope ?: viewModelScope).launch {
            updateState { copy(attachedEmail = AttachedEmail(fileName, 0, AttachmentStatus.Indexing), error = null) }
            runCatching {
                val email = withContext(Dispatchers.Default) {
                    if (fileName.endsWith(".mbox", ignoreCase = true)) emailExtractor.extractMbox(bytes, fileName)
                    else emailExtractor.extractEml(bytes, fileName)
                }
                if (email.rawText.isBlank()) error("Could not extract text from this email file.")
                val chunks = withContext(Dispatchers.Default) {
                    chunker.indexText(email.rawText, email.fileName)
                }
                AttachedEmail(email.fileName, email.emailCount, AttachmentStatus.Ready, chunks)
            }.onSuccess { attached ->
                updateState { copy(attachedEmail = attached) }
            }.onFailure { e ->
                updateState { copy(attachedEmail = null, error = "Email error: ${e.message}") }
            }
        }
    }

    private fun attachGmail() {
        val controller = GmailIntegration.controller
        val source = gmailSource
        if (controller == null || source == null) return
        // First tap connects; the user completes consent then taps Gmail again to fetch.
        if (!controller.isConnected()) {
            controller.connect()
            updateState { copy(gmailConnected = controller.isConnected()) }
            return
        }
        (generationScope ?: viewModelScope).launch {
            updateState {
                copy(attachedEmail = AttachedEmail("Gmail", 0, AttachmentStatus.Indexing), isFetchingEmail = true, error = null)
            }
            runCatching {
                val doc = withContext(Dispatchers.Default) {
                    source.fetchAsSingleDocument(query = null, maxResults = 20, label = "Gmail")
                }
                if (doc.rawText.isBlank()) error("No Gmail messages found.")
                val chunks = withContext(Dispatchers.Default) {
                    chunker.indexText(doc.rawText, doc.fileName)
                }
                AttachedEmail(doc.fileName, doc.emailCount, AttachmentStatus.Ready, chunks)
            }.onSuccess { attached ->
                updateState { copy(attachedEmail = attached, isFetchingEmail = false, gmailConnected = true) }
            }.onFailure { e ->
                updateState { copy(attachedEmail = null, isFetchingEmail = false, error = "Gmail error: ${e.message}") }
            }
        }
    }

    private fun saveConversation(question: String, result: GenerationResult) {
        val snap = state.value
        val conversation = ChatConversation(
            id = result.generatedAt.replace(Regex("[^0-9]"), ""),
            title = question.take(60).ifBlank { "Conversation" },
            modelName = result.model.label,
            backendName = result.backend.label,
            industry = snap.selectedIndustry?.label ?: "General",
            messages = listOf(
                ChatMessage(role = "user", content = question, timestamp = result.generatedAt),
                ChatMessage(role = "assistant", content = result.text, timestamp = result.generatedAt),
            ),
            createdAt = result.generatedAt,
        )
        conversationRepository.save(conversation)
    }

    private fun shareBubble(bubbleId: String, format: com.kevin.astra.domain.export.ExportFormat) {
        val bubble = state.value.messages.find { it.id == bubbleId } ?: return
        val snap = state.value
        val now = Clock.System.now().toEpochMilliseconds().toString()
        val conversation = ChatConversation(
            id = "share_$now",
            title = bubble.text.take(60).ifBlank { "ASTRA Response" },
            modelName = bubble.metrics?.model ?: snap.sessionModel?.displayName ?: "ASTRA",
            backendName = bubble.metrics?.backend ?: "—",
            industry = snap.selectedIndustry?.label ?: "General",
            messages = listOf(ChatMessage(role = "assistant", content = bubble.text, timestamp = now)),
            createdAt = now,
        )
        shareHelper.share(conversation, format)
    }

    private fun loadConversation(id: String) {
        val conversation = conversationRepository.getById(id) ?: return
        val bubbles = conversation.messages.mapIndexed { index, msg ->
            ChatBubble(
                id = "${msg.role}_${index}_${msg.timestamp}",
                role = if (msg.role == "user") ChatRole.User else ChatRole.Assistant,
                text = msg.content,
            )
        }
        updateState {
            copy(
                messages = bubbles,
                question = "",
                streamingText = "",
                isGenerating = false,
                error = null,
                attachedPdf = null,
                attachedImage = null,
            )
        }
    }

    private fun askQuestion() {
        val snapshot = state.value
        if (snapshot.isGenerating) return
        if (snapshot.question.isBlank()) {
            updateState { copy(error = "Enter a question before asking ASTRA.") }
            return
        }

        val userMessage = ChatBubble(
            id = "user_${Clock.System.now().toEpochMilliseconds()}",
            role = ChatRole.User,
            text = buildString {
                append(snapshot.question)
                if (snapshot.attachedPdf != null) append(" [📄 ${snapshot.attachedPdf.fileName}]")
                if (snapshot.attachedImage != null) append(" [📷 Image]")
                if (snapshot.attachedEmail != null) append(" [📧 ${snapshot.attachedEmail.label}]")
            },
        )

        updateState {
            copy(
                messages = messages + userMessage,
                question = "",
                isGenerating = true,
                streamingText = "",
                activeTemplate = null,
                error = null,
                installedModels = modelCatalog.installedModels(),
            )
        }

        generationJob?.cancel()
        generationJob = (generationScope ?: viewModelScope).launch {
            val configuration = aiConfigurationRepository.getConfiguration()
            val selectedModel = snapshot.sessionModel
                ?: modelCatalog.modelById(configuration.selectedModelId)
            val selectedBackend = backendCatalog.backendById(configuration.selectedBackendId)

            if (selectedModel == null || selectedBackend == null) {
                updateState { copy(isGenerating = false, error = "Invalid AI configuration. Check Settings.") }
                return@launch
            }

            val industry = snapshot.selectedIndustry?.toPromptIndustry()

            // Inject prior turns so the AI can continue the conversation
            val enrichedQuestion = buildString {
                val priorMessages = snapshot.messages.takeLast(10)
                if (priorMessages.isNotEmpty()) {
                    appendLine("[Conversation history]")
                    priorMessages.forEach { bubble ->
                        val role = if (bubble.role == ChatRole.User) "User" else "ASTRA"
                        appendLine("$role: ${bubble.text.take(500)}")
                    }
                    appendLine()
                    appendLine("[New question]")
                }
                append(snapshot.question)
                snapshot.attachedImage?.classification?.let { cls ->
                    append("\n\n[Image context: ${cls.toPromptDescription()}]")
                }
            }

            // Re-rank all attached sources (PDF + email/Gmail) against the final question, so chat
            // answers from the most relevant passages instead of stale placeholder-ranked context.
            val attachedChunks = snapshot.attachedPdf?.chunks.orEmpty() + snapshot.attachedEmail?.chunks.orEmpty()
            val documentContext = when {
                attachedChunks.isNotEmpty() -> withContext(Dispatchers.Default) {
                    contextRetriever.retrieve(
                        question = snapshot.question,
                        chunks = attachedChunks,
                        maxChunks = 6,
                    ).text
                }
                else -> snapshot.attachedPdf?.extractedContext
            }

            val preparedParts = promptPipeline.preparePrompt(
                PromptBuildRequest(
                    engineerQuestion = enrichedQuestion,
                    selectedIndustry = industry,
                    selectedModel = selectedModel,
                    extractedDocumentContext = documentContext,
                ),
            )

            val promptRequest = PromptRequest(
                prompt = preparedParts.fullPrompt,
                systemPrompt = preparedParts.systemPrompt,
                userMessage = preparedParts.userMessage,
                industry = industry,
                model = selectedModel.runtimeModel,
                backend = selectedBackend.runtimeBackend,
                maxTokens = configuration.maxTokens,
                temperature = configuration.temperature,
            )

            var finalResult: com.kevin.astra.core.ai.GenerationResult? = null
            askLocalAssistant.stream(promptRequest)
                .flowOn(Dispatchers.Default)
                .collect { event ->
                    when (event) {
                        is StreamEvent.Token -> updateState { copy(streamingText = streamingText + event.text) }
                        is StreamEvent.Complete -> finalResult = event.result
                        is StreamEvent.Error -> updateState {
                            copy(isGenerating = false, error = event.message, streamingText = "")
                        }
                    }
                }

            val result = finalResult ?: return@launch
            val assistantMetrics = result.toAssistantMetrics()
            val assistantMessage = ChatBubble(
                id = "assistant_${result.generatedAt}",
                role = ChatRole.Assistant,
                text = result.text,
                metrics = assistantMetrics,
            )

            updateState {
                copy(
                    isGenerating = false,
                    streamingText = "",
                    messages = messages + assistantMessage,
                    metrics = assistantMetrics,
                    attachedPdf = null,
                    attachedImage = null,
                    attachedEmail = null,
                )
            }

            saveConversation(snapshot.question, result)
            updateState { copy(recentHistory = conversationRepository.getAll().takeLast(20).reversed()) }
            notificationService.showNotification(
                title = "AI Analysis Ready",
                message = "ASTRA has completed the analysis.",
                targetDestination = AstraDestination.Assistant,
            )
        }
    }
}

private fun GenerationResult.toAssistantMetrics(): AssistantMetrics =
    AssistantMetrics(
        model = model.label,
        backend = backend.label,
        runtimeMode = runtimeInfo.mode.label,
        latency = "${metrics.latencyMillis / 1_000.0} s",
        tokensPerSecond = if (metrics.tokensPerSecond > 0) metrics.tokensPerSecond.toString() else "N/A",
        timeToFirstToken = "${metrics.timeToFirstTokenMillis} ms",
        tokensGenerated = metrics.tokensGenerated.toString(),
        memoryUsage = "${metrics.memoryUsageMb} MB",
        modelLoadTime = "${runtimeInfo.modelLoadTimeMillis} ms",
        totalExecutionTime = "${runtimeInfo.totalExecutionTimeMillis / 1_000.0} s",
        fallbackReason = runtimeInfo.fallbackReason,
    )
