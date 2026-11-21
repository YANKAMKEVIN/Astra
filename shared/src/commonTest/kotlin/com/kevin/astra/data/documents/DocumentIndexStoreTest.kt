package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.DocumentLibrary
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.IndexedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentIndexStoreTest {

    private class MemoryFile(var text: String? = null) : DocumentIndexFile {
        override fun read(): String? = text
        override fun write(text: String) { this.text = text }
        override fun delete() { text = null }
    }

    private fun source(id: String, isEmail: Boolean = false, summary: String? = null) = IndexedSource(
        id = id,
        name = id.substringAfter(':'),
        isEmail = isEmail,
        pageCount = if (isEmail) 0 else 3,
        emailCount = if (isEmail) 2 else 0,
        chunks = listOf(
            IndexedDocumentChunk("$id-chunk-1", id, "Chunk 1", "Délai de préavis : \"trois\" mois.\nFin.", pageHint = 2),
            IndexedDocumentChunk("$id-chunk-2", id, "Chunk 2", "Second passage"),
        ),
        summary = summary,
    )

    @Test
    fun aSavedLibraryIsReadBackIdentical() {
        val file = MemoryFile()
        val library = listOf(
            source(IndexedSource.pdfId("bail.pdf"), summary = "Un bail de location."),
            source(IndexedSource.gmailId("from:bank"), isEmail = true),
        )

        DocumentIndexStore(file).save(library)

        assertEquals(library, DocumentIndexStore(file).load())
    }

    @Test
    fun savingAnEmptyLibraryDeletesTheFile() {
        val file = MemoryFile()
        val store = DocumentIndexStore(file)
        store.save(listOf(source("pdf:a.pdf")))

        store.save(emptyList())

        assertNull(file.text)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun aCorruptOrUnknownFileReadsAsAnEmptyLibrary() {
        assertTrue(DocumentIndexStore(MemoryFile("{not json")).load().isEmpty())
        assertTrue(DocumentIndexStore(MemoryFile("""{"version":99,"sources":[]}""")).load().isEmpty())
        assertTrue(DocumentIndexStore(MemoryFile(null)).load().isEmpty())
    }

    @Test
    fun aFailingFileNeverThrows() {
        val broken = object : DocumentIndexFile {
            override fun read(): String = error("disk")
            override fun write(text: String) = error("disk full")
            override fun delete() = error("disk")
        }
        val store = DocumentIndexStore(broken)

        store.save(listOf(source("pdf:a.pdf")))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun theLibraryReplacesASourceLoadedAgainAndKeepsTheNewestLast() {
        val first = DocumentLibrary.add(emptyList(), source("pdf:a.pdf"))
        val second = DocumentLibrary.add(first, source("pdf:b.pdf"))
        val reloaded = DocumentLibrary.add(second, source("pdf:a.pdf", summary = "new"))

        assertEquals(listOf("pdf:b.pdf", "pdf:a.pdf"), reloaded.map { it.id })
        assertEquals("new", reloaded.last().summary)
        assertEquals(4, DocumentLibrary.allChunks(reloaded).size)
    }

    @Test
    fun theLibraryDropsTheOldestSourcePastTheCap() {
        var library = emptyList<IndexedSource>()
        repeat(DocumentLibrary.MAX_SOURCES + 2) { library = DocumentLibrary.add(library, source("pdf:$it.pdf")) }

        assertEquals(DocumentLibrary.MAX_SOURCES, library.size)
        assertEquals("pdf:2.pdf", library.first().id)
        assertEquals("pdf:${DocumentLibrary.MAX_SOURCES + 1}.pdf", library.last().id)
    }

    @Test
    fun gmailSourcesAreRecognized() {
        assertTrue(source(IndexedSource.gmailId(null), isEmail = true).isGmail)
        assertTrue(!source(IndexedSource.emailId("inbox.mbox"), isEmail = true).isGmail)
    }
}
