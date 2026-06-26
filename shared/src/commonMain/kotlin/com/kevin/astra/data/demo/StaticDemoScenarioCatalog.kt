package com.kevin.astra.data.demo

import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.demo.DemoScenario
import com.kevin.astra.domain.demo.DemoScenarioCatalog

class StaticDemoScenarioCatalog : DemoScenarioCatalog {
    private val allScenarios = listOf(
        // Industrial Maintenance
        DemoScenario(
            id = "ind-01",
            title = "Restart Pump A",
            industry = PromptIndustry.IndustrialMaintenance,
            description = "Restart procedure after emergency shutdown",
            prompt = "What are the safe steps to restart Pump A following an emergency shutdown? Include pressure verification requirements.",
            expectedValue = "Critical safety procedure for resuming operations without damaging hardware."
        ),
        DemoScenario(
            id = "ind-02",
            title = "Conveyor Vibration",
            industry = PromptIndustry.IndustrialMaintenance,
            description = "Diagnose abnormal motor vibration",
            prompt = "The conveyor motor is exhibiting 15mm/s RMS vibration. Is this within safe limits for a Class II machine, and what are the likely causes?",
            expectedValue = "Diagnostic assistance for predictive maintenance."
        ),
        // Aerospace
        DemoScenario(
            id = "aero-01",
            title = "Hydraulic Checklist",
            industry = PromptIndustry.Aerospace,
            description = "Review pre-flight hydraulic system",
            prompt = "Review the pre-flight hydraulic system checklist for a standard commercial aircraft. Highlight critical pressure points.",
            expectedValue = "Ensures pre-flight compliance with local AI verification."
        ),
        DemoScenario(
            id = "aero-02",
            title = "Sensor Inconsistency",
            industry = PromptIndustry.Aerospace,
            description = "Diagnose cockpit sensor anomaly",
            prompt = "The pilot reports a 5% discrepancy between Pitot System A and B. What is the standard troubleshooting protocol for this altitude?",
            expectedValue = "Rapid decision support for cockpit crew."
        ),
        // Defense
        DemoScenario(
            id = "def-01",
            title = "Offline Operation",
            industry = PromptIndustry.Defense,
            description = "Operate in secure offline mode",
            prompt = "List the operational constraints when deploying ASTRA in a Faraday-shielded environment for tactical planning.",
            expectedValue = "Demonstrates security of local-only data processing."
        ),
        DemoScenario(
            id = "def-02",
            title = "Classified Summary",
            industry = PromptIndustry.Defense,
            description = "Safe maintenance summary",
            prompt = "Summarize the maintenance logs for Zone 4 without exposing individual engineer IDs or specific coordinate data.",
            expectedValue = "Privacy-preserving data summarization on the edge."
        ),
        // Energy
        DemoScenario(
            id = "nrg-01",
            title = "Isolated Site Anomaly",
            industry = PromptIndustry.Energy,
            description = "Pressure anomaly on isolated site",
            prompt = "A pressure drop of 2 bar detected on the secondary loop at the solar thermal plant. We are currently offline. Analyze potential leaks.",
            expectedValue = "Reliable analysis in remote areas without connectivity."
        ),
        DemoScenario(
            id = "nrg-02",
            title = "Turbine Inspection",
            industry = PromptIndustry.Energy,
            description = "Prepare turbine checklist",
            prompt = "Generate a 10-point inspection checklist for a wind turbine blade after a lightning strike event.",
            expectedValue = "Standardized safety inspections for field technicians."
        ),
        // Healthcare
        DemoScenario(
            id = "hlth-01",
            title = "Medical Device Alarm",
            industry = PromptIndustry.Healthcare,
            description = "Troubleshoot portable device",
            prompt = "Portable Ventilator V-4 is showing 'Error 402 - Low Oxygen Flow'. What are the immediate troubleshooting steps?",
            expectedValue = "Life-critical technical support at the point of care."
        ),
        DemoScenario(
            id = "hlth-02",
            title = "Privacy Verification",
            industry = PromptIndustry.Healthcare,
            description = "Local handling of patient data",
            prompt = "Verify that my current session parameters ensure no patient data is transmitted to external servers during this diagnostic assist.",
            expectedValue = "Compliance and data sovereignty for sensitive medical environments."
        )
    )

    override fun scenarios(): List<DemoScenario> = allScenarios

    override fun scenariosForIndustry(industry: PromptIndustry): List<DemoScenario> =
        allScenarios.filter { it.industry == industry }

    override fun scenarioById(id: String): DemoScenario? =
        allScenarios.find { it.id == id }
}
