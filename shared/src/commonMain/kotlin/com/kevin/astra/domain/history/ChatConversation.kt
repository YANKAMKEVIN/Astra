package com.kevin.astra.domain.history

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: String,
)

data class ChatConversation(
    val id: String,
    val title: String,
    val modelName: String,
    val backendName: String,
    val industry: String,
    val messages: List<ChatMessage>,
    val createdAt: String,
)

interface ConversationRepository {
    fun save(conversation: ChatConversation)
    fun getAll(): List<ChatConversation>
    fun getById(id: String): ChatConversation?
    fun delete(id: String): Boolean
    fun search(query: String): List<ChatConversation>
}
