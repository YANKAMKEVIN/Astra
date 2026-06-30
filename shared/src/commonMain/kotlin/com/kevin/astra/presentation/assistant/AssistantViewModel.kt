package com.kevin.astra.presentation.assistant

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.assistant.StreamEvent
import com.kevin.astra.domain.assistant.StaticPromptTemplateCatalog
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage
import com.kevin.astra.domain.history.ConversationRepository
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val demoScenarioCatalog: DemoScenarioCatalog,
    private val notificationService: NotificationService,
    private val conversationRepository: ConversationRepository,
    private val generationScope: CoroutineScope? = null,
) : AstraViewModel<AssistantState, AssistantIntent, AssistantEffect>(
    initialState = AssistantState(
        availableScenarios = demoScenarioCatalog.scenarios(),
        promptTemplates = StaticPromptTemplateCatalog.all,
        installedModels = modelCatalog.installedModels(),
        sessionModel = modelCatalog.currentModel(),
    ),
) {
    override fun handleIntent(intent: AssistantIntent) {
        when (intent) {
            is AssistantIntent.UpdateQuestion -> updateState {
                copy(question = intent.question, error = null, activeTemplate = null)
            }

            is AssistantIntent.SelectIndustry -> updateState {
                copy(
                    selectedIndustry = intent.industry,
                    availableScenarios = demoScenarioCatalog.scenariosForIndustry(intent.industry.toPromptIndustry()),
                    error = null,
                )
            }

            is AssistantIntent.SelectScenario -> updateState {
                copy(
                    question = intent.scenario.prompt,
                    selectedIndustry = intent.scenario.industry.toAssistantIndustry(),
                    availableScenarios = demoScenarioCatalog.scenariosForIndustry(intent.scenario.industry),
                    activeTemplate = null,
                    error = null,
                )
            }

            is AssistantIntent.SelectTemplate -> updateState {
                copy(
                    question = intent.template.promptText,
                    activeTemplate = intent.template,
                    error = null,
                )
            }

            is AssistantIntent.SelectSessionModel -> {
                val model = modelCatalog.modelById(intent.modelId) ?: return
                updateState { copy(sessionModel = model) }
            }

            AssistantIntent.AskQuestion -> askQuestion()

            AssistantIntent.ClearConversation -> updateState {
                copy(
                    question = "",
                    response = null,
                    isGenerating = false,
                    generationTimestamp = null,
                    metrics = AssistantMetrics(),
                    activeTemplate = null,
                )
            }
        }
    }

    private fun saveConversation(question: String, result: GenerationResult) {
        val snap = state.value
        val conversation = ChatConversation(
            id = result.generatedAt.replace(Regex("[^0-9]"), ""),
            title = question.take(60).ifBlank { "Conversation" },
            modelName = result.model.label,
            backendName = result.backend.label,
            industry = snap.selectedIndustry.label,
            messages = listOf(
                ChatMessage(role = "user", content = question, timestamp = result.generatedAt),
                ChatMessage(role = "assistant", content = result.text, timestamp = result.generatedAt),
            ),
            createdAt = result.generatedAt,
        )
        conversationRepository.save(conversation)
    }

    private fun askQuestion() {
        val snapshot = state.value
        if (snapshot.isGenerating) return

        if (snapshot.question.isBlank()) {
            updateState { copy(error = "Enter a critical operation question before asking ASTRA.") }
            return
        }

        (generationScope ?: viewModelScope).launch {
            val configuration = aiConfigurationRepository.getConfiguration()

            val selectedModel = snapshot.sessionModel
                ?: modelCatalog.modelById(configuration.selectedModelId)
            val selectedBackend = backendCatalog.backendById(configuration.selectedBackendId)

            if (selectedModel == null || selectedBackend == null) {
                updateState { copy(error = "Invalid AI configuration. Please check Settings.") }
                return@launch
            }

            val industry = snapshot.selectedIndustry.toPromptIndustry()
            val preparedPrompt = promptPipeline.preparePrompt(
                PromptBuildRequest(
                    engineerQuestion = snapshot.question,
                    selectedIndustry = industry,
                    selectedModel = selectedModel,
                ),
            )

            updateState {
                copy(
                    isGenerating = true,
                    streamingText = "",
                    response = null,
                    generationTimestamp = null,
                    installedModels = modelCatalog.installedModels(),
                )
            }

            val promptRequest = PromptRequest(
                prompt = preparedPrompt,
                industry = industry,
                model = selectedModel.runtimeModel,
                backend = selectedBackend.runtimeBackend,
                maxTokens = configuration.maxTokens,
                temperature = configuration.temperature,
            )

            var finalResult: com.kevin.astra.core.ai.GenerationResult? = null
            askLocalAssistant.stream(promptRequest)
                .flowOn(Dispatchers.Default)
                .collect { event ->
                    when (event) {
                        is StreamEvent.Token -> updateState { copy(streamingText = streamingText + event.text) }
                        is StreamEvent.Complete -> finalResult = event.result
                        is StreamEvent.Error -> updateState { copy(isGenerating = false, error = event.message, streamingText = "") }
                    }
                }

            val result = finalResult ?: return@launch
            updateState {
                copy(
                    isGenerating = false,
                    streamingText = "",
                    response = result.toAssistantResponse(),
                    generationTimestamp = result.generatedAt,
                    metrics = result.toAssistantMetrics(),
                )
            }

            saveConversation(snapshot.question, result)

            notificationService.showNotification(
                title = "AI Analysis Ready",
                message = "ASTRA has completed the mission-critical analysis.",
                targetDestination = AstraDestination.Assistant,
            )
        }
    }
}

private fun GenerationResult.toAssistantResponse(): AssistantResponse =
    AssistantResponse(
        title = text.lineSequence().firstOrNull().orEmpty().ifBlank { "ASTRA response" },
        body = text,
    )

private fun GenerationResult.toAssistantMetrics(): AssistantMetrics =
    AssistantMetrics(
        model = model.label,
        backend = backend.label,
        runtimeMode = runtimeInfo.mode.label,
        latency = "${metrics.latencyMillis / 1_000.0} s",
        tokensPerSecond = if (metrics.tokensPerSecond > 0) metrics.tokensPerSecond.toString() else "N/A",
        timeToFirstToken = "${metrics.timeToFirstTokenMillis} ms",
        tokensGenerated = metrics.tokensGenerated.toString(),
        memoryUsage = "${metrics.memoryUsageMb} MB",
        modelLoadTime = "${runtimeInfo.modelLoadTimeMillis} ms",
        totalExecutionTime = "${runtimeInfo.totalExecutionTimeMillis / 1_000.0} s",
        fallbackReason = runtimeInfo.fallbackReason,
    )
