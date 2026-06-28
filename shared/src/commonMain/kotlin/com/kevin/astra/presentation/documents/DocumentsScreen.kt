package com.kevin.astra.presentation.documents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraEmptyView
import com.kevin.astra.core.design.AstraErrorView
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.domain.documents.AstraDocument
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.RetrievedDocumentContext

@Composable
fun DocumentsScreen(
    contentPadding: PaddingValues,
    viewModel: DocumentsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.isGenerating) {
        if (!state.isGenerating && state.answer != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    DocumentsContent(
        state = state,
        contentPadding = contentPadding,
        onIntent = viewModel::dispatch,
    )
}

@Composable
private fun DocumentsContent(
    state: DocumentsState,
    contentPadding: PaddingValues,
    onIntent: (DocumentsIntent) -> Unit,
) {
    AstraScreen(
        title = "Documents Assistant",
        description = "Local document intelligence for critical operations",
        contentPadding = contentPadding,
    ) {
        DocumentCard(
            document = state.selectedDocument,
            status = state.documentStatus,
            chunksCount = state.indexedChunks.size,
            isIndexing = state.isIndexing,
            canIndex = state.canIndex,
            onIndex = { onIntent(DocumentsIntent.IndexSelectedDocument) },
        )

        AnimatedVisibility(visible = state.isGenerating) {
            AstraCard(
                title = "Generating document answer...",
                subtitle = "ASTRA is using extracted local context with the mock inference engine.",
                status = "LOCAL",
            )
        }

        QuestionCard(
            question = state.question,
            enabled = !state.isGenerating && !state.isIndexing,
            canAsk = state.canAsk,
            error = state.error,
            onQuestionChanged = { onIntent(DocumentsIntent.UpdateQuestion(it)) },
            onAsk = { onIntent(DocumentsIntent.AskDocument) },
            onClear = { onIntent(DocumentsIntent.ClearConversation) },
        )

        ContextCard(context = state.extractedContext)

        state.answer?.let { answer ->
            AnswerCard(answer = answer)
            MetricsPanel(metrics = state.metrics)
        }
    }
}

@Composable
private fun DocumentCard(
    document: AstraDocument?,
    status: DocumentStatus,
    chunksCount: Int,
    isIndexing: Boolean,
    canIndex: Boolean,
    onIndex: () -> Unit,
) {
    AstraCard(
        title = "Document",
        subtitle = document?.title ?: "No embedded document available",
        status = status.label,
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = chunksCount.toString(),
                unit = "",
                label = "Chunks",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = document?.estimatedSizeKb?.toString() ?: "0",
                unit = "KB",
                label = "Size estimate",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraChip(
                label = if (isIndexing) "PROCESSING" else status.label.uppercase(),
                color = when (status) {
                    DocumentStatus.Indexed -> AstraColors.Success
                    DocumentStatus.Processing -> AstraColors.Warning
                    DocumentStatus.NotIndexed -> AstraColors.Secondary
                },
            )
            AstraChip(label = "EMBEDDED", color = AstraColors.Secondary)
        }
        Spacer(Modifier.height(AstraSpacing.M))
        AstraButton(
            text = if (isIndexing) "Indexing..." else "Index document",
            onClick = onIndex,
            enabled = canIndex,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuestionCard(
    question: String,
    enabled: Boolean,
    canAsk: Boolean,
    error: String?,
    onQuestionChanged: (String) -> Unit,
    onAsk: () -> Unit,
    onClear: () -> Unit,
) {
    AstraCard(
        title = "Question",
        subtitle = "Ask ASTRA about the embedded maintenance guide.",
        status = "LOCAL QA",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        BasicTextField(
            value = question,
            onValueChange = onQuestionChanged,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp)
                .alpha(if (enabled) 1f else 0.64f),
            textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
            cursorBrush = SolidColor(AstraColors.Secondary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 128.dp)
                        .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp))
                        .padding(AstraSpacing.M),
                ) {
                    if (question.isBlank()) {
                        Text(
                            text = "Ask a question about the maintenance guide...",
                            style = AstraTypography.Body,
                            color = AstraColors.TextDisabled,
                        )
                    }
                    innerTextField()
                }
            },
        )

        AnimatedVisibility(visible = error != null) {
            error?.let {
                Spacer(Modifier.height(AstraSpacing.M))
                AstraErrorView(
                    title = "Document Error",
                    message = it,
                )
            }
        }

        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraButton(
                text = "Ask Document",
                onClick = onAsk,
                enabled = canAsk,
                modifier = Modifier.weight(1f),
            )
            AstraButton(
                text = "Clear",
                onClick = onClear,
                enabled = enabled && question.isNotBlank(),
                style = AstraButtonStyle.Ghost,
            )
        }
    }
}

@Composable
private fun ContextCard(context: RetrievedDocumentContext?) {
    AstraCard(
        title = "Extracted Context",
        subtitle = "Relevant local chunks selected with keyword matching.",
        status = if (context == null) "WAITING" else "${context.chunks.size} CHUNKS",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        if (context == null || context.chunks.isEmpty()) {
            AstraEmptyView(
                title = "No context extracted",
                message = "Context will be retrieved from the local index after you ask a question.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                context.chunks.forEach { chunk ->
                    ContextChunk(title = chunk.title, content = chunk.content)
                }
            }
        }
    }
}

@Composable
private fun ContextChunk(
    title: String,
    content: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = title,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = content,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
private fun AnswerCard(answer: DocumentsAnswer) {
    AstraCard(
        title = answer.title,
        subtitle = "Generated through AskLocalAssistantUseCase using extracted document context.",
        status = "SIMULATED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Text(
            text = answer.body,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
        )
    }
}

@Composable
private fun MetricsPanel(metrics: DocumentsMetrics) {
    AstraCard(
        title = "Metrics",
        subtitle = "Mock local inference telemetry for document question answering.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(metrics.model, "", "Model", Modifier.weight(1f))
                AstraMetricCard(metrics.backend, "", "Backend", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(metrics.latency, "", "Latency", Modifier.weight(1f))
                AstraMetricCard(metrics.tokensPerSecond, "", "Tokens/sec", Modifier.weight(1f))
            }
        }
    }
}
