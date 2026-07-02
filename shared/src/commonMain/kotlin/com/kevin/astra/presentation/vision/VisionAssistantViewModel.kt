package com.kevin.astra.presentation.vision

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.export.ConversationShareHelper
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage
import com.kevin.astra.domain.history.ConversationRepository
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.domain.vision.ImageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VisionAssistantViewModel(
    private val imageClassifier: ImageClassifier,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val conversationRepository: ConversationRepository,
    private val shareHelper: ConversationShareHelper,
) : AstraViewModel<VisionAssistantState, VisionAssistantIntent, VisionAssistantEffect>(
    initialState = VisionAssistantState(classifierAvailable = false),
) {
    private var analysisJob: Job? = null

    init {
        updateState { copy(classifierAvailable = imageClassifier.isAvailable) }
    }

    override fun handleIntent(intent: VisionAssistantIntent) {
        when (intent) {
            is VisionAssistantIntent.PhotoCaptured -> {
                analysisJob?.cancel()
                updateState { copy(capturedImageBytes = intent.bytes, phase = VisionPhase.Classifying, error = null) }
                analyzeAndAsk(intent.bytes)
            }
            is VisionAssistantIntent.UpdateQuestion -> {
                updateState { copy(userQuestion = intent.question) }
            }
            VisionAssistantIntent.Reset -> {
                analysisJob?.cancel()
                analysisJob = null
                updateState {
                    VisionAssistantState(classifierAvailable = imageClassifier.isAvailable)
                }
            }
            VisionAssistantIntent.ClearError -> updateState { copy(error = null) }
            VisionAssistantIntent.ExportResponse -> exportResponse()
        }
    }

    private fun analyzeAndAsk(imageBytes: ByteArray) {
        analysisJob = viewModelScope.launch {
            // Step 1: classify
            val classification = runCatching {
                withContext(Dispatchers.Default) { imageClassifier.classify(imageBytes) }
            }.getOrElse {
                updateState { copy(phase = VisionPhase.Idle, error = "Image analysis failed: ${it.message}") }
                return@launch
            }

            updateState { copy(classificationResult = classification, phase = VisionPhase.Thinking) }

            // Step 2: build prompt for Gemma
            val config = aiConfigurationRepository.getConfiguration()
            val model = modelCatalog.modelById(config.selectedModelId)
            val backend = backendCatalog.backendById(config.selectedBackendId)

            if (model == null || backend == null) {
                updateState { copy(phase = VisionPhase.Idle, error = "No AI model configured. Check Settings.") }
                return@launch
            }

            val visionContext = classification.toPromptDescription()
            val question = "${state.value.userQuestion}\n\nContext: $visionContext"

            val preparedParts = promptPipeline.preparePrompt(
                PromptBuildRequest(
                    engineerQuestion = question,
                    selectedIndustry = PromptIndustry.IndustrialMaintenance,
                    selectedModel = model,
                )
            )

            // Step 3: ask LLM
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    askLocalAssistant(
                        PromptRequest(
                            prompt = preparedParts.fullPrompt,
                            systemPrompt = preparedParts.systemPrompt,
                            userMessage = preparedParts.userMessage,
                            industry = PromptIndustry.IndustrialMaintenance,
                            model = model.runtimeModel,
                            backend = backend.runtimeBackend,
                            maxTokens = config.maxTokens,
                            temperature = config.temperature,
                        )
                    )
                }
            }

            if (!isActive) return@launch

            result.onSuccess { generation ->
                updateState { copy(phase = VisionPhase.Done, response = generation.text) }
                conversationRepository.save(
                    ChatConversation(
                        id = generation.generatedAt.replace(Regex("[^0-9]"), ""),
                        title = state.value.userQuestion.take(60).ifBlank { "Vision conversation" },
                        modelName = generation.model.label,
                        backendName = generation.backend.label,
                        industry = "Vision",
                        messages = listOf(
                            ChatMessage(role = "user", content = state.value.userQuestion, timestamp = generation.generatedAt),
                            ChatMessage(role = "assistant", content = generation.text, timestamp = generation.generatedAt),
                        ),
                        createdAt = generation.generatedAt,
                    ),
                )
            }.onFailure { e ->
                updateState { copy(phase = VisionPhase.Idle, error = "Generation failed: ${e.message}") }
            }
        }
    }

    fun exportResponse() {
        val s = state.value
        if (s.response.isBlank()) return
        shareHelper.share(
            ChatConversation(
                id = "vision_export",
                title = s.userQuestion.take(60).ifBlank { "Vision analysis" },
                modelName = "Vision",
                backendName = "On-device",
                industry = "Vision",
                messages = listOf(
                    ChatMessage(role = "user", content = s.userQuestion, timestamp = ""),
                    ChatMessage(role = "assistant", content = s.response, timestamp = ""),
                ),
                createdAt = "",
            ),
            ExportFormat.PlainText,
        )
    }
}
