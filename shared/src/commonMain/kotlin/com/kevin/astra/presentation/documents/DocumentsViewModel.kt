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
import com.kevin.astra.data.documents.DocumentIndexStore
import com.kevin.astra.data.documents.NoOpDocumentIndexFile
import com.kevin.astra.data.documents.SmartTextChunker
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.DocumentLibrary
import com.kevin.astra.domain.documents.DocumentQuestionRouter
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.FetchGmailUseCase
import com.kevin.astra.domain.documents.IndexEmailFileUseCase
import com.kevin.astra.domain.documents.IndexedSource
import com.kevin.astra.domain.documents.PdfExtractor
import com.kevin.astra.domain.gmail.GmailApiException
import com.kevin.astra.domain.gmail.GmailIntegration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsViewModel(
    private val pdfExtractor: PdfExtractor,
    private val indexEmailFile: IndexEmailFileUseCase,
    private val fetchGmailUseCase: FetchGmailUseCase,
    private val chunker: SmartTextChunker,
    private val contextRetriever: DocumentContextRetriever,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val notificationService: NotificationService,
    private val documentIndexStore: DocumentIndexStore = DocumentIndexStore(NoOpDocumentIndexFile),
    private val workScope: CoroutineScope? = null,
) : AstraViewModel<DocumentsState, DocumentsIntent, DocumentsEffect>(
    initialState = DocumentsState(
        availableModels = modelCatalog.installedModels(),
        activeModel = modelCatalog.currentModel(),
        gmailSupported = GmailIntegration.controller?.isSupported == true,
        gmailConnected = GmailIntegration.controller?.isConnected() == true,
    ),
) {
    private var generationJob: Job? = null
    private val persistDispatcher = Dispatchers.Default.limitedParallelism(1)

    init {
        restoreLibrary()
    }

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
                updateState {
                    DocumentsState(
                        availableModels = availableModels,
                        sessionModel = sessionModel,
                        activeModel = activeModel,
                        gmailSupported = gmailSupported,
                        gmailConnected = gmailConnected,
                    )
                }
                persistLibrary()
            }
            is DocumentsIntent.RemoveSource -> removeSources { it.id == intent.sourceId }
            DocumentsIntent.ClearAnswer -> updateState { copy(answer = null, extractedContext = null, metrics = DocumentsMetrics(), error = null, documentSummary = null) }
            // The installed list and the app-wide model change from the Models screen; a snapshot
            // taken at startup kept answering with the launch-time model (often Mock).
            DocumentsIntent.RefreshModels -> updateState {
                copy(availableModels = modelCatalog.installedModels(), activeModel = modelCatalog.currentModel())
            }
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
                // Disconnecting also forgets the mail fetched from Gmail, including the saved copy.
                removeSources { it.isGmail }
            }
            is DocumentsIntent.UpdateGmailQuery -> updateState { copy(gmailQuery = intent.query, error = null) }
            DocumentsIntent.FetchGmailRecent -> fetchGmail(query = null)
            DocumentsIntent.FetchGmailSearch -> fetchGmail(query = state.value.gmailQuery.ifBlank { null })
            DocumentsIntent.FetchGmailMore -> state.value.gmailFetch?.takeIf { it.hasMore }?.let { last ->
                fetchGmail(query = last.query, count = last.count + FetchGmailUseCase.PAGE_SIZE)
            }
        }
    }

    private fun fetchGmail(query: String?, count: Int = FetchGmailUseCase.PAGE_SIZE) {
        if (!fetchGmailUseCase.isAvailable) return
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
                    gmailFetch = null,
                    currentSourceId = null,
                )
            }
            try {
                val indexed = withContext(Dispatchers.Default) { fetchGmailUseCase(query = query, maxResults = count) }
                updateState {
                    copy(
                        isFetchingGmail = false,
                        loadedFileName = indexed.label,
                        emailCount = indexed.emailCount,
                        indexedChunks = indexed.chunks,
                        documentStatus = DocumentStatus.Indexed,
                        gmailFetch = GmailFetch(query = query, count = count, hasMore = indexed.hasMore),
                    )
                }
                val source = IndexedSource(
                    id = IndexedSource.gmailId(query),
                    name = indexed.label,
                    isEmail = true,
                    emailCount = indexed.emailCount,
                    chunks = indexed.chunks,
                )
                addToLibrary(source)
                generateSummaryInternal(source.id, indexed.chunks)
            } catch (e: GmailApiException) {
                // 401/403: the stored authorization is useless; drop it so "Connect Gmail" shows again.
                if (e.needsReconnect) GmailIntegration.controller?.disconnect()
                updateState {
                    copy(
                        isFetchingGmail = false,
                        error = e.message,
                        gmailConnected = GmailIntegration.controller?.isConnected() == true,
                    )
                }
            } catch (e: Exception) {
                updateState { copy(isFetchingGmail = false, error = e.message ?: "Failed to fetch Gmail.") }
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
                    gmailFetch = null,
                    currentSourceId = null,
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
                    gmailFetch = null,
                    currentSourceId = null,
                    documentSummary = null,
                    emailCount = 0,
                    pageCount = 0,
                    question = "",
                    sourceType = DocumentSourceType.Email,
                    documentStatus = DocumentStatus.NotIndexed,
                    indexedChunks = emptyList(),
                    answer = null,
                    extractedContext = null,
                    metrics = DocumentsMetrics(),
                )
            }
            try {
                val indexed = withContext(Dispatchers.Default) { indexEmailFile(bytes, fileName) }
                updateState {
                    copy(
                        isLoading = false,
                        loadedFileName = indexed.label,
                        emailCount = indexed.emailCount,
                        indexedChunks = indexed.chunks,
                        documentStatus = DocumentStatus.Indexed,
                    )
                }
                val source = IndexedSource(
                    id = IndexedSource.emailId(fileName),
                    name = indexed.label,
                    isEmail = true,
                    emailCount = indexed.emailCount,
                    chunks = indexed.chunks,
                )
                addToLibrary(source)
                generateSummaryInternal(source.id, indexed.chunks)
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = e.message ?: "Failed to read email file.") }
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
            val source = IndexedSource(
                id = IndexedSource.pdfId(fileName),
                name = pdf.fileName,
                isEmail = false,
                pageCount = pdf.pageCount,
                chunks = chunks,
            )
            addToLibrary(source)
            generateSummaryInternal(source.id, chunks)
        } catch (e: Exception) {
            updateState { copy(isIndexing = false, documentStatus = DocumentStatus.Error, error = "Indexing failed: ${e.message}") }
        }
    }

    private suspend fun generateSummaryInternal(
        sourceId: String,
        chunks: List<com.kevin.astra.domain.documents.IndexedDocumentChunk>,
    ) {
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
                        maxTokens = 512,
                        temperature = 0.3,
                        contextWindow = selectedModel.contextWindow,
                    ),
                )
            }
            updateState {
                copy(
                    isSummarizing = false,
                    // A newer document may have replaced this one meanwhile: keep its summary out.
                    documentSummary = if (currentSourceId == sourceId) result.text else documentSummary,
                    library = library.map { if (it.id == sourceId) it.copy(summary = result.text) else it },
                )
            }
            persistLibrary()
        } catch (e: Exception) {
            updateState { copy(isSummarizing = false) }
        }
    }

    /** Restores the saved library; the last source becomes the current document. */
    private fun restoreLibrary() {
        (workScope ?: viewModelScope).launch {
            val saved = withContext(Dispatchers.Default) { documentIndexStore.load() }
            if (saved.isEmpty()) return@launch
            updateState {
                // Something was loaded while the file was read: it wins, the rest is kept behind it.
                if (library.isNotEmpty() || loadedFileName != null) {
                    copy(library = saved.filterNot { s -> library.any { it.id == s.id } } + library)
                } else {
                    showing(saved.last()).copy(library = saved)
                }
            }
        }
    }

    private fun addToLibrary(source: IndexedSource) {
        updateState { copy(library = DocumentLibrary.add(library, source), currentSourceId = source.id) }
        persistLibrary()
    }

    private fun removeSources(predicate: (IndexedSource) -> Boolean) {
        val before = state.value
        if (before.library.none(predicate)) return
        updateState {
            val remaining = library.filterNot(predicate)
            val currentRemoved = library.any { it.id == currentSourceId && predicate(it) }
            when {
                !currentRemoved -> copy(library = remaining)
                remaining.isEmpty() -> DocumentsState(
                    availableModels = availableModels,
                    sessionModel = sessionModel,
                    activeModel = activeModel,
                    gmailSupported = gmailSupported,
                    gmailConnected = gmailConnected,
                    gmailQuery = gmailQuery,
                )
                else -> showing(remaining.last()).copy(library = remaining)
            }
        }
        persistLibrary()
    }

    private fun persistLibrary() {
        val library = state.value.library
        // One writer at a time, in order: an older snapshot never lands after a newer one.
        (workScope ?: viewModelScope).launch(persistDispatcher) { documentIndexStore.save(library) }
    }

    private fun askDocument() {
        val snapshot = state.value
        if (!snapshot.canAsk) return

        // Question about the whole document → return the pre-generated summary directly. With
        // several documents loaded, one document's summary is not the answer: search them all.
        if (snapshot.library.size <= 1 &&
            DocumentQuestionRouter.isWholeDocumentQuestion(snapshot.question) &&
            snapshot.documentSummary != null
        ) {
            updateState {
                copy(
                    answer = DocumentsAnswer(title = "Document Summary", body = snapshot.documentSummary),
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
                    chunks = snapshot.library.takeIf { it.isNotEmpty() }
                        ?.let(DocumentLibrary::allChunks)
                        ?: snapshot.indexedChunks,
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
                        contextWindow = minOf(configuration.contextWindow, selectedModel.contextWindow),
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
                message = if (snapshot.library.size > 1) {
                    "ASTRA answered your question from ${snapshot.library.size} documents."
                } else {
                    "ASTRA answered your question from ${snapshot.loadedFileName}."
                },
                targetDestination = AstraDestination.Documents,
            )
        }
    }
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

/** This state with [source] shown as the current document (answer and question cleared). */
private fun DocumentsState.showing(source: IndexedSource): DocumentsState = copy(
    currentSourceId = source.id,
    loadedFileName = source.name,
    sourceType = if (source.isEmail) DocumentSourceType.Email else DocumentSourceType.Pdf,
    pageCount = source.pageCount,
    emailCount = source.emailCount,
    indexedChunks = source.chunks,
    documentStatus = DocumentStatus.Indexed,
    documentSummary = source.summary,
    gmailFetch = null,
    answer = null,
    extractedContext = null,
    question = "",
    error = null,
)
