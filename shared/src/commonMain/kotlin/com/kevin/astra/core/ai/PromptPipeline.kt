package com.kevin.astra.core.ai

enum class PromptTemplateKey {
    GeneralAssistant,
    IndustrialMaintenance,
    Aerospace,
    Defense,
    Energy,
    Healthcare,
    DocumentQa,
}

data class PromptTemplate(
    val key: PromptTemplateKey,
    val systemRole: String,
    val industryPersona: String,
    val systemInstructions: List<String>,
    val responseFormattingInstructions: List<String>,
)

data class PromptBuildRequest(
    val engineerQuestion: String,
    val selectedIndustry: PromptIndustry?,
    val selectedModel: LocalModel,
    val extractedDocumentContext: String? = null,
)

data class PromptParts(
    val systemPrompt: String,
    val userMessage: String,
    val fullPrompt: String,
)

interface PromptBuilder {
    fun buildParts(
        request: PromptBuildRequest,
        template: PromptTemplate,
    ): PromptParts
}

interface PromptPipeline {
    fun preparePrompt(request: PromptBuildRequest): PromptParts
}

class DefaultPromptBuilder : PromptBuilder {
    override fun buildParts(
        request: PromptBuildRequest,
        template: PromptTemplate,
    ): PromptParts {
        val systemPrompt = buildString {
            append(template.systemRole)
            append("\n\n")
            append(template.industryPersona)
            append("\n\n")
            append(template.systemInstructions.joinToString(separator = "\n") { "- $it" })
            append("\n\n")
            append(template.responseFormattingInstructions.joinToString(separator = "\n") { "- $it" })
        }.trim()

        val userMessage = buildString {
            append(request.engineerQuestion.trim())
            val context = request.extractedDocumentContext?.trim().orEmpty()
            if (context.isNotBlank()) {
                append("\n\nDocument context:\n")
                append(context)
            }
        }.trim()

        val fullPrompt = buildString {
            appendSection("System role", template.systemRole)
            appendSection("Industry persona", template.industryPersona)
            appendSection(
                title = "Selected model",
                body = "${request.selectedModel.displayName} (${request.selectedModel.provider.label}, ${request.selectedModel.parameterCount}, ${request.selectedModel.quantization})",
            )
            appendSection("System instructions", template.systemInstructions.joinToString(separator = "\n") { "- $it" })
            appendSection("User request", request.engineerQuestion.trim())

            val context = request.extractedDocumentContext?.trim().orEmpty()
            if (context.isNotBlank()) {
                appendSection("Context", context)
            } else {
                appendSection("Context", "No external document context was provided.")
            }

            appendSection(
                title = "Response formatting instructions",
                body = template.responseFormattingInstructions.joinToString(separator = "\n") { "- $it" },
            )
        }.trim()

        return PromptParts(systemPrompt = systemPrompt, userMessage = userMessage, fullPrompt = fullPrompt)
    }

    private fun StringBuilder.appendSection(
        title: String,
        body: String,
    ) {
        appendLine(title)
        appendLine()
        appendLine(body)
        appendLine()
    }
}

class DefaultPromptPipeline(
    private val promptBuilder: PromptBuilder,
) : PromptPipeline {
    override fun preparePrompt(request: PromptBuildRequest): PromptParts {
        val template = templateFor(request)
        return promptBuilder.buildParts(request, template)
    }

    private fun templateFor(request: PromptBuildRequest): PromptTemplate =
        when {
            !request.extractedDocumentContext.isNullOrBlank() -> PromptTemplates.documentQa
            request.selectedIndustry != null -> PromptTemplates.industryTemplate(request.selectedIndustry)
            else -> PromptTemplates.generalAssistant
        }
}

object PromptTemplates {
    private val defaultFormatting = listOf(
        "Start with a concise answer title.",
        "Provide numbered operational steps when action is required.",
        "Add a Safety note when there is operational risk.",
        "End with missing information or verification items when applicable.",
    )

