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
import com.kevin.astra.domain.documents.PdfExtractor
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsViewModel(
    private val pdfExtractor: PdfExtractor,
    private val chunker: SmartTextChunker,
    private val contextRetriever: DocumentContextRetriever,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val notificationService: NotificationService,
    private val workScope: CoroutineScope? = null,
) : AstraViewModel<DocumentsState, DocumentsIntent, DocumentsEffect>(
    initialState = DocumentsState(
        availableModels = modelCatalog.installedModels(),
        sessionModel = modelCatalog.currentModel(),
    ),
) {
    override fun handleIntent(intent: DocumentsIntent) {
        when (intent) {
            is DocumentsIntent.PdfLoaded -> loadPdf(intent.bytes, intent.fileName)
            DocumentsIntent.IndexDocument -> indexDocument()
            is DocumentsIntent.UpdateQuestion -> updateState { copy(question = intent.question, error = null) }
            DocumentsIntent.AskDocument -> askDocument()
            DocumentsIntent.ClearDocument -> updateState {
                DocumentsState(availableModels = availableModels, sessionModel = sessionModel)
            }
            DocumentsIntent.ClearAnswer -> updateState { copy(answer = null, extractedContext = null, metrics = DocumentsMetrics(), error = null) }
            is DocumentsIntent.SelectSessionModel -> {
                val model = modelCatalog.modelById(intent.modelId) ?: return
                updateState { copy(sessionModel = model) }
            }
        }
    }

    private fun loadPdf(bytes: ByteArray, fileName: String) {
        (workScope ?: viewModelScope).launch {
            updateState { copy(isLoading = true, error = null, documentStatus = DocumentStatus.NotIndexed, indexedChunks = emptyList()) }
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
        } catch (e: Exception) {
            updateState { copy(isIndexing = false, documentStatus = DocumentStatus.Error, error = "Indexing failed: ${e.message}") }
        }
    }

    private fun askDocument() {
        val snapshot = state.value
        if (!snapshot.canAsk) return

        (workScope ?: viewModelScope).launch {
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

            val preparedPrompt = promptPipeline.preparePrompt(
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
                        prompt = preparedPrompt,
                        industry = configuration.selectedIndustry,
                        model = selectedModel.runtimeModel,
                        backend = selectedBackend.runtimeBackend,
                        maxTokens = configuration.maxTokens,
                        temperature = configuration.temperature,
                    ),
                )
            }

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
