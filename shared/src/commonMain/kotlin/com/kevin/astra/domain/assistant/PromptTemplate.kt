package com.kevin.astra.domain.assistant

data class PromptTemplate(
    val id: String,
    val label: String,
    val icon: String,
    val promptText: String,
)

object StaticPromptTemplateCatalog {
    val all: List<PromptTemplate> = listOf(
        PromptTemplate(
            id = "summarize",
            label = "Summarize",
            icon = "◈",
            promptText = "Summarize the following information in clear, concise bullet points suitable for a field engineer: ",
        ),
        PromptTemplate(
            id = "explain",
            label = "Explain",
            icon = "◎",
            promptText = "Explain the following concept or procedure in simple terms for an operator with basic technical knowledge: ",
        ),
        PromptTemplate(
            id = "translate",
            label = "Translate",
            icon = "◇",
            promptText = "Translate the following technical content into plain language, avoiding jargon: ",
        ),
        PromptTemplate(
            id = "generate-code",
            label = "Generate Code",
            icon = "◆",
            promptText = "Generate a code snippet or configuration script for the following task: ",
        ),
        PromptTemplate(
            id = "brainstorm",
            label = "Brainstorm",
            icon = "◉",
            promptText = "Brainstorm possible causes and solutions for the following operational issue: ",
        ),
        PromptTemplate(
            id = "proofread",
            label = "Proofread",
            icon = "◐",
            promptText = "Review and improve the following technical document or procedure for clarity and accuracy: ",
        ),
    )
}
