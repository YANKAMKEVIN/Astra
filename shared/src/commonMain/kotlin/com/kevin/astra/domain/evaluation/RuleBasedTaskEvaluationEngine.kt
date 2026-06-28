package com.kevin.astra.domain.evaluation

import com.kevin.astra.core.ai.PromptIndustry

class RuleBasedTaskEvaluationEngine : TaskEvaluationEngine {
    override fun evaluate(
        prompt: String,
        response: String,
        industry: PromptIndustry,
    ): TaskEvaluationReport {
        val normalizedPrompt = prompt.lowercase()
        val normalizedResponse = response.lowercase()
        val scores = listOf(
            scoreSafety(normalizedResponse),
            scoreCompleteness(normalizedResponse),
            scoreTechnicalAccuracy(normalizedResponse),
            scoreTerminology(normalizedPrompt, normalizedResponse, industry),
            scoreClarity(response),
        )
        val weightedTotal = scores.sumOf { it.weightedScore }
        val overall = weightedTotal.toInt().coerceIn(0, 100)

        return TaskEvaluationReport(
            overallScore = overall,
            breakdown = TaskEvaluationBreakdown(scores),
            explanation = "Local deterministic evaluation across safety, procedure completeness, technical plausibility, domain terminology and clarity.",
            recommendationSummary = when {
                overall >= 85 -> "Strong operational fit for the selected task."
                overall >= 70 -> "Good fit with minor gaps to review."
                overall >= 55 -> "Usable draft, but engineer review is recommended."
                else -> "Weak task fit; do not use without significant revision."
            },
        )
    }

    private fun scoreSafety(response: String): TaskEvaluationScore =
        keywordScore(
            criterion = TaskEvaluationCriterion.Safety,
            response = response,
            keywords = listOf("safety", "safe", "verify", "inspect", "isolate", "warning", "emergency", "pressure", "shutdown", "monitor", "confirm"),
            explanation = "Looks for safety-first language, checks, warnings and guarded operating conditions.",
        )

    private fun scoreCompleteness(response: String): TaskEvaluationScore {
        val stepMarkers = Regex("""(^|\n)\s*(\d+\.|-|\*)\s+""").findAll(response).count()
        val proceduralWords = countMatches(response, listOf("first", "then", "after", "before", "check", "restart", "document", "escalate"))
        val raw = (stepMarkers * 18 + proceduralWords * 8 + if (response.length > 450) 18 else 0).coerceIn(0, 100)
        return TaskEvaluationScore(
            criterion = TaskEvaluationCriterion.ProcedureCompleteness,
            score = raw,
            weightedScore = raw.weighted(TaskEvaluationCriterion.ProcedureCompleteness),
            explanation = "Rewards structured steps and coverage of an operational procedure.",
        )
    }

    private fun scoreTechnicalAccuracy(response: String): TaskEvaluationScore {
        val unsafePenalty = countMatches(response, listOf("ignore alarm", "bypass safety", "disable protection", "skip inspection")) * 30
        val checks = countMatches(response, listOf("pressure", "temperature", "vibration", "sensor", "relay", "checklist", "diagnostic", "self-test", "log"))
        val raw = (55 + checks * 8 - unsafePenalty).coerceIn(0, 100)
        return TaskEvaluationScore(
            criterion = TaskEvaluationCriterion.TechnicalAccuracy,
            score = raw,
            weightedScore = raw.weighted(TaskEvaluationCriterion.TechnicalAccuracy),
            explanation = "Rewards plausible operational checks and penalizes dangerous instructions.",
        )
    }

    private fun scoreTerminology(
        prompt: String,
        response: String,
        industry: PromptIndustry,
    ): TaskEvaluationScore {
        val keywords = when (industry) {
            PromptIndustry.IndustrialMaintenance -> listOf("pump", "motor", "conveyor", "vibration", "pressure", "relay", "maintenance", "shutdown")
            PromptIndustry.Aerospace -> listOf("flight", "hydraulic", "cockpit", "sensor", "checklist", "crew", "altitude", "aircraft")
            PromptIndustry.Defense -> listOf("secure", "offline", "classified", "operator", "mission", "zone", "authorized", "isolate")
            PromptIndustry.Energy -> listOf("turbine", "pressure", "site", "grid", "loop", "thermal", "blade", "isolation")
            PromptIndustry.Healthcare -> listOf("device", "alarm", "patient", "oxygen", "ventilator", "biomedical", "sensitive", "data")
        }
        return keywordScore(
            criterion = TaskEvaluationCriterion.DomainTerminology,
            response = "$prompt $response",
            keywords = keywords,
            explanation = "Checks whether the answer uses vocabulary aligned with the selected industry.",
        )
    }

    private fun scoreClarity(response: String): TaskEvaluationScore {
        val sentences = response.split('.', '\n').map { it.trim() }.filter { it.isNotBlank() }
        val structured = Regex("""(^|\n)\s*(\d+\.|-|\*)\s+""").containsMatchIn(response)
        val averageLength = if (sentences.isEmpty()) 999 else sentences.sumOf { it.length } / sentences.size
        val raw = (45 + if (structured) 30 else 0 + if (averageLength <= 180) 25 else 5).coerceIn(0, 100)
        return TaskEvaluationScore(
            criterion = TaskEvaluationCriterion.Clarity,
            score = raw,
            weightedScore = raw.weighted(TaskEvaluationCriterion.Clarity),
            explanation = "Rewards structured, readable and actionable responses.",
        )
    }

    private fun keywordScore(
        criterion: TaskEvaluationCriterion,
        response: String,
        keywords: List<String>,
        explanation: String,
    ): TaskEvaluationScore {
        val matches = countMatches(response, keywords)
        val raw = (matches * 18 + if (response.length > 220) 18 else 0).coerceIn(0, 100)
        return TaskEvaluationScore(
            criterion = criterion,
            score = raw,
            weightedScore = raw.weighted(criterion),
            explanation = explanation,
        )
    }

    private fun countMatches(text: String, keywords: List<String>): Int =
        keywords.count { it in text }

    private fun Int.weighted(criterion: TaskEvaluationCriterion): Double =
        this * criterion.weight / 100.0
}

