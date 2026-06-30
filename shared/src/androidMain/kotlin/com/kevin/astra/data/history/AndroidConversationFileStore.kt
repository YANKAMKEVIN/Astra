package com.kevin.astra.data.history

import android.content.Context
import com.kevin.astra.domain.history.ChatConversation

private lateinit var applicationContext: Context

fun initializeAndroidConversationFileStore(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createConversationFileStore(): ConversationFileStore =
    if (::applicationContext.isInitialized) AndroidConversationFileStore(applicationContext)
    else NoOpConversationFileStore

private class AndroidConversationFileStore(private val context: Context) : ConversationFileStore {

    private val dir get() = context.filesDir.resolve("conversations").also { it.mkdirs() }

    override fun write(conversation: ChatConversation) {
        dir.resolve("${conversation.id}.json").writeText(ConversationSerializer.toJson(conversation))
    }

    override fun readAll(): List<ChatConversation> =
        dir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { ConversationSerializer.fromJson(it.readText()) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()

    override fun delete(id: String): Boolean = dir.resolve("$id.json").delete()
}

private object NoOpConversationFileStore : ConversationFileStore {
    override fun write(conversation: ChatConversation) = Unit
    override fun readAll(): List<ChatConversation> = emptyList()
    override fun delete(id: String): Boolean = false
}
