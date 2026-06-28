package com.kevin.astra.domain.evaluation

import com.kevin.astra.core.ai.PromptIndustry

enum class TaskEvaluationCriterion(
    val label: String,
    val weight: Int,
) {
    Safety("Safety", 30),
    ProcedureCompleteness("Procedure Completeness", 25),
    TechnicalAccuracy("Technical Accuracy", 20),
    DomainTerminology("Domain Terminology", 15),
    Clarity("Clarity", 10),
}

data class TaskEvaluationScore(
    val criterion: TaskEvaluationCriterion,
    val score: Int,
    val weightedScore: Double,
    val explanation: String,
)

data class TaskEvaluationBreakdown(
    val scores: List<TaskEvaluationScore>,
) {
    fun scoreFor(criterion: TaskEvaluationCriterion): TaskEvaluationScore =
        scores.first { it.criterion == criterion }
}

data class TaskEvaluationReport(
    val overallScore: Int,
    val breakdown: TaskEvaluationBreakdown,
    val explanation: String,
    val recommendationSummary: String,
)

interface TaskEvaluationEngine {
    fun evaluate(
        prompt: String,
        response: String,
        industry: PromptIndustry,
    ): TaskEvaluationReport
}

