package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.IndexedSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Raw storage of the saved index: one text file in the app's private storage. */
interface DocumentIndexFile {
    fun read(): String?
    fun write(text: String)
    fun delete()
}

/** No persistence (iOS for now, previews, tests): the library lives only in memory. */
object NoOpDocumentIndexFile : DocumentIndexFile {
    override fun read(): String? = null
    override fun write(text: String) = Unit
    override fun delete() = Unit
}

expect fun createDocumentIndexFile(): DocumentIndexFile

/**
 * Saves the RAG library (chunks and summaries) so indexed documents survive an app restart
 * without being extracted, chunked and summarized again. A missing, corrupt or older-format file
 * reads as an empty library: the index can always be rebuilt from the documents.
 */
class DocumentIndexStore(private val file: DocumentIndexFile) {

    fun load(): List<IndexedSource> {
        val text = runCatching { file.read() }.getOrNull() ?: return emptyList()
        val stored = runCatching { json.decodeFromString(StoredIndex.serializer(), text) }.getOrNull()
            ?: return emptyList()
        if (stored.version != FORMAT_VERSION) return emptyList()
        return stored.sources.map { it.toDomain() }
    }

    fun save(library: List<IndexedSource>) {
        runCatching {
            if (library.isEmpty()) {
                file.delete()
            } else {
                val stored = StoredIndex(FORMAT_VERSION, library.map { it.toStored() })
                file.write(json.encodeToString(StoredIndex.serializer(), stored))
            }
        }
    }

    @Serializable
    private data class StoredIndex(val version: Int, val sources: List<StoredSource>)

    @Serializable
    private data class StoredSource(
        val id: String,
        val name: String,
        val isEmail: Boolean,
        val pageCount: Int = 0,
        val emailCount: Int = 0,
        val summary: String? = null,
        val chunks: List<StoredChunk>,
    )

    @Serializable
    private data class StoredChunk(
        val id: String,
        val documentId: String,
        val title: String,
        val content: String,
        val pageHint: Int = 0,
    )

    private fun StoredSource.toDomain() = IndexedSource(
        id = id,
        name = name,
        isEmail = isEmail,
        pageCount = pageCount,
        emailCount = emailCount,
        summary = summary,
        chunks = chunks.map { IndexedDocumentChunk(it.id, it.documentId, it.title, it.content, it.pageHint) },
    )

    private fun IndexedSource.toStored() = StoredSource(
        id = id,
        name = name,
        isEmail = isEmail,
        pageCount = pageCount,
        emailCount = emailCount,
        summary = summary,
        chunks = chunks.map { StoredChunk(it.id, it.documentId, it.title, it.content, it.pageHint) },
    )

    private companion object {
        const val FORMAT_VERSION = 1
        val json = Json { ignoreUnknownKeys = true }
    }
}
