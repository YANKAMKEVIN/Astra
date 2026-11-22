package com.kevin.astra.data.documents

import com.kevin.astra.data.storage.MemoryTextFileDirectory
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.IndexedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DirectoryDocumentIndexFileTest {

    private val library = listOf(
        IndexedSource(
            id = IndexedSource.pdfId("bail.pdf"),
            name = "bail.pdf",
            isEmail = false,
            pageCount = 2,
            emailCount = 0,
            chunks = listOf(IndexedDocumentChunk("c1", IndexedSource.pdfId("bail.pdf"), "Chunk 1", "Préavis : trois mois.")),
            summary = "Un bail.",
        ),
    )

    @Test
    fun theIndexIsSavedAsIndexJsonAndReadBackAfterARestart() {
        val directory = MemoryTextFileDirectory()

        DocumentIndexStore(DirectoryDocumentIndexFile(directory)).save(library)
        // A new store over the same directory = the app launched again.
        val restored = DocumentIndexStore(DirectoryDocumentIndexFile(directory)).load()

        assertEquals(listOf(DirectoryDocumentIndexFile.FILE_NAME), directory.list())
        assertEquals(library, restored)
    }

    @Test
    fun anEmptyLibraryRemovesTheFile() {
        val directory = MemoryTextFileDirectory()
        val store = DocumentIndexStore(DirectoryDocumentIndexFile(directory))
        store.save(library)

        store.save(emptyList())

        assertTrue(directory.list().isEmpty())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun aMissingFileReadsAsNoIndex() {
        assertNull(DirectoryDocumentIndexFile(MemoryTextFileDirectory()).read())
        assertTrue(DocumentIndexStore(DirectoryDocumentIndexFile(MemoryTextFileDirectory())).load().isEmpty())
    }

    @Test
    fun aFailedWriteIsReportedAndTheStoreKeepsTheAppRunning() {
        val directory = MemoryTextFileDirectory(failWrites = true)

        assertFailsWith<IllegalStateException> { DirectoryDocumentIndexFile(directory).write("{}") }
        DocumentIndexStore(DirectoryDocumentIndexFile(directory)).save(library) // must not throw
        assertTrue(directory.list().isEmpty())
    }
}
