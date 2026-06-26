package com.kevin.astra.core.ai

import com.kevin.astra.data.ai.DefaultModelCatalog
import kotlin.test.Test
import kotlin.test.assertContains

class PromptPipelineTest {
    private val catalog = DefaultModelCatalog()
    private val pipeline = DefaultPromptPipeline(DefaultPromptBuilder())

    @Test
    fun preparesAssistantPromptWithRequiredSections() {
        val prompt = pipeline.preparePrompt(
            PromptBuildRequest(
                engineerQuestion = "How should we restart Pump A?",
                selectedIndustry = PromptIndustry.IndustrialMaintenance,
                selectedModel = catalog.currentModel(),
            ),
        )

        assertContains(prompt, "System role")
        assertContains(prompt, "Industry persona")
        assertContains(prompt, "User request")
        assertContains(prompt, "Context")
        assertContains(prompt, "Response formatting instructions")
        assertContains(prompt, "industrial maintenance assistant")
        assertContains(prompt, "How should we restart Pump A?")
    }

    @Test
    fun usesDocumentQaTemplateWhenContextIsProvided() {
        val prompt = pipeline.preparePrompt(
            PromptBuildRequest(
                engineerQuestion = "Which checks are needed?",
                selectedIndustry = PromptIndustry.Energy,
                selectedModel = catalog.currentModel(),
                extractedDocumentContext = "Pump Restart Procedure: verify pressure before restart.",
            ),
        )

        assertContains(prompt, "local document question-answering assistant")
        assertContains(prompt, "Pump Restart Procedure")
        assertContains(prompt, "If the context does not contain the answer")
    }
}
