package com.kevin.astra.data.history

import com.kevin.astra.data.storage.TextFileDirectory
import com.kevin.astra.domain.history.ChatConversation

/**
 * One `<id>.json` file per conversation in [directory], as on Android. Unreadable or corrupt files
 * are skipped rather than failing the whole history. A failed write leaves the history as it was.
 */
class DirectoryConversationFileStore(private val directory: TextFileDirectory) : ConversationFileStore {

    override fun write(conversation: ChatConversation) {
        directory.write(fileName(conversation.id), ConversationSerializer.toJson(conversation))
    }

    override fun readAll(): List<ChatConversation> =
        directory.list()
            .filter { it.endsWith(EXTENSION) }
            .mapNotNull { name -> directory.read(name)?.let(ConversationSerializer::fromJson) }
            .sortedByDescending { it.createdAt }

    override fun delete(id: String): Boolean = directory.delete(fileName(id))

    private fun fileName(id: String) = "$id$EXTENSION"

    private companion object {
        const val EXTENSION = ".json"
    }
}
