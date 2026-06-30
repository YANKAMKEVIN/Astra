package com.kevin.astra.domain.export

import com.kevin.astra.domain.history.ChatConversation

interface ConversationShareHelper {
    fun share(conversation: ChatConversation, format: ExportFormat)
}

expect fun createConversationShareHelper(): ConversationShareHelper
