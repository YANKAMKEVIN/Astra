package com.kevin.astra.presentation.documents

import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.RetrievedDocumentContext

data class DocumentsMetrics(
    val model: String = "—",
    val backend: String = "—",
    val latency: String = "—",
    val tokensPerSecond: String = "—",
)

data class DocumentsAnswer(
    val title: String,
    val body: String,
)

enum class DocumentSourceType { Pdf, Email }

data class DocumentsState(
    val availableModels: List<LocalModel> = emptyList(),
    val sessionModel: LocalModel? = null,
    val loadedFileName: String? = null,
    val pageCount: Int = 0,
    val emailCount: Int = 0,
    val sourceType: DocumentSourceType = DocumentSourceType.Pdf,
    val documentStatus: DocumentStatus = DocumentStatus.NotIndexed,
    val indexedChunks: List<IndexedDocumentChunk> = emptyList(),
    val documentSummary: String? = null,
    val isSummarizing: Boolean = false,
    val question: String = "",
    val extractedContext: RetrievedDocumentContext? = null,
    val answer: DocumentsAnswer? = null,
    val isLoading: Boolean = false,
    val isIndexing: Boolean = false,
    val isGenerating: Boolean = false,
    val metrics: DocumentsMetrics = DocumentsMetrics(),
    val error: String? = null,
) : AstraState {
    val canIndex: Boolean
        get() = loadedFileName != null && !isIndexing && !isLoading &&
            documentStatus == DocumentStatus.NotIndexed

    val canAsk: Boolean
        get() = question.isNotBlank() &&
            documentStatus == DocumentStatus.Indexed &&
            !isGenerating && !isIndexing && !isSummarizing
}

sealed interface DocumentsIntent : AstraIntent {
    data class PdfLoaded(val bytes: ByteArray, val fileName: String) : DocumentsIntent
    data class EmailLoaded(val bytes: ByteArray, val fileName: String) : DocumentsIntent
    data object IndexDocument : DocumentsIntent
    data class UpdateQuestion(val question: String) : DocumentsIntent
    data object AskDocument : DocumentsIntent
    data object ClearDocument : DocumentsIntent
    data object ClearAnswer : DocumentsIntent
    data class SelectSessionModel(val modelId: String) : DocumentsIntent
}

sealed interface DocumentsEffect : AstraEffect
