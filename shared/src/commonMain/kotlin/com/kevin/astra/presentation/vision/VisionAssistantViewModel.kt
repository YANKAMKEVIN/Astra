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
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.domain.vision.ImageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VisionAssistantViewModel(
    private val imageClassifier: ImageClassifier,
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
) : AstraViewModel<VisionAssistantState, VisionAssistantIntent, VisionAssistantEffect>(
    initialState = VisionAssistantState(classifierAvailable = false),
) {
    init {
        updateState { copy(classifierAvailable = imageClassifier.isAvailable) }
    }

    override fun handleIntent(intent: VisionAssistantIntent) {
        when (intent) {
            is VisionAssistantIntent.PhotoCaptured -> {
                updateState { copy(capturedImageBytes = intent.bytes, phase = VisionPhase.Classifying, error = null) }
                analyzeAndAsk(intent.bytes)
            }
            is VisionAssistantIntent.UpdateQuestion -> {
                updateState { copy(userQuestion = intent.question) }
            }
            VisionAssistantIntent.Reset -> {
                updateState {
                    VisionAssistantState(classifierAvailable = imageClassifier.isAvailable)
                }
            }
            VisionAssistantIntent.ClearError -> updateState { copy(error = null) }
        }
    }

    private fun analyzeAndAsk(imageBytes: ByteArray) {
        viewModelScope.launch {
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

            val preparedPrompt = promptPipeline.preparePrompt(
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
                            prompt = preparedPrompt,
                            industry = PromptIndustry.IndustrialMaintenance,
                            model = model.runtimeModel,
                            backend = backend.runtimeBackend,
                            maxTokens = config.maxTokens,
                            temperature = config.temperature,
                        )
                    )
                }
            }

            result.onSuccess { generation ->
                updateState { copy(phase = VisionPhase.Done, response = generation.text) }
            }.onFailure { e ->
                updateState { copy(phase = VisionPhase.Idle, error = "Generation failed: ${e.message}") }
            }
        }
    }
}
