package com.kevin.astra.presentation.history

import com.kevin.astra.domain.export.ConversationShareHelper
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationHistoryViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deletingAConversationOffersToUndoIt() = runBlocking {
        val repository = InMemoryConversationRepository(conversation("a", "2026-01-02"), conversation("b", "2026-01-01"))
        val viewModel = ConversationHistoryViewModel(repository, NoShare)

        viewModel.dispatch(ConversationHistoryIntent.DeleteConversation("a"))

        assertEquals(listOf("b"), viewModel.state.value.conversations.map { it.id })
        assertEquals("a", viewModel.state.value.recentlyDeleted?.id)
        assertEquals(ConversationHistoryEffect.ConversationDeleted("Title a"), viewModel.effects.first())
    }

    @Test
    fun undoRestoresTheDeletedConversationInPlace() {
        val repository = InMemoryConversationRepository(conversation("a", "2026-01-02"), conversation("b", "2026-01-01"))
        val viewModel = ConversationHistoryViewModel(repository, NoShare)

        viewModel.dispatch(ConversationHistoryIntent.DeleteConversation("a"))
        viewModel.dispatch(ConversationHistoryIntent.UndoDelete)

        assertEquals(listOf("a", "b"), viewModel.state.value.conversations.map { it.id })
        assertEquals(listOf("a", "b"), repository.getAll().map { it.id })
        assertNull(viewModel.state.value.recentlyDeleted)
    }

    @Test
    fun undoWithNothingDeletedChangesNothing() {
        val repository = InMemoryConversationRepository(conversation("a", "2026-01-02"))
        val viewModel = ConversationHistoryViewModel(repository, NoShare)

        viewModel.dispatch(ConversationHistoryIntent.UndoDelete)

        assertEquals(listOf("a"), viewModel.state.value.conversations.map { it.id })
    }

    private fun conversation(id: String, createdAt: String) = ChatConversation(
        id = id,
        title = "Title $id",
        modelName = "Mock",
        backendName = "CPU",
        industry = "General",
        messages = emptyList(),
        createdAt = createdAt,
    )
}

private object NoShare : ConversationShareHelper {
    override fun share(conversation: ChatConversation, format: ExportFormat) = Unit
}

private class InMemoryConversationRepository(vararg initial: ChatConversation) : ConversationRepository {
    private val store = initial.associateBy { it.id }.toMutableMap()
    override fun save(conversation: ChatConversation) {
        store[conversation.id] = conversation
    }
    override fun getAll(): List<ChatConversation> = store.values.sortedByDescending { it.createdAt }
    override fun getById(id: String): ChatConversation? = store[id]
    override fun delete(id: String): Boolean = store.remove(id) != null
    override fun search(query: String): List<ChatConversation> =
        getAll().filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
}
