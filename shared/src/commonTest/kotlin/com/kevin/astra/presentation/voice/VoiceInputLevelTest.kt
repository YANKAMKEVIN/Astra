package com.kevin.astra.presentation.voice

import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.MockInferenceEngine
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ConversationRepository
import com.kevin.astra.domain.voice.SpeechRecognitionService
import com.kevin.astra.domain.voice.SpeechRecognitionState
import com.kevin.astra.domain.voice.TextToSpeechService
import com.kevin.astra.domain.voice.TtsState
import com.kevin.astra.domain.voice.normalizeRmsDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInputLevelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rmsIsMappedFromSilenceToLoud() {
        assertEquals(0f, normalizeRmsDb(-2f))
        assertEquals(0f, normalizeRmsDb(-10f))
        assertEquals(0.5f, normalizeRmsDb(4f))
        assertEquals(1f, normalizeRmsDb(12f))
    }

    @Test
    fun measuredLevelDrivesTheWaveformOnlyWhileListening() {
        val speech = FakeSpeech(reportsLevel = true)
        val viewModel = voiceViewModel(speech)

        viewModel.dispatch(VoiceAssistantIntent.ToggleMic)
        speech.level.value = 0.7f
        assertEquals(0.7f, viewModel.state.value.inputLevel)

        speech.level.value = 0f
        assertEquals(0f, viewModel.state.value.inputLevel)

        viewModel.dispatch(VoiceAssistantIntent.ToggleMic) // stop
        speech.level.value = 0.9f
        assertEquals(0f, viewModel.state.value.inputLevel)
    }

    @Test
    fun withoutAMeterNewWordsPulseThenSettleFlat() {
        val speech = FakeSpeech(reportsLevel = false)
        val viewModel = voiceViewModel(speech)
        viewModel.dispatch(VoiceAssistantIntent.ToggleMic)
        assertEquals(0f, viewModel.state.value.inputLevel)

        speech.mutableState.value = SpeechRecognitionState.Partial("What is")
        assertEquals(0.85f, viewModel.state.value.inputLevel)

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0f, viewModel.state.value.inputLevel)
        assertEquals("What is", viewModel.state.value.transcript)
    }

    private fun voiceViewModel(speech: FakeSpeech) = VoiceAssistantViewModel(
        speechRecognitionService = speech,
        textToSpeechService = SilentTts,
        askLocalAssistant = AskLocalAssistantUseCase(MockInferenceEngine(streamDelayMs = 0)),
        aiConfigurationRepository = testAiConfigurationRepository(),
        modelCatalog = DefaultModelCatalog(),
        backendCatalog = DefaultBackendCatalog(),
        promptPipeline = DefaultPromptPipeline(DefaultPromptBuilder()),
        conversationRepository = NoConversations,
    )
}

private class FakeSpeech(reportsLevel: Boolean) : SpeechRecognitionService {
    val mutableState = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val level = MutableStateFlow(0f)
    override val state: StateFlow<SpeechRecognitionState> = mutableState
    override val inputLevel: StateFlow<Float> = level
    override val reportsInputLevel: Boolean = reportsLevel
    override fun startListening() {
        mutableState.value = SpeechRecognitionState.Listening
    }
    override fun stopListening() {
        mutableState.value = SpeechRecognitionState.Idle
    }
    override fun destroy() = Unit
}

private object SilentTts : TextToSpeechService {
    override val state: StateFlow<TtsState> = MutableStateFlow(TtsState.Idle)
    override fun speak(text: String) = Unit
    override fun stop() = Unit
    override fun destroy() = Unit
}

private object NoConversations : ConversationRepository {
    override fun save(conversation: ChatConversation) = Unit
    override fun getAll(): List<ChatConversation> = emptyList()
    override fun getById(id: String): ChatConversation? = null
    override fun delete(id: String): Boolean = false
    override fun search(query: String): List<ChatConversation> = emptyList()
}
