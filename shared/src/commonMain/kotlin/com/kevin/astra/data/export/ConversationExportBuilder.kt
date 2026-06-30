package com.kevin.astra.data.export

import com.kevin.astra.domain.history.ChatConversation

internal object ConversationExportBuilder {

    fun toMarkdown(conversation: ChatConversation): String = buildString {
        appendLine("# ${conversation.title}")
        appendLine()
        appendLine("**Model:** ${conversation.modelName}  ")
        appendLine("**Backend:** ${conversation.backendName}  ")
        appendLine("**Industry:** ${conversation.industry}  ")
        appendLine("**Date:** ${conversation.createdAt}")
        appendLine()
        appendLine("---")
        appendLine()
        conversation.messages.forEach { message ->
            val speaker = if (message.role == "user") "**You**" else "**ASTRA**"
            appendLine("### $speaker")
            appendLine()
            appendLine(message.content)
            appendLine()
        }
    }

    fun toPlainText(conversation: ChatConversation): String = buildString {
        appendLine(conversation.title)
        appendLine("=".repeat(conversation.title.length.coerceAtMost(60)))
        appendLine()
        appendLine("Model   : ${conversation.modelName}")
        appendLine("Backend : ${conversation.backendName}")
        appendLine("Industry: ${conversation.industry}")
        appendLine("Date    : ${conversation.createdAt}")
        appendLine()
        conversation.messages.forEachIndexed { index, message ->
            if (index > 0) appendLine()
            val speaker = if (message.role == "user") "YOU" else "ASTRA"
            appendLine("[$speaker]")
            appendLine(message.content)
        }
    }
}
