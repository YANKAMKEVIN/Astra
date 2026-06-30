package com.kevin.astra.presentation.history

import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.export.ConversationShareHelper
import com.kevin.astra.domain.history.ConversationRepository

class ConversationHistoryViewModel(
    private val conversationRepository: ConversationRepository,
    private val shareHelper: ConversationShareHelper,
) : AstraViewModel<ConversationHistoryState, ConversationHistoryIntent, ConversationHistoryEffect>(
    initialState = ConversationHistoryState(isLoading = true),
) {
    init {
        loadAll()
    }

    override fun handleIntent(intent: ConversationHistoryIntent) {
        when (intent) {
            ConversationHistoryIntent.LoadHistory -> loadAll()

            is ConversationHistoryIntent.UpdateSearch -> {
                val query = intent.query
                updateState {
                    copy(
                        searchQuery = query,
                        conversations = conversationRepository.search(query),
                    )
                }
            }

            is ConversationHistoryIntent.SelectConversation -> {
                val conv = conversationRepository.getById(intent.id)
                updateState { copy(selectedConversation = conv) }
            }

            ConversationHistoryIntent.CloseDetail -> {
                updateState { copy(selectedConversation = null) }
            }

            is ConversationHistoryIntent.DeleteConversation -> {
                conversationRepository.delete(intent.id)
                updateState {
                    val updated = conversationRepository.search(searchQuery)
                    copy(
                        conversations = updated,
                        selectedConversation = if (selectedConversation?.id == intent.id) null else selectedConversation,
                    )
                }
            }

            is ConversationHistoryIntent.ExportConversation -> {
                val conv = conversationRepository.getById(intent.id) ?: return
                shareHelper.share(conv, intent.format)
            }
        }
    }

    private fun loadAll() {
        val all = conversationRepository.getAll()
        updateState { copy(conversations = all, isLoading = false) }
    }
}
