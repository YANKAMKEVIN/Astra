package com.kevin.astra.presentation.documents

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
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.EmailExtractor
import com.kevin.astra.domain.documents.PdfExtractor
import com.kevin.astra.domain.gmail.GmailIntegration
import com.kevin.astra.domain.gmail.GmailMessageSource
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsViewModel(
    private val pdfExtractor: PdfExtractor,
    private val emailExtractor: EmailExtractor,
    private val chunker: SmartTextChunker,
    private val contextRetriever: DocumentContextRetriever,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val notificationService: NotificationService,
    private val gmailSource: GmailMessageSource? = null,
    private val workScope: CoroutineScope? = null,
) : AstraViewModel<DocumentsState, DocumentsIntent, DocumentsEffect>(
    initialState = DocumentsState(
        availableModels = modelCatalog.installedModels(),
        sessionModel = modelCatalog.currentModel(),
        gmailSupported = GmailIntegration.controller?.isSupported == true,
        gmailConnected = GmailIntegration.controller?.isConnected() == true,
    ),
) {
    private var generationJob: Job? = null

    override fun handleIntent(intent: DocumentsIntent) {
        when (intent) {
            is DocumentsIntent.PdfLoaded -> loadPdf(intent.bytes, intent.fileName)
            is DocumentsIntent.EmailLoaded -> loadEmail(intent.bytes, intent.fileName)
            DocumentsIntent.IndexDocument -> indexDocument()
            is DocumentsIntent.UpdateQuestion -> updateState { copy(question = intent.question, error = null) }
            DocumentsIntent.AskDocument -> askDocument()
            DocumentsIntent.ClearDocument -> {
                generationJob?.cancel()
                generationJob = null
                updateState { DocumentsState(availableModels = availableModels, sessionModel = sessionModel) }
            }
            DocumentsIntent.ClearAnswer -> updateState { copy(answer = null, extractedContext = null, metrics = DocumentsMetrics(), error = null, documentSummary = null) }
            is DocumentsIntent.SelectSessionModel -> {
                val model = modelCatalog.modelById(intent.modelId) ?: return
                updateState { copy(sessionModel = model) }
            }
            DocumentsIntent.RefreshGmailState -> updateState {
                copy(
                    gmailSupported = GmailIntegration.controller?.isSupported == true,
                    gmailConnected = GmailIntegration.controller?.isConnected() == true,
                )
            }
            DocumentsIntent.ConnectGmail -> GmailIntegration.controller?.connect()
            DocumentsIntent.DisconnectGmail -> {
                GmailIntegration.controller?.disconnect()
                updateState { copy(gmailConnected = GmailIntegration.controller?.isConnected() == true) }
            }
            is DocumentsIntent.UpdateGmailQuery -> updateState { copy(gmailQuery = intent.query, error = null) }
            DocumentsIntent.FetchGmailRecent -> fetchGmail(query = null)
            DocumentsIntent.FetchGmailSearch -> fetchGmail(query = state.value.gmailQuery.ifBlank { null })
        }
    }

    private fun fetchGmail(query: String?) {
        val source = gmailSource ?: return
        generationJob?.cancel()
        generationJob = null
        (workScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isFetchingGmail = true,
                    error = null,
                    loadedFileName = null,
                    emailCount = 0,
                    pageCount = 0,
                    question = "",
                    sourceType = DocumentSourceType.Email,
                    documentStatus = DocumentStatus.NotIndexed,
                    indexedChunks = emptyList(),
                    answer = null,
                    extractedContext = null,
                    documentSummary = null,
                    metrics = DocumentsMetrics(),
                )
            }
            try {
                val doc = withContext(Dispatchers.Default) {
                    source.fetchAsSingleDocument(query = query, maxResults = 20, label = "Gmail")
                }
                if (doc.rawText.isBlank()) {
                    updateState { copy(isFetchingGmail = false, error = "No Gmail messages found for this request.") }
                    return@launch
                }
                updateState {
                    copy(
                        isFetchingGmail = false,
                        loadedFileName = doc.fileName,
                        emailCount = doc.emailCount,
                        documentStatus = DocumentStatus.NotIndexed,
                    )
                }
                indexEmailInternal(doc.rawText, doc.fileName, doc.emailCount)
            } catch (e: Exception) {
                updateState { copy(isFetchingGmail = false, error = "Failed to fetch Gmail: ${e.message}") }
            }
        }
    }

    private fun loadPdf(bytes: ByteArray, fileName: String) {
        generationJob?.cancel()
        generationJob = null
        (workScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isLoading = true,
                    error = null,
                    loadedFileName = null,
                    pageCount = 0,
                    question = "",
                    documentStatus = DocumentStatus.NotIndexed,
                    indexedChunks = emptyList(),
                    answer = null,
                    extractedContext = null,
                    metrics = DocumentsMetrics(),
                )
            }
            try {
                val pdf = withContext(Dispatchers.Default) { pdfExtractor.extract(bytes, fileName) }
                if (pdf.rawText.isBlank()) {
                    updateState { copy(isLoading = false, error = "Could not extract text from this PDF. It may be image-based.") }
                    return@launch
                }
                updateState {
                    copy(
                        isLoading = false,
                        loadedFileName = pdf.fileName,
                        pageCount = pdf.pageCount,
                        documentStatus = DocumentStatus.NotIndexed,
                    )
                }
                // Auto-index after loading
                indexDocumentInternal(bytes, fileName)
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = "Failed to read PDF: ${e.message}") }
            }
        }
    }

    private fun loadEmail(bytes: ByteArray, fileName: String) {
        generationJob?.cancel()
        generationJob = null
        (workScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isLoading = true,
                    error = null,
                    loadedFileName = null,
                    emailCount = 0,
                    pageCount = 0,
                    question = "",
                    sourceType = DocumentSourceType.Email,
                    documentStatus = DocumentStatus.NotIndexed,
                    indexedChunks = emptyList(),
                    answer = null,
                    extractedContext = null,
                    documentSummary = null,
                    metrics = DocumentsMetrics(),
                )
            }
            try {
                val email = withContext(Dispatchers.Default) {
                    if (fileName.endsWith(".mbox", ignoreCase = true)) {
                        emailExtractor.extractMbox(bytes, fileName)
                    } else {
                        emailExtractor.extractEml(bytes, fileName)
                    }
                }
                if (email.rawText.isBlank()) {
                    updateState { copy(isLoading = false, error = "Could not extract text from this email file.") }
                    return@launch
                }
                updateState {
                    copy(
                        isLoading = false,
                        loadedFileName = email.fileName,
                        emailCount = email.emailCount,
                        documentStatus = DocumentStatus.NotIndexed,
                    )
                }
                indexEmailInternal(email.rawText, email.fileName, email.emailCount)
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = "Failed to read email file: ${e.message}") }
            }
        }
    }

    private fun indexEmailInternal(rawText: String, fileName: String, emailCount: Int) {
        (workScope ?: viewModelScope).launch {
            updateState { copy(isIndexing = true, error = null) }
            try {
                val chunks = withContext(Dispatchers.Default) {
                    chunker.indexPdf(com.kevin.astra.domain.documents.LoadedPdfDocument(fileName, rawText, emailCount))
                }
                updateState {
                    copy(
                        isIndexing = false,
                        indexedChunks = chunks,
                        documentStatus = DocumentStatus.Indexed,
                    )
                }
                generateSummaryInternal(chunks)
            } catch (e: Exception) {
                updateState { copy(isIndexing = false, error = "Indexing failed: ${e.message}") }
            }
        }
    }

    private fun indexDocument() {
        if (!state.value.canIndex) return
        (workScope ?: viewModelScope).launch { indexDocumentInternal(null, state.value.loadedFileName ?: return@launch) }
    }

    private suspend fun indexDocumentInternal(bytes: ByteArray?, fileName: String) {
        updateState { copy(isIndexing = true, documentStatus = DocumentStatus.Processing, error = null) }
        try {
            val pdf = if (bytes != null) {
                withContext(Dispatchers.Default) { pdfExtractor.extract(bytes, fileName) }
            } else return

            val chunks = withContext(Dispatchers.Default) { chunker.indexPdf(pdf) }
            updateState {
                copy(
                    isIndexing = false,
                    documentStatus = DocumentStatus.Indexed,
                    indexedChunks = chunks,
                )
            }
            generateSummaryInternal(chunks)
        } catch (e: Exception) {
            updateState { copy(isIndexing = false, documentStatus = DocumentStatus.Error, error = "Indexing failed: ${e.message}") }
        }
    }

    private suspend fun generateSummaryInternal(chunks: List<com.kevin.astra.domain.documents.IndexedDocumentChunk>) {
        if (chunks.isEmpty()) return
        updateState { copy(isSummarizing = true) }
        try {
            val configuration = aiConfigurationRepository.getConfiguration()
            val selectedModel = state.value.sessionModel
                ?: modelCatalog.modelById(configuration.selectedModelId)
                ?: modelCatalog.currentModel()
            val selectedBackend = backendCatalog.backendById(configuration.selectedBackendId)
                ?: backendCatalog.currentBackend()

            // Truncate to ~250 words to stay well within the 4096-token context window
            val contextText = chunks.take(2)
                .joinToString("\n\n") { chunk ->
                    chunk.content.split(Regex("\\s+")).take(250).joinToString(" ")
                }
            val systemPrompt = "You are a document summarization assistant. Be concise and accurate."
            val userMessage = "Summarize this document in 3-5 sentences, covering the main topic, purpose and key points:\n\n$contextText"

            val result = withContext(Dispatchers.Default) {
                askLocalAssistant(
                    PromptRequest(
                        prompt = userMessage,
                        systemPrompt = systemPrompt,
                        userMessage = userMessage,
                        model = selectedModel.runtimeModel,
                        backend = selectedBackend.runtimeBackend,
                        maxTokens = 4_096,
                        temperature = 0.3,
                    ),
                )
            }
            updateState { copy(isSummarizing = false, documentSummary = result.text) }
        } catch (e: Exception) {
            updateState { copy(isSummarizing = false) }
        }
    }

    private fun askDocument() {
        val snapshot = state.value
        if (!snapshot.canAsk) return

        // General question → return pre-generated summary directly
        if (isGeneralDocumentQuestion(snapshot.question) && snapshot.documentSummary != null) {
            updateState {
                copy(
                    answer = DocumentsAnswer(title = "Document Summary", body = snapshot.documentSummary!!),
                    extractedContext = null,
                    metrics = DocumentsMetrics(),
                )
            }
            return
        }

        generationJob = (workScope ?: viewModelScope).launch {
            val configuration = aiConfigurationRepository.getConfiguration()
            val selectedModel = snapshot.sessionModel
                ?: modelCatalog.modelById(configuration.selectedModelId)
                ?: modelCatalog.currentModel()
            val selectedBackend = backendCatalog.backendById(configuration.selectedBackendId) ?: backendCatalog.currentBackend()

            val context = withContext(Dispatchers.Default) {
                contextRetriever.retrieve(
                    question = snapshot.question,
                    chunks = snapshot.indexedChunks,
                    maxChunks = 4,
                )
            }

            val preparedParts = promptPipeline.preparePrompt(
                PromptBuildRequest(
                    engineerQuestion = snapshot.question,
                    selectedIndustry = configuration.selectedIndustry,
                    selectedModel = selectedModel,
                    extractedDocumentContext = context.text,
                ),
            )

            updateState { copy(isGenerating = true, extractedContext = context, answer = null, error = null) }

            val result = withContext(Dispatchers.Default) {
                askLocalAssistant(
                    PromptRequest(
                        prompt = preparedParts.fullPrompt,
                        systemPrompt = preparedParts.systemPrompt,
                        userMessage = preparedParts.userMessage,
                        industry = configuration.selectedIndustry,
                        model = selectedModel.runtimeModel,
                        backend = selectedBackend.runtimeBackend,
                        maxTokens = configuration.maxTokens,
                        temperature = configuration.temperature,
                    ),
                )
            }

            if (!isActive) return@launch

            updateState {
                copy(
                    isGenerating = false,
                    answer = result.toDocumentsAnswer(),
                    metrics = result.toDocumentsMetrics(),
                )
            }

            notificationService.showNotification(
                title = "Document Analysis Ready",
                message = "ASTRA answered your question from ${snapshot.loadedFileName}.",
                targetDestination = AstraDestination.Documents,
            )
        }
    }
}

private fun isGeneralDocumentQuestion(question: String): Boolean {
    val q = question.lowercase().trim()
    return listOf(
        "de quoi", "résumé", "resume", "summary", "summarize",
        "what is this", "what's this", "about this", "about the doc",
        "overview", "synthèse", "sujet", "c'est quoi",
        "qu'est-ce que", "explain this", "describe this",
        "what does this document", "quel est le sujet",
        "de quoi il parle", "ça parle de quoi",
    ).any { q.contains(it) }
}

private fun GenerationResult.toDocumentsAnswer(): DocumentsAnswer =
    DocumentsAnswer(
        title = text.lineSequence().firstOrNull().orEmpty().ifBlank { "Answer" },
        body = text,
    )

private fun GenerationResult.toDocumentsMetrics(): DocumentsMetrics =
    DocumentsMetrics(
        model = model.label,
        backend = backend.label,
        latency = "${metrics.latencyMillis / 1_000.0} s",
        tokensPerSecond = metrics.tokensPerSecond.toString(),
    )
