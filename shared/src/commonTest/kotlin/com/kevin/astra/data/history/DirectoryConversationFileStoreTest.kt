package com.kevin.astra.data.history

import com.kevin.astra.data.storage.MemoryTextFileDirectory
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectoryConversationFileStoreTest {

    private fun conversation(id: String, createdAt: String) = ChatConversation(
        id = id,
        title = "Bail $id",
        modelName = "Qwen3 0.6B",
        backendName = "LiteRT-LM",
        industry = "General",
        messages = listOf(
            ChatMessage("user", "Quel est le \"préavis\" ?", createdAt),
            ChatMessage("assistant", "Trois mois.\nVoir l'article 4.", createdAt),
        ),
        createdAt = createdAt,
    )

    @Test
    fun conversationsAreSavedOneFilePerIdAndReadBackNewestFirst() {
        val directory = MemoryTextFileDirectory()
        val store = DirectoryConversationFileStore(directory)
        val old = conversation("1700000000000", "2026-01-01T10:00:00Z")
        val new = conversation("1800000000000", "2026-03-01T10:00:00Z")

        store.write(old)
        store.write(new)

        assertEquals(listOf("1700000000000.json", "1800000000000.json"), directory.list())
        assertEquals(listOf(new, old), DirectoryConversationFileStore(directory).readAll())
    }

    @Test
    fun savingAgainReplacesTheConversation() {
        val directory = MemoryTextFileDirectory()
        val store = DirectoryConversationFileStore(directory)
        val first = conversation("42", "2026-01-01T10:00:00Z")
        store.write(first)

        val updated = first.copy(title = "Renamed", messages = first.messages.take(1))
        store.write(updated)

        assertEquals(listOf(updated), store.readAll())
    }

    @Test
    fun deleteRemovesOnlyThatConversation() {
        val store = DirectoryConversationFileStore(MemoryTextFileDirectory())
        store.write(conversation("a", "2026-01-01T10:00:00Z"))
        store.write(conversation("b", "2026-01-02T10:00:00Z"))

        assertTrue(store.delete("a"))
        assertFalse(store.delete("a"))
        assertEquals(listOf("b"), store.readAll().map { it.id })
    }

    @Test
    fun corruptAndForeignFilesAreSkipped() {
        val directory = MemoryTextFileDirectory()
        val store = DirectoryConversationFileStore(directory)
        store.write(conversation("ok", "2026-01-01T10:00:00Z"))
        directory.files["broken.json"] = "{ not json"
        directory.files["notes.txt"] = "ignored"

        assertEquals(listOf("ok"), store.readAll().map { it.id })
    }

    @Test
    fun aFailedWriteLeavesTheHistoryAsItWas() {
        val directory = MemoryTextFileDirectory()
        val store = DirectoryConversationFileStore(directory)
        store.write(conversation("kept", "2026-01-01T10:00:00Z"))

        directory.failWrites = true
        store.write(conversation("lost", "2026-01-02T10:00:00Z"))

        assertEquals(listOf("kept"), store.readAll().map { it.id })
    }
}
