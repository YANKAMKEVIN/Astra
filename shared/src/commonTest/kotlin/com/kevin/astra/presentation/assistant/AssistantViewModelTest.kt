package com.kevin.astra.presentation.assistant

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantViewModelTest {
    @Test
    fun startsWithIndustrialMaintenanceAndMockMetrics() {
        val viewModel = AssistantViewModel(timestampProvider = { "timestamp" })

        val state = viewModel.state.value

        assertEquals(AssistantIndustry.IndustrialMaintenance, state.selectedIndustry)
        assertEquals("Mock Model", state.metrics.model)
        assertEquals("Mock Engine", state.metrics.backend)
        assertEquals("1.2 s", state.metrics.latency)
        assertEquals("18", state.metrics.tokensPerSecond)
        assertFalse(state.canAsk)
    }

    @Test
    fun updatesQuestionAndIndustry() {
        val viewModel = AssistantViewModel(timestampProvider = { "timestamp" })

        viewModel.dispatch(AssistantIntent.SelectIndustry(AssistantIndustry.Energy))
        viewModel.dispatch(AssistantIntent.UpdateQuestion("Restart Pump A"))

        val state = viewModel.state.value
        assertEquals(AssistantIndustry.Energy, state.selectedIndustry)
        assertEquals("Restart Pump A", state.question)
        assertTrue(state.canAsk)
    }

    @Test
    fun askQuestionShowsLoadingThenMockResponse() = runBlocking {
        val viewModel = AssistantViewModel(
            timestampProvider = { "2026-06-26T10:15:30Z" },
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.SelectIndustry(AssistantIndustry.Aerospace))
        viewModel.dispatch(AssistantIntent.UpdateQuestion("How do we restart Pump A safely?"))
        viewModel.dispatch(AssistantIntent.AskQuestion)

        yield()

        assertTrue(viewModel.state.value.isGenerating)

        delay(1_100)

        val state = viewModel.state.value
        assertFalse(state.isGenerating)
        assertEquals("Emergency restart procedure", state.response?.title)
        assertTrue(state.response?.body.orEmpty().contains("aerospace ground operations engineer"))
        assertEquals("2026-06-26T10:15:30Z", state.generationTimestamp)
    }

    @Test
    fun clearConversationResetsPromptResponseAndTimestamp() = runBlocking {
        val viewModel = AssistantViewModel(
            timestampProvider = { "timestamp" },
            generationScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(AssistantIntent.UpdateQuestion("Restart Pump A"))
        viewModel.dispatch(AssistantIntent.AskQuestion)
        delay(1_100)

        assertNotNull(viewModel.state.value.response)

        viewModel.dispatch(AssistantIntent.ClearConversation)

        val state = viewModel.state.value
        assertEquals("", state.question)
        assertNull(state.response)
        assertNull(state.generationTimestamp)
        assertFalse(state.isGenerating)
    }
}
