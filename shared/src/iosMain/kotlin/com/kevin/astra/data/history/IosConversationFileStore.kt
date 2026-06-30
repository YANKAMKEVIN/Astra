package com.kevin.astra.data.history

import com.kevin.astra.domain.history.ChatConversation

actual fun createConversationFileStore(): ConversationFileStore = IosConversationFileStore

private object IosConversationFileStore : ConversationFileStore {
    override fun write(conversation: ChatConversation) = Unit
    override fun readAll(): List<ChatConversation> = emptyList()
    override fun delete(id: String): Boolean = false
}
