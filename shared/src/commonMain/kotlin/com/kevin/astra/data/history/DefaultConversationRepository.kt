package com.kevin.astra.data.history

import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ConversationRepository

class DefaultConversationRepository(
    private val fileStore: ConversationFileStore,
) : ConversationRepository {

    override fun save(conversation: ChatConversation) = fileStore.write(conversation)

    override fun getAll(): List<ChatConversation> = fileStore.readAll()

    override fun getById(id: String): ChatConversation? = fileStore.readAll().find { it.id == id }

    override fun delete(id: String): Boolean = fileStore.delete(id)

    override fun search(query: String): List<ChatConversation> {
        if (query.isBlank()) return getAll()
        val q = query.trim().lowercase()
        return getAll().filter { conv ->
            conv.title.lowercase().contains(q) ||
                conv.modelName.lowercase().contains(q) ||
                conv.industry.lowercase().contains(q) ||
                conv.messages.any { it.content.lowercase().contains(q) }
        }
    }
}
