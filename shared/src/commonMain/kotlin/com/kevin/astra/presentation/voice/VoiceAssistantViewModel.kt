package com.kevin.astra.presentation.voice

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage
import com.kevin.astra.domain.history.ConversationRepository
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.domain.voice.SpeechRecognitionService
import com.kevin.astra.domain.voice.SpeechRecognitionState
import com.kevin.astra.domain.voice.TextToSpeechService
import com.kevin.astra.domain.voice.TtsState
import com.kevin.astra.core.ai.PromptIndustry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceAssistantViewModel(
    private val speechRecognitionService: SpeechRecognitionService,
    private val textToSpeechService: TextToSpeechService,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val conversationRepository: ConversationRepository,
) : AstraViewModel<VoiceAssistantState, VoiceAssistantIntent, VoiceAssistantEffect>(
    initialState = VoiceAssistantState(),
) {
    private var wordPulseJob: Job? = null

    init {
        collectSpeechState()
        collectInputLevel()
        collectTtsState()
    }

    override fun handleIntent(intent: VoiceAssistantIntent) {
        when (intent) {
            VoiceAssistantIntent.ToggleMic -> {
                when (state.value.phase) {
                    VoicePhase.Idle -> {
                        startListening()
                    }
                    VoicePhase.Listening -> {
                        speechRecognitionService.stopListening()
                        wordPulseJob?.cancel()
                        updateState { copy(phase = VoicePhase.Idle, transcript = "", inputLevel = 0f) }
                    }
                    VoicePhase.Speaking -> {
                        textToSpeechService.stop()
                        updateState { copy(phase = VoicePhase.Idle) }
                    }
                    VoicePhase.Processing -> { /* can't interrupt LLM yet */ }
                }
            }

            VoiceAssistantIntent.StopSpeaking -> {
                textToSpeechService.stop()
                updateState { copy(phase = VoicePhase.Idle) }
            }

            VoiceAssistantIntent.ClearError -> {
                updateState { copy(error = null) }
            }

        }
    }

    private fun startListening() {
        updateState { copy(phase = VoicePhase.Listening, transcript = "", response = "", error = null, inputLevel = 0f) }
        speechRecognitionService.startListening()
    }

    private fun collectSpeechState() {
        viewModelScope.launch {
            speechRecognitionService.state.collect { srState ->
                when (srState) {
                    is SpeechRecognitionState.Listening -> {
                        updateState { copy(phase = VoicePhase.Listening) }
                    }
                    is SpeechRecognitionState.Partial -> {
                        val grew = srState.text.length > state.value.transcript.length
                        updateState { copy(transcript = srState.text) }
                        if (grew && !speechRecognitionService.reportsInputLevel) pulseOnNewWords()
                    }
                    is SpeechRecognitionState.Result -> {
                        wordPulseJob?.cancel()
                        updateState { copy(phase = VoicePhase.Processing, transcript = srState.text, inputLevel = 0f) }
                        processQuestion(srState.text)
                    }
                    is SpeechRecognitionState.Error -> {
                        wordPulseJob?.cancel()
                        updateState { copy(phase = VoicePhase.Idle, error = srState.message, inputLevel = 0f) }
                    }
                    SpeechRecognitionState.Idle -> {}
                }
            }
        }
    }

    /** Real microphone level, where the platform measures it. */
    private fun collectInputLevel() {
        if (!speechRecognitionService.reportsInputLevel) return
        viewModelScope.launch {
            speechRecognitionService.inputLevel.collect { level ->
                val shown = if (state.value.phase == VoicePhase.Listening) level else 0f
                if (shown != state.value.inputLevel) updateState { copy(inputLevel = shown) }
            }
        }
    }

    /**
     * Without a level meter, newly transcribed words are the sign that the microphone hears
     * something: the waveform jumps, then settles back to flat.
     */
    private fun pulseOnNewWords() {
        wordPulseJob?.cancel()
        wordPulseJob = viewModelScope.launch {
            WordPulseLevels.forEach { level ->
                if (state.value.phase != VoicePhase.Listening) {
                    updateState { copy(inputLevel = 0f) }
                    return@launch
                }
                updateState { copy(inputLevel = level) }
                delay(WordPulseStepMillis)
            }
        }
    }

    private fun collectTtsState() {
        viewModelScope.launch {
            textToSpeechService.state.collect { ttsState ->
                when (ttsState) {
                    TtsState.Done -> updateState { copy(phase = VoicePhase.Idle) }
                    is TtsState.Error -> updateState { copy(phase = VoicePhase.Idle, error = ttsState.message) }
                    else -> {}
                }
            }
        }
    }

    private fun processQuestion(question: String) {
        viewModelScope.launch {
            val config = aiConfigurationRepository.getConfiguration()
            val model = modelCatalog.modelById(config.selectedModelId)
            val backend = backendCatalog.backendById(config.selectedBackendId)

            if (model == null || backend == null) {
                updateState { copy(phase = VoicePhase.Idle, error = "No AI model configured. Check Settings.") }
                return@launch
            }

            val industry = PromptIndustry.IndustrialMaintenance
            val preparedParts = promptPipeline.preparePrompt(
                PromptBuildRequest(
                    engineerQuestion = question,
                    selectedIndustry = industry,
                    selectedModel = model,
                )
            )

            val result = runCatching {
                withContext(Dispatchers.Default) {
                    askLocalAssistant(
                        PromptRequest(
                            prompt = preparedParts.fullPrompt,
                            systemPrompt = preparedParts.systemPrompt,
                            userMessage = preparedParts.userMessage,
                            industry = industry,
                            model = model.runtimeModel,
                            backend = backend.runtimeBackend,
                            maxTokens = config.maxTokens,
                            temperature = config.temperature,
                        )
                    )
                }
            }

            result.onSuccess { generation ->
                updateState { copy(phase = VoicePhase.Speaking, response = generation.text) }
                textToSpeechService.speak(generation.text)
                conversationRepository.save(
                    ChatConversation(
                        id = generation.generatedAt.replace(Regex("[^0-9]"), ""),
                        title = question.take(60).ifBlank { "Voice conversation" },
                        modelName = generation.model.label,
                        backendName = generation.backend.label,
                        industry = "Voice",
                        messages = listOf(
                            ChatMessage(role = "user", content = question, timestamp = generation.generatedAt),
                            ChatMessage(role = "assistant", content = generation.text, timestamp = generation.generatedAt),
                        ),
                        createdAt = generation.generatedAt,
                    ),
                )
            }.onFailure { e ->
                updateState { copy(phase = VoicePhase.Idle, error = "Generation failed: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        speechRecognitionService.destroy()
        textToSpeechService.destroy()
    }
}

private val WordPulseLevels = listOf(0.85f, 0.55f, 0.25f, 0f)
private const val WordPulseStepMillis = 180L
