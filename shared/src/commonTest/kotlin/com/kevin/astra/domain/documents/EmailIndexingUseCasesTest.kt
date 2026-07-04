package com.kevin.astra.domain.documents

import com.kevin.astra.data.documents.SmartTextChunker
import com.kevin.astra.domain.gmail.GmailMessageSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailIndexingUseCasesTest {
    private val chunker = SmartTextChunker()

    private val extractor = object : EmailExtractor {
        override fun extractEml(bytes: ByteArray, fileName: String) =
            LoadedEmailDocument(fileName, 1, bytes.decodeToString())
        override fun extractMbox(bytes: ByteArray, fileName: String) =
            LoadedEmailDocument(fileName, 3, bytes.decodeToString())
    }

    @Test
    fun indexesEmlFileIntoChunks() {
        val useCase = IndexEmailFileUseCase(extractor, chunker)
        val text = "Subject: Invoice. Amount due next week. ".repeat(60)

        val indexed = useCase(text.encodeToByteArray(), "invoice.eml")

        assertEquals("invoice.eml", indexed.label)
        assertEquals(1, indexed.emailCount)
        assertTrue(indexed.chunks.isNotEmpty())
    }

    @Test
    fun usesMboxExtractorForMboxFiles() {
        val useCase = IndexEmailFileUseCase(extractor, chunker)
        val indexed = useCase("From x\nSubject: A\n\nbody".encodeToByteArray(), "inbox.mbox")
        assertEquals(3, indexed.emailCount)
    }

    @Test
    fun rejectsEmptyEmail() {
        val emptyExtractor = object : EmailExtractor {
            override fun extractEml(bytes: ByteArray, fileName: String) = LoadedEmailDocument(fileName, 0, "")
            override fun extractMbox(bytes: ByteArray, fileName: String) = LoadedEmailDocument(fileName, 0, "")
        }
        val useCase = IndexEmailFileUseCase(emptyExtractor, chunker)
        assertFailsWith<IllegalArgumentException> { useCase(ByteArray(0), "empty.eml") }
    }

    @Test
    fun gmailUseCaseIsUnavailableWithoutSource() {
        assertFalse(FetchGmailUseCase(gmailSource = null, indexer = chunker).isAvailable)
    }

    @Test
    fun gmailUseCaseFetchesAndChunks() = runBlocking {
        val source = GmailMessageSource { _, _, label -> LoadedEmailDocument(label, 2, "Balance is 1000. ".repeat(50)) }
        val useCase = FetchGmailUseCase(source, chunker)

        assertTrue(useCase.isAvailable)
        val indexed = useCase(query = null)
        assertEquals("Gmail", indexed.label)
        assertEquals(2, indexed.emailCount)
        assertTrue(indexed.chunks.isNotEmpty())
    }
}
