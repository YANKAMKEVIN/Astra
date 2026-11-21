package com.kevin.astra.domain.documents

/** One indexed source of the RAG library: a PDF, an e-mail file or a Gmail fetch. */
data class IndexedSource(
    /** Stable key: loading the same file (or the same Gmail request) again replaces the entry. */
    val id: String,
    val name: String,
    val isEmail: Boolean,
    val pageCount: Int = 0,
    val emailCount: Int = 0,
    val chunks: List<IndexedDocumentChunk>,
    val summary: String? = null,
) {
    val isGmail: Boolean get() = id.startsWith(GMAIL_PREFIX)

    companion object {
        const val GMAIL_PREFIX = "gmail:"

        fun pdfId(fileName: String) = "pdf:$fileName"
        fun emailId(fileName: String) = "email:$fileName"
        fun gmailId(query: String?) = GMAIL_PREFIX + (query ?: "latest")
    }
}

/**
 * The documents the questions are answered from. Several sources can be loaded at once; the most
 * recently added one is last. The library is capped at [MAX_SOURCES]: every chunk is ranked on
 * the device for each question.
 */
object DocumentLibrary {
    const val MAX_SOURCES = 10

    /** Adds [source] last, replacing an entry with the same id, and drops the oldest past the cap. */
    fun add(library: List<IndexedSource>, source: IndexedSource): List<IndexedSource> =
        (library.filterNot { it.id == source.id } + source).takeLast(MAX_SOURCES)

    fun allChunks(library: List<IndexedSource>): List<IndexedDocumentChunk> = library.flatMap { it.chunks }
}
