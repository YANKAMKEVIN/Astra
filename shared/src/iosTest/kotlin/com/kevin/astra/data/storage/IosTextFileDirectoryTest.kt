package com.kevin.astra.data.storage

import com.kevin.astra.data.documents.DirectoryDocumentIndexFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosTextFileDirectoryTest {

    // A dedicated subdirectory, so the test never touches the app's real index or conversations.
    private val directory = IosTextFileDirectory("astra-storage-test")

    @BeforeTest
    fun setUp() = clear()

    @AfterTest
    fun tearDown() = clear()

    private fun clear() = directory.list().forEach { directory.delete(it) }

    @Test
    fun aFileIsWrittenReadBackListedAndDeleted() {
        assertTrue(directory.list().isEmpty())
        assertNull(directory.read("a.json"))

        assertTrue(directory.write("a.json", "{\"title\":\"Préavis — 3 mois\"}"))

        assertEquals("{\"title\":\"Préavis — 3 mois\"}", directory.read("a.json"))
        assertEquals(listOf("a.json"), directory.list())
        assertTrue(directory.delete("a.json"))
        assertFalse(directory.delete("a.json"))
        assertTrue(directory.list().isEmpty())
    }

    @Test
    fun writingAgainReplacesTheFile() {
        directory.write(DirectoryDocumentIndexFile.FILE_NAME, "old")
        directory.write(DirectoryDocumentIndexFile.FILE_NAME, "new")

        assertEquals("new", DirectoryDocumentIndexFile(directory).read())
        assertEquals(listOf(DirectoryDocumentIndexFile.FILE_NAME), directory.list())
    }
}
