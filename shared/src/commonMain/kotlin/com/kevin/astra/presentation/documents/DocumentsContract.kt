package com.kevin.astra.presentation.documents

import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.FetchGmailUseCase
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.IndexedSource
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
    /** Model picked for this screen only; null = follow the app-wide selection ([activeModel]). */
    val sessionModel: LocalModel? = null,
    /** The app-wide selected model, refreshed each time the screen resumes. */
    val activeModel: LocalModel? = null,
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
    // Gmail integration (optional, opt-in — data is fetched from Google then analyzed on-device)
    val gmailSupported: Boolean = false,
    val gmailConnected: Boolean = false,
    val gmailQuery: String = "",
    val isFetchingGmail: Boolean = false,
    /** The Gmail request behind the loaded messages (null query = latest), kept to load more. */
    val gmailFetch: GmailFetch? = null,
    /** Every indexed source questions are answered from (several PDFs / e-mails at once), saved locally. */
    val library: List<IndexedSource> = emptyList(),
    /** The library entry shown in the document card (the last one added). */
    val currentSourceId: String? = null,
) : AstraState {
    val canIndex: Boolean
        get() = loadedFileName != null && !isIndexing && !isLoading &&
            documentStatus == DocumentStatus.NotIndexed

    /** True when there is something to ask about: the current document or the saved library. */
    val hasIndexedContent: Boolean
        get() = documentStatus == DocumentStatus.Indexed || library.isNotEmpty()

    val canAsk: Boolean
        get() = question.isNotBlank() &&
            hasIndexedContent &&
            !isGenerating && !isIndexing && !isSummarizing
}

sealed interface DocumentsIntent : AstraIntent {
    data class PdfLoaded(val bytes: ByteArray, val fileName: String) : DocumentsIntent
    data class EmailLoaded(val bytes: ByteArray, val fileName: String) : DocumentsIntent
    data object IndexDocument : DocumentsIntent
    data class UpdateQuestion(val question: String) : DocumentsIntent
    data object AskDocument : DocumentsIntent
    /** Clears every loaded document and the saved index. */
    data object ClearDocument : DocumentsIntent
    /** Removes one source from the library (and from the saved index). */
    data class RemoveSource(val sourceId: String) : DocumentsIntent
    data object ClearAnswer : DocumentsIntent
    data class SelectSessionModel(val modelId: String) : DocumentsIntent
    data object RefreshModels : DocumentsIntent
    // Gmail
    data object RefreshGmailState : DocumentsIntent
    data object ConnectGmail : DocumentsIntent
    data object DisconnectGmail : DocumentsIntent
    data class UpdateGmailQuery(val query: String) : DocumentsIntent
    data object FetchGmailRecent : DocumentsIntent
    data object FetchGmailSearch : DocumentsIntent
    /** Refetches the same Gmail request with [FetchGmailUseCase.PAGE_SIZE] more messages. */
    data object FetchGmailMore : DocumentsIntent
}

/** A Gmail fetch: its query, how many messages were asked, and whether older ones remain. */
data class GmailFetch(val query: String?, val count: Int, val hasMore: Boolean)

sealed interface DocumentsEffect : AstraEffect
