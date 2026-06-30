package com.kevin.astra.domain.export

import com.kevin.astra.domain.history.ChatConversation

actual fun createConversationShareHelper(): ConversationShareHelper = IosConversationShareHelper

private object IosConversationShareHelper : ConversationShareHelper {
    override fun share(conversation: ChatConversation, format: ExportFormat) = Unit
}
