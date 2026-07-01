package com.kevin.astra.presentation.assistant

import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.assistant.PromptTemplate
import com.kevin.astra.domain.demo.DemoScenario
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.voice.SpeechRecognitionState
import com.kevin.astra.domain.vision.ImageClassificationResult

enum class AssistantIndustry(val label: String) {
    IndustrialMaintenance("Industrial Maintenance"),
    Aerospace("Aerospace"),
    Defense("Defense"),
    Energy("Energy"),
    Healthcare("Healthcare"),
}

fun AssistantIndustry.toPromptIndustry(): PromptIndustry = when (this) {
    AssistantIndustry.IndustrialMaintenance -> PromptIndustry.IndustrialMaintenance
    AssistantIndustry.Aerospace -> PromptIndustry.Aerospace
    AssistantIndustry.Defense -> PromptIndustry.Defense
    AssistantIndustry.Energy -> PromptIndustry.Energy
    AssistantIndustry.Healthcare -> PromptIndustry.Healthcare
}

fun PromptIndustry.toAssistantIndustry(): AssistantIndustry = when (this) {
    PromptIndustry.IndustrialMaintenance -> AssistantIndustry.IndustrialMaintenance
    PromptIndustry.Aerospace -> AssistantIndustry.Aerospace
    PromptIndustry.Defense -> AssistantIndustry.Defense
    PromptIndustry.Energy -> AssistantIndustry.Energy
    PromptIndustry.Healthcare -> AssistantIndustry.Healthcare
}

data class AssistantMetrics(
    val model: String = "—",
    val backend: String = "—",
    val runtimeMode: String = "Pending",
    val latency: String = "—",
    val tokensPerSecond: String = "—",
    val timeToFirstToken: String = "—",
    val tokensGenerated: String = "—",
    val memoryUsage: String = "—",
    val modelLoadTime: String = "—",
    val totalExecutionTime: String = "—",
    val fallbackReason: String? = null,
)

data class AssistantResponse(
    val title: String,
    val body: String,
)

enum class ChatRole { User, Assistant }

data class ChatBubble(
    val id: String,
    val role: ChatRole,
    val text: String,
    val metrics: AssistantMetrics? = null,
)

enum class AttachmentStatus { Idle, Indexing, Ready, Error }

data class AttachedPdf(
    val fileName: String,
    val pageCount: Int,
    val extractedContext: String,
    val status: AttachmentStatus = AttachmentStatus.Ready,
)

data class AttachedImage(
    val bytes: ByteArray,
    val classification: ImageClassificationResult?,
    val status: AttachmentStatus = AttachmentStatus.Ready,
) {
    override fun equals(other: Any?) = other is AttachedImage && bytes.contentEquals(other.bytes)
    override fun hashCode() = bytes.contentHashCode()
}

data class AssistantState(
    val selectedIndustry: AssistantIndustry? = null,
    val question: String = "",
    val streamingText: String = "",
    val isGenerating: Boolean = false,
    val metrics: AssistantMetrics = AssistantMetrics(),
    val messages: List<ChatBubble> = emptyList(),
    val availableScenarios: List<DemoScenario> = emptyList(),
    val promptTemplates: List<PromptTemplate> = emptyList(),
    val activeTemplate: PromptTemplate? = null,
    val installedModels: List<LocalModel> = emptyList(),
    val sessionModel: LocalModel? = null,
    val attachedPdf: AttachedPdf? = null,
    val attachedImage: AttachedImage? = null,
    val voiceState: SpeechRecognitionState = SpeechRecognitionState.Idle,
    val recentHistory: List<ChatConversation> = emptyList(),
    val error: String? = null,
) : AstraState {
    val canAsk: Boolean
        get() = question.isNotBlank() && !isGenerating &&
            attachedPdf?.status != AttachmentStatus.Indexing &&
            attachedImage?.status != AttachmentStatus.Indexing

    val isStreaming: Boolean
        get() = isGenerating && streamingText.isNotEmpty()

    val isListening: Boolean
        get() = voiceState is SpeechRecognitionState.Listening ||
            voiceState is SpeechRecognitionState.Partial

    val hasAttachment: Boolean
        get() = attachedPdf != null || attachedImage != null

    val isEmpty: Boolean
        get() = messages.isEmpty() && !isGenerating
}

sealed interface AssistantIntent : AstraIntent {
    data class UpdateQuestion(val question: String) : AssistantIntent
    data class SelectIndustry(val industry: AssistantIndustry?) : AssistantIntent
    data class SelectScenario(val scenario: DemoScenario) : AssistantIntent
    data class SelectTemplate(val template: PromptTemplate) : AssistantIntent
    data class SelectSessionModel(val modelId: String) : AssistantIntent
    data class PdfAttached(val bytes: ByteArray, val fileName: String) : AssistantIntent
    data class ImageAttached(val bytes: ByteArray) : AssistantIntent
    data object RemovePdf : AssistantIntent
    data object RemoveImage : AssistantIntent
    data object ToggleVoiceInput : AssistantIntent
    data class LoadConversation(val id: String) : AssistantIntent
    data class ShareBubble(val bubbleId: String, val format: ExportFormat) : AssistantIntent
    data class RemoveMessage(val bubbleId: String) : AssistantIntent
    data object AskQuestion : AssistantIntent
    data object ClearConversation : AssistantIntent
}

sealed interface AssistantEffect : AstraEffect {
    data class ShowError(val message: String) : AssistantEffect
}
