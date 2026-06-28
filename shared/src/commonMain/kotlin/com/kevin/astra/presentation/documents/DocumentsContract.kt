package com.kevin.astra.presentation.documents

import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.documents.AstraDocument
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

data class DocumentsState(
    val availableDocuments: List<AstraDocument> = emptyList(),
    val selectedDocument: AstraDocument? = null,
    val documentStatus: DocumentStatus = DocumentStatus.NotIndexed,
    val indexedChunks: List<IndexedDocumentChunk> = emptyList(),
    val question: String = "",
    val extractedContext: RetrievedDocumentContext? = null,
    val answer: DocumentsAnswer? = null,
    val isIndexing: Boolean = false,
    val isGenerating: Boolean = false,
    val metrics: DocumentsMetrics = DocumentsMetrics(),
    val error: String? = null,
) : AstraState {
    val canIndex: Boolean
        get() = selectedDocument != null && !isIndexing && documentStatus != DocumentStatus.Indexed

    val canAsk: Boolean
        get() = question.isNotBlank() &&
            selectedDocument != null &&
            documentStatus == DocumentStatus.Indexed &&
            !isGenerating &&
            !isIndexing
}

sealed interface DocumentsIntent : AstraIntent {
    data object IndexSelectedDocument : DocumentsIntent
    data class UpdateQuestion(val question: String) : DocumentsIntent
    data object AskDocument : DocumentsIntent
    data object ClearConversation : DocumentsIntent
}

sealed interface DocumentsEffect : AstraEffect
