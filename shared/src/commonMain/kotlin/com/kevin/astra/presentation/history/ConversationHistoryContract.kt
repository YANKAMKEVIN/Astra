package com.kevin.astra.presentation.history

import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.history.ChatConversation

data class ConversationHistoryState(
    val conversations: List<ChatConversation> = emptyList(),
    val searchQuery: String = "",
    val selectedConversation: ChatConversation? = null,
    val isLoading: Boolean = false,
    /** Last deleted conversation, kept so the deletion can be undone. */
    val recentlyDeleted: ChatConversation? = null,
) : AstraState

sealed interface ConversationHistoryIntent : AstraIntent {
    data object LoadHistory : ConversationHistoryIntent
    data class UpdateSearch(val query: String) : ConversationHistoryIntent
    data class SelectConversation(val id: String) : ConversationHistoryIntent
    data object CloseDetail : ConversationHistoryIntent
    data class DeleteConversation(val id: String) : ConversationHistoryIntent
    /** Restores the conversation deleted last (the "Undo" of the deletion message). */
    data object UndoDelete : ConversationHistoryIntent
    data class ExportConversation(val id: String, val format: ExportFormat) : ConversationHistoryIntent
}

sealed interface ConversationHistoryEffect : AstraEffect {
    data class ShowError(val message: String) : ConversationHistoryEffect
    /** A conversation was deleted; the screen offers to undo it. */
    data class ConversationDeleted(val title: String) : ConversationHistoryEffect
}
