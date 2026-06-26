package com.kevin.astra.presentation.documents

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.data.documents.EmbeddedMaintenanceDocument
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.DocumentIndexer
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DocumentsViewModel(
    private val documentIndexer: DocumentIndexer,
    private val contextRetriever: DocumentContextRetriever,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val documentsScope: CoroutineScope? = null,
) : AstraViewModel<DocumentsState, DocumentsIntent, DocumentsEffect>(
    initialState = DocumentsState(
        availableDocuments = listOf(EmbeddedMaintenanceDocument.industrialPumpMaintenanceGuide),
        selectedDocument = EmbeddedMaintenanceDocument.industrialPumpMaintenanceGuide,
    ),
) {
    override fun handleIntent(intent: DocumentsIntent) {
        when (intent) {
            DocumentsIntent.IndexSelectedDocument -> indexSelectedDocument()

            is DocumentsIntent.UpdateQuestion -> updateState {
                copy(question = intent.question, error = null)
            }

            DocumentsIntent.AskDocument -> askDocument()
            DocumentsIntent.ClearConversation -> updateState {
                copy(
                    question = "",
                    extractedContext = null,
                    answer = null,
                    error = null,
                )
            }
        }
    }

    private fun indexSelectedDocument() {
        val document = state.value.selectedDocument ?: return
        if (!state.value.canIndex) return

        (documentsScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isIndexing = true,
                    documentStatus = DocumentStatus.Processing,
                    error = null,
                )
            }

            val chunks = documentIndexer.index(document)

            updateState {
                copy(
                    isIndexing = false,
                    documentStatus = DocumentStatus.Indexed,
                    indexedChunks = chunks,
                )
            }
        }
    }

    private fun askDocument() {
        val snapshot = state.value
        if (!snapshot.canAsk) {
            updateState {
                copy(error = "Index the embedded document and enter a question before asking ASTRA.")
            }
            return
        }

        (documentsScope ?: viewModelScope).launch {
            val configuration = aiConfigurationRepository.currentConfiguration.value
            val selectedModel = modelCatalog.currentModel()
            val context = contextRetriever.retrieve(
                question = snapshot.question,
                chunks = snapshot.indexedChunks,
            )
            val prompt = buildDocumentPrompt(
                question = snapshot.question,
                context = context.text,
            )

            updateState {
                copy(
                    isGenerating = true,
                    extractedContext = context,
                    answer = null,
                    error = null,
                )
            }

            val result = askLocalAssistant(
                PromptRequest(
                    prompt = prompt,
                    industry = configuration.selectedIndustry,
                    model = selectedModel.runtimeModel,
                    backend = configuration.selectedBackend,
                    maxTokens = configuration.maxTokens,
                    temperature = configuration.temperature,
                ),
            )

            updateState {
                copy(
                    isGenerating = false,
                    answer = result.toDocumentsAnswer(),
                    metrics = result.toDocumentsMetrics(),
                )
            }
        }
    }
}

private fun buildDocumentPrompt(
    question: String,
    context: String,
): String =
    """
        Use the following local maintenance guide context to answer the engineer's question.

        Extracted context:
        $context

        Engineer question:
        $question
    """.trimIndent()

private fun GenerationResult.toDocumentsAnswer(): DocumentsAnswer =
    DocumentsAnswer(
        title = text.lineSequence().firstOrNull().orEmpty().ifBlank { "Document answer" },
        body = text,
    )

private fun GenerationResult.toDocumentsMetrics(): DocumentsMetrics =
    DocumentsMetrics(
        model = model.label,
        backend = backend.label,
        latency = "${metrics.latencyMillis / 1_000.0} s",
        tokensPerSecond = metrics.tokensPerSecond.toString(),
    )
