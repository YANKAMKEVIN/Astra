package com.kevin.astra.presentation.assistant

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.mvi.AstraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AssistantViewModel(
    private val timestampProvider: () -> String = ::currentGenerationTimestamp,
    private val generationScope: CoroutineScope? = null,
) : AstraViewModel<AssistantState, AssistantIntent, AssistantEffect>(
    initialState = AssistantState(),
) {
    override fun handleIntent(intent: AssistantIntent) {
        when (intent) {
            is AssistantIntent.UpdateQuestion -> updateState {
                copy(question = intent.question)
            }

            is AssistantIntent.SelectIndustry -> updateState {
                copy(selectedIndustry = intent.industry)
            }

            AssistantIntent.AskQuestion -> askQuestion()
            AssistantIntent.ClearConversation -> updateState {
                copy(
                    question = "",
                    response = null,
                    isGenerating = false,
                    generationTimestamp = null,
                )
            }
        }
    }

    private fun askQuestion() {
        val snapshot = state.value
        if (snapshot.isGenerating) return

        if (snapshot.question.isBlank()) {
            emitEffect(AssistantEffect.ShowError("Enter a critical operation question before asking ASTRA."))
            return
        }

        (generationScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isGenerating = true,
                    response = null,
                    generationTimestamp = null,
                )
            }

            delay(1_000)

            updateState {
                copy(
                    isGenerating = false,
                    response = AssistantResponse(
                        title = "Emergency restart procedure",
                        body = buildMockResponse(snapshot.selectedIndustry),
                    ),
                    generationTimestamp = timestampProvider(),
                )
            }
        }
    }
}

private fun buildMockResponse(industry: AssistantIndustry): String {
    val industryContext = when (industry) {
        AssistantIndustry.IndustrialMaintenance -> "industrial maintenance shift lead"
        AssistantIndustry.Aerospace -> "aerospace ground operations engineer"
        AssistantIndustry.Defense -> "defense systems operator"
        AssistantIndustry.Energy -> "energy infrastructure supervisor"
        AssistantIndustry.Healthcare -> "healthcare facility operations engineer"
    }

    return """
        Mock local response for the $industryContext.

        1. Verify that the emergency stop has been released.

        2. Check the pressure level and confirm it is within the approved operating range.

        3. Reset the protection relay from the local control panel.

        4. Restart Pump A using Local Mode and keep remote commands disabled until stabilization.

        5. Monitor operating pressure, vibration and thermal readings for five minutes.

        Status:
        Pump restarted successfully. Continue local supervision and record the intervention in the shift log.
    """.trimIndent()
}

@OptIn(ExperimentalTime::class)
private fun currentGenerationTimestamp(): String = Clock.System.now().toString()
