package com.kevin.astra.domain.gmail

import com.kevin.astra.domain.documents.EmailExtractor
import com.kevin.astra.domain.documents.LoadedEmailDocument

/**
 * Fetches messages from Gmail and turns them into [LoadedEmailDocument]s using the same
 * [EmailExtractor] that powers local .eml/.mbox import — so downstream RAG indexing is identical
 * whether the email came from a file or from Gmail.
 */
class GmailRepository(
    private val apiClient: GmailApiClient,
    private val emailExtractor: EmailExtractor,
) {
    /** Fetches up to [maxResults] messages (optionally filtered by [query]) as parsed documents. */
    suspend fun fetchMessages(query: String? = null, maxResults: Int = 20): List<LoadedEmailDocument> {
        val ids = apiClient.listMessageIds(query = query, maxResults = maxResults)
        return ids.map { id ->
            val bytes = apiClient.getRawMessage(id)
            emailExtractor.extractEml(bytes, fileName = "gmail_$id.eml")
        }
    }

    /**
     * Fetches messages and merges them into a single [LoadedEmailDocument] (like an mbox), ready to
     * feed straight into the existing email RAG indexing path.
     */
    suspend fun fetchAsSingleDocument(
        query: String? = null,
        maxResults: Int = 20,
        label: String = "Gmail",
    ): LoadedEmailDocument {
        val messages = fetchMessages(query = query, maxResults = maxResults)
        val merged = messages.joinToString("\n\n---\n\n") { it.rawText }
        return LoadedEmailDocument(
            fileName = if (query.isNullOrBlank()) "$label (latest $maxResults)" else "$label · \"$query\"",
            emailCount = messages.size,
            rawText = merged,
        )
    }
}