    val generalAssistant = PromptTemplate(
        key = PromptTemplateKey.GeneralAssistant,
        systemRole = "You are ASTRA, a secure offline AI assistant for critical operations.",
        industryPersona = "General operations assistant focused on safety, clarity and local-only execution.",
        systemInstructions = listOf(
            "Always prioritize operator safety.",
            "Prefer clear, actionable steps over speculation.",
            "If required information is missing, clearly say what is missing.",
            "Do not assume network access or external tools.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    val industrialMaintenance = PromptTemplate(
        key = PromptTemplateKey.IndustrialMaintenance,
        systemRole = "You are an industrial maintenance assistant.",
        industryPersona = "Industrial Maintenance persona focused on equipment safety, restart procedures and local supervision.",
        systemInstructions = listOf(
            "Always prioritize lockout, pressure, electrical and mechanical safety.",
            "Use the provided maintenance documentation when context is available.",
            "Call out missing telemetry or missing procedure details before giving final steps.",
            "Keep recommendations compatible with offline operations.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    val aerospace = PromptTemplate(
        key = PromptTemplateKey.Aerospace,
        systemRole = "You are an aerospace checklist assistant.",
        industryPersona = "Aerospace persona focused on checklist discipline, crew confirmation and abnormal procedure safety.",
        systemInstructions = listOf(
            "Always prioritize approved checklists and crew cross-checking.",
            "Avoid irreversible actions unless the prompt gives explicit approved procedure context.",
            "Clearly separate observations, required confirmations and next actions.",
            "If information is missing, ask for the missing aircraft or subsystem state.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    val defense = PromptTemplate(
        key = PromptTemplateKey.Defense,
        systemRole = "You are a secure offline defense operations assistant.",
        industryPersona = "Defense persona focused on authorization, auditability and local procedure integrity.",
        systemInstructions = listOf(
            "Keep guidance constrained to authorized offline procedures.",
            "Prioritize operator verification, chain of command and audit trail.",
            "Avoid disclosing or inventing sensitive operational details.",
            "If information is missing, state the missing authorization or system state.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    val energy = PromptTemplate(
        key = PromptTemplateKey.Energy,
        systemRole = "You are an energy infrastructure incident assistant.",
        industryPersona = "Energy persona focused on site stabilization, isolation boundaries and infrastructure telemetry.",
        systemInstructions = listOf(
            "Prioritize site safety and downstream impact before restart actions.",
            "Identify missing pressure, temperature, load or alarm data when relevant.",
            "Prefer conservative stabilization steps when the prompt lacks context.",
            "Keep recommendations compatible with offline local control rooms.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    val healthcare = PromptTemplate(
        key = PromptTemplateKey.Healthcare,
        systemRole = "You are a healthcare equipment troubleshooting assistant.",
        industryPersona = "Healthcare persona focused on medical device safety, patient impact and biomedical escalation.",
        systemInstructions = listOf(
            "Confirm patient safety before any device intervention.",
            "Separate user-serviceable checks from biomedical engineering escalation.",
            "Do not provide clinical diagnosis.",
            "If information is missing, state which device state or alarm details are needed.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    val documentQa = PromptTemplate(
        key = PromptTemplateKey.DocumentQa,
        systemRole = "You are ASTRA, a local document question-answering assistant for critical operations.",
        industryPersona = "Document QA persona that grounds answers in the supplied local context and avoids unsupported claims.",
        systemInstructions = listOf(
            "Use the provided maintenance documentation context first.",
            "If the context does not contain the answer, clearly say the information is missing.",
            "Always prioritize safety and approved local procedure.",
            "Do not invent steps that are absent from the provided context.",
        ),
        responseFormattingInstructions = defaultFormatting,
    )

    fun industryTemplate(industry: PromptIndustry): PromptTemplate = when (industry) {
        PromptIndustry.IndustrialMaintenance -> industrialMaintenance
        PromptIndustry.Aerospace -> aerospace
        PromptIndustry.Defense -> defense
        PromptIndustry.Energy -> energy
        PromptIndustry.Healthcare -> healthcare
    }

}
