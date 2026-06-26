package com.kevin.astra.domain.demo

import com.kevin.astra.core.ai.PromptIndustry

data class DemoScenario(
    val id: String,
    val title: String,
    val industry: PromptIndustry,
    val description: String,
    val prompt: String,
    val expectedValue: String,
)

interface DemoScenarioCatalog {
    fun scenarios(): List<DemoScenario>
    fun scenariosForIndustry(industry: PromptIndustry): List<DemoScenario>
    fun scenarioById(id: String): DemoScenario?
}
