package com.kevin.astra.domain.documents

import com.kevin.astra.domain.gmail.GmailMessageSource

/** An email source (file or Gmail) turned into RAG-ready chunks. */
data class IndexedEmail(
    val label: String,
    val emailCount: Int,
    val chunks: List<IndexedDocumentChunk>,
    /** Gmail only: more messages can be fetched (see [FetchGmailUseCase.PAGE_SIZE]). */
    val hasMore: Boolean = false,
)

/**
 * Extracts an imported `.eml`/`.mbox` file and chunks it — shared by the Documents and Assistant
 * screens so the extract-and-index logic lives in one tested place.
 */
class IndexEmailFileUseCase(
    private val emailExtractor: EmailExtractor,
    private val indexer: DocumentIndexer,
) {
    operator fun invoke(bytes: ByteArray, fileName: String): IndexedEmail {
        val email = if (fileName.endsWith(".mbox", ignoreCase = true)) {
            emailExtractor.extractMbox(bytes, fileName)
        } else {
            emailExtractor.extractEml(bytes, fileName)
        }
        require(email.rawText.isNotBlank()) { "Could not extract text from this email file." }
        return IndexedEmail(email.fileName, email.emailCount, indexer.indexText(email.rawText, email.fileName))
    }
}

/** Fetches messages from Gmail and chunks them — shared by the Documents and Assistant screens. */
class FetchGmailUseCase(
    private val gmailSource: GmailMessageSource?,
    private val indexer: DocumentIndexer,
) {
    val isAvailable: Boolean get() = gmailSource != null

    suspend operator fun invoke(query: String?, maxResults: Int = PAGE_SIZE, label: String = "Gmail"): IndexedEmail {
        val source = gmailSource ?: error("Gmail is not available.")
        val doc = source.fetchAsSingleDocument(query = query, maxResults = maxResults, label = label)
        require(doc.rawText.isNotBlank()) { "No Gmail messages found for this request." }
        return IndexedEmail(
            label = doc.fileName,
            emailCount = doc.emailCount,
            chunks = indexer.indexText(doc.rawText, doc.fileName),
            hasMore = doc.hasMore && maxResults < MAX_MESSAGES,
        )
    }

    companion object {
        /** Messages fetched at first, then added by each "load more". */
        const val PAGE_SIZE = 20

        /** Upper bound: every message is downloaded and indexed on the device. */
        const val MAX_MESSAGES = 100
    }
}
