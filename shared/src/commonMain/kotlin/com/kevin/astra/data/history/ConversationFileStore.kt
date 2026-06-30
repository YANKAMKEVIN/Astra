package com.kevin.astra.data.history

import com.kevin.astra.domain.history.ChatConversation

interface ConversationFileStore {
    fun write(conversation: ChatConversation)
    fun readAll(): List<ChatConversation>
    fun delete(id: String): Boolean
}

expect fun createConversationFileStore(): ConversationFileStore
