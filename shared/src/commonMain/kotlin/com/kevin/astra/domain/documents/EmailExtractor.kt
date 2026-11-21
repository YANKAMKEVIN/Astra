package com.kevin.astra.domain.documents

data class LoadedEmailDocument(
    val fileName: String,
    val emailCount: Int,
    val rawText: String,
    val preview: String = rawText.take(200),
    /** Gmail only: older messages exist beyond the ones fetched. */
    val hasMore: Boolean = false,
)

interface EmailExtractor {
    fun extractEml(bytes: ByteArray, fileName: String): LoadedEmailDocument
    fun extractMbox(bytes: ByteArray, fileName: String): LoadedEmailDocument
}

expect fun createEmailExtractor(): EmailExtractor
