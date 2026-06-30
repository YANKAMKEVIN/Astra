package com.kevin.astra.core.ai

import com.kevin.astra.domain.assistant.StreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class MockInferenceEngine(
    private val timestampProvider: () -> String = ::currentGenerationTimestamp,
    private val streamDelayMs: Long = 40L,
) : InferenceEngine {
    override suspend fun generate(request: PromptRequest): GenerationResult {
        delay(1_000)

        return GenerationResult(
            text = buildResponse(request),
            metrics = buildMetrics(request.industry),
            model = request.model,
            backend = request.backend,
            generatedAt = timestampProvider(),
            runtimeInfo = GenerationRuntimeInfo(
                mode = RuntimeMode.Simulated,
                inferenceLatencyMillis = 1_200,
                totalExecutionTimeMillis = 1_200,
            ),
        )
    }

    override fun generateStream(request: PromptRequest): Flow<StreamEvent> = flow {
        val fullText = buildResponse(request)
        val words = fullText.split(" ")
        val startMs = kotlinx.coroutines.currentCoroutineContext()
            .let { System.currentTimeMillis() }

        words.forEachIndexed { index, word ->
            delay(streamDelayMs)
            val token = if (index == 0) word else " $word"
            emit(StreamEvent.Token(token))
        }

        val elapsed = System.currentTimeMillis() - startMs
        val metrics = buildMetrics(request.industry)
        emit(
            StreamEvent.Complete(
                GenerationResult(
                    text = fullText,
                    metrics = metrics.copy(latencyMillis = elapsed),
                    model = request.model,
                    backend = request.backend,
                    generatedAt = timestampProvider(),
                    runtimeInfo = GenerationRuntimeInfo(
                        mode = RuntimeMode.Simulated,
                        inferenceLatencyMillis = elapsed,
                        totalExecutionTimeMillis = elapsed,
                    ),
                ),
            ),
        )
    }

    private fun buildResponse(request: PromptRequest): String {
        val normalizedPrompt = request.prompt.trim()

        return when (request.industry) {
            PromptIndustry.IndustrialMaintenance -> """
                Emergency restart procedure

                Context:
                ASTRA is operating in Industrial Maintenance mode and treating the prompt as a critical equipment intervention:
                "$normalizedPrompt"

                1. Verify that the emergency stop has been released.

                2. Check the pressure level and confirm it is within the approved operating range.

                3. Reset the protection relay from the local control panel.

                4. Restart Pump A using Local Mode and keep remote commands disabled until stabilization.

                5. Monitor operating pressure, vibration and thermal readings for five minutes.

                Status:
                Pump restarted successfully. Continue local supervision and record the intervention in the shift log.
            """.trimIndent()

            PromptIndustry.Aerospace -> """
                Cockpit checklist assistance

                Context:
                ASTRA is operating in Aerospace mode and treating the prompt as a safety-critical checklist review:
                "$normalizedPrompt"

                1. Freeze the current checklist step and confirm aircraft state with the crew lead.

                2. Cross-check hydraulic, electrical and warning panel indicators before taking action.

                3. Apply the approved abnormal procedure from the local operations manual.

                4. Require verbal confirmation before resetting any protected control.

                5. Log the event and maintain monitoring until all indicators remain stable.

                Status:
                Checklist assistance completed in simulated offline mode.
            """.trimIndent()

            PromptIndustry.Defense -> """
                Secure offline procedure assistance

                Context:
                ASTRA is operating in Defense mode and treating the prompt as a secure local procedure:
                "$normalizedPrompt"

                1. Confirm the operator role and isolate the workstation from non-approved channels.

                2. Validate the mission system state against the local operating baseline.

                3. Execute the recovery sequence using only authorized offline documentation.

                4. Record each control action with timestamp and operator confirmation.

                5. Keep the system in supervised mode until the incident commander clears escalation.

                Status:
                Secure mock procedure generated without network access.
            """.trimIndent()

            PromptIndustry.Energy -> """
                Site incident diagnosis

                Context:
                ASTRA is operating in Energy mode and treating the prompt as an infrastructure incident:
                "$normalizedPrompt"

                1. Stabilize the site by confirming isolation boundaries and active alarms.

                2. Review pressure, temperature and load telemetry from the local panel.

                3. Identify whether the incident is mechanical, electrical or process-related.

                4. Apply the approved restart or shutdown sequence for the affected asset.

                5. Monitor downstream impact for five minutes before returning to normal operation.

                Status:
                Incident diagnosis completed with mock local telemetry assumptions.
            """.trimIndent()

            PromptIndustry.Healthcare -> """
                Medical device troubleshooting

                Context:
                ASTRA is operating in Healthcare mode and treating the prompt as facility equipment troubleshooting:
                "$normalizedPrompt"

                1. Confirm the device is not actively supporting a patient before intervention.

                2. Check power, alarm history and local diagnostic indicators.

                3. Follow the manufacturer-approved reset sequence from the on-site procedure.

                4. Run the built-in self-test and verify that all safety checks pass.

                5. Escalate to biomedical engineering if the alarm reappears.

                Status:
                Troubleshooting guidance generated in simulated offline mode.
            """.trimIndent()
        }
    }

    private fun buildMetrics(industry: PromptIndustry): GenerationMetrics {
        val tokenCount = when (industry) {
            PromptIndustry.IndustrialMaintenance -> 138
            PromptIndustry.Aerospace -> 126
            PromptIndustry.Defense -> 132
            PromptIndustry.Energy -> 124
            PromptIndustry.Healthcare -> 118
        }

        return GenerationMetrics(
            latencyMillis = 1_200,
            timeToFirstTokenMillis = 320,
            tokensGenerated = tokenCount,
            tokensPerSecond = 18,
            memoryUsageMb = 384,
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun currentGenerationTimestamp(): String = Clock.System.now().toString()
