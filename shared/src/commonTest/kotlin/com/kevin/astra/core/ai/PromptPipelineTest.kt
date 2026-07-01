package com.kevin.astra.core.ai

import com.kevin.astra.data.ai.DefaultModelCatalog
import kotlin.test.Test
import kotlin.test.assertContains

class PromptPipelineTest {
    private val catalog = DefaultModelCatalog()
    private val pipeline = DefaultPromptPipeline(DefaultPromptBuilder())

    @Test
    fun preparesAssistantPromptWithRequiredSections() {
        val parts = pipeline.preparePrompt(
            PromptBuildRequest(
                engineerQuestion = "How should we restart Pump A?",
                selectedIndustry = PromptIndustry.IndustrialMaintenance,
                selectedModel = catalog.currentModel(),
            ),
        )

        assertContains(parts.fullPrompt, "System role")
        assertContains(parts.fullPrompt, "Industry persona")
        assertContains(parts.fullPrompt, "User request")
        assertContains(parts.fullPrompt, "Context")
        assertContains(parts.fullPrompt, "Response formatting instructions")
        assertContains(parts.fullPrompt, "industrial maintenance assistant")
        assertContains(parts.fullPrompt, "How should we restart Pump A?")
        assertContains(parts.systemPrompt, "industrial maintenance assistant")
        assertContains(parts.userMessage, "How should we restart Pump A?")
    }

    @Test
    fun usesDocumentQaTemplateWhenContextIsProvided() {
        val parts = pipeline.preparePrompt(
            PromptBuildRequest(
                engineerQuestion = "Which checks are needed?",
                selectedIndustry = PromptIndustry.Energy,
                selectedModel = catalog.currentModel(),
                extractedDocumentContext = "Pump Restart Procedure: verify pressure before restart.",
            ),
        )

        assertContains(parts.fullPrompt, "local document question-answering assistant")
        assertContains(parts.fullPrompt, "Pump Restart Procedure")
        assertContains(parts.fullPrompt, "If the context does not contain the answer")
        assertContains(parts.userMessage, "Pump Restart Procedure")
    }
}
