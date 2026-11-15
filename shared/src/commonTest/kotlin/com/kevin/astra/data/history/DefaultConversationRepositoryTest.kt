package com.kevin.astra.data.history

import com.kevin.astra.domain.history.ChatConversation
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultConversationRepositoryTest {
    private class ListFileStore(private val stored: List<ChatConversation>) : ConversationFileStore {
        override fun write(conversation: ChatConversation) = Unit
        override fun readAll(): List<ChatConversation> = stored
        override fun delete(id: String): Boolean = false
    }

    private fun conversation(id: String, createdAt: String) =
        ChatConversation(id, "Title $id", "model", "backend", "General", emptyList(), createdAt)

    @Test
    fun getAllReturnsConversationsNewestFirstWhateverTheStoreOrder() {
        val repository = DefaultConversationRepository(
            ListFileStore(
                listOf(
                    conversation("old", "2026-01-01T10:00:00Z"),
                    conversation("new", "2026-03-01T10:00:00Z"),
                    conversation("mid", "2026-02-01T10:00:00Z"),
                ),
            ),
        )

        assertEquals(listOf("new", "mid", "old"), repository.getAll().map { it.id })
    }
}
