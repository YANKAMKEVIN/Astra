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
import androidx.compose.material3.LinearProgressIndicator
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
    val pdfLauncher = rememberPdfPickerLauncher { bytes, fileName ->
        onIntent(DocumentsIntent.PdfLoaded(bytes, fileName))
    }

    AstraScreen(
        title = "RAG Document Assistant",
        description = "On-device retrieval — your documents never leave the phone",
        contentPadding = contentPadding,
    ) {
        DocumentCard(
            fileName = state.loadedFileName,
            pageCount = state.pageCount,
            status = state.documentStatus,
            chunksCount = state.indexedChunks.size,
            isLoading = state.isLoading,
            isIndexing = state.isIndexing,
            onPickPdf = pdfLauncher,
            onClear = { onIntent(DocumentsIntent.ClearDocument) },
        )

        AnimatedVisibility(visible = state.isLoading || state.isIndexing) {
            Column {
                Spacer(Modifier.height(AstraSpacing.S))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AstraColors.Secondary,
                    trackColor = AstraColors.Border,
                )
                Spacer(Modifier.height(AstraSpacing.XS))
                Text(
                    text = if (state.isLoading) "Extracting text from PDF…" else "Chunking & indexing document…",
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            }
        }

        AnimatedVisibility(visible = state.documentStatus == DocumentStatus.Indexed) {
            QuestionCard(
                question = state.question,
                enabled = !state.isGenerating,
                canAsk = state.canAsk,
                isGenerating = state.isGenerating,
                error = state.error,
                onQuestionChanged = { onIntent(DocumentsIntent.UpdateQuestion(it)) },
                onAsk = { onIntent(DocumentsIntent.AskDocument) },
                onClear = { onIntent(DocumentsIntent.ClearAnswer) },
            )
        }

        AnimatedVisibility(visible = state.extractedContext != null) {
            ContextCard(context = state.extractedContext)
        }

        state.answer?.let { answer ->
            AnswerCard(answer = answer)
            MetricsPanel(metrics = state.metrics)
        }

        state.error?.let { err ->
            AnimatedVisibility(visible = !state.isLoading && !state.isIndexing) {
                AstraErrorView(title = "Error", message = err)
            }
        }
    }
}

@Composable
private fun DocumentCard(
    fileName: String?,
    pageCount: Int,
    status: DocumentStatus,
    chunksCount: Int,
    isLoading: Boolean,
    isIndexing: Boolean,
    onPickPdf: () -> Unit,
    onClear: () -> Unit,
) {
    AstraCard(
        title = "Document",
        subtitle = fileName ?: "No document loaded — select a PDF to begin",
        status = if (isLoading) "LOADING" else if (isIndexing) "INDEXING" else status.label.uppercase(),
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = if (pageCount > 0) pageCount.toString() else "—",
                unit = "",
                label = "Pages",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = if (chunksCount > 0) chunksCount.toString() else "—",
                unit = "",
                label = "Chunks",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraChip(
                label = if (isLoading || isIndexing) "PROCESSING" else status.label.uppercase(),
                color = when (status) {
                    DocumentStatus.Indexed -> AstraColors.Success
                    DocumentStatus.Processing -> AstraColors.Warning
                    DocumentStatus.Error -> AstraColors.Error
                    DocumentStatus.NotIndexed -> AstraColors.Secondary
                },
            )
            AstraChip(label = "ON-DEVICE", color = AstraColors.Secondary)
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraButton(
                text = if (isLoading || isIndexing) "Processing…" else "Choose PDF",
                onClick = onPickPdf,
                enabled = !isLoading && !isIndexing,
                modifier = Modifier.weight(1f),
            )
            if (fileName != null) {
                AstraButton(
                    text = "Clear",
                    onClick = onClear,
                    enabled = !isLoading && !isIndexing,
                    style = AstraButtonStyle.Ghost,
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: String,
    enabled: Boolean,
    canAsk: Boolean,
    isGenerating: Boolean,
    error: String?,
    onQuestionChanged: (String) -> Unit,
    onAsk: () -> Unit,
    onClear: () -> Unit,
) {
    AstraCard(
        title = "Ask your document",
        subtitle = "ASTRA answers using only the content of your PDF — no internet.",
        status = "RAG",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        BasicTextField(
            value = question,
            onValueChange = onQuestionChanged,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .alpha(if (enabled) 1f else 0.64f),
            textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
            cursorBrush = SolidColor(AstraColors.Secondary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp))
                        .padding(AstraSpacing.M),
                ) {
                    if (question.isBlank()) {
                        Text(
                            text = "What does this document say about…",
                            style = AstraTypography.Body,
                            color = AstraColors.TextDisabled,
                        )
                    }
                    innerTextField()
                }
            },
        )

        AnimatedVisibility(visible = isGenerating) {
            Column {
                Spacer(Modifier.height(AstraSpacing.S))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AstraColors.Secondary,
                    trackColor = AstraColors.Border,
                )
            }
        }

        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraButton(
                text = if (isGenerating) "Generating…" else "Ask ASTRA",
                onClick = onAsk,
                enabled = canAsk,
                modifier = Modifier.weight(1f),
            )
            AstraButton(
                text = "Clear",
                onClick = onClear,
                enabled = question.isNotBlank() && !isGenerating,
                style = AstraButtonStyle.Ghost,
            )
        }
    }
}

@Composable
private fun ContextCard(context: RetrievedDocumentContext?) {
    if (context == null) return
    AstraCard(
        title = "Retrieved context",
        subtitle = "Top chunks selected by TF-IDF scoring from the local index.",
        status = "${context.chunks.size} CHUNKS",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        if (context.chunks.isEmpty()) {
            AstraEmptyView(
                title = "No relevant chunks found",
                message = "Try rephrasing your question.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                context.chunks.forEach { chunk ->
                    ContextChunk(
                        pageHint = chunk.pageHint,
                        content = chunk.content,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextChunk(pageHint: Int, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        if (pageHint > 0) {
            Text(
                text = "Page ~$pageHint",
                style = AstraTypography.Caption,
                color = AstraColors.Secondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(AstraSpacing.XS))
        }
        Text(
            text = content.take(280) + if (content.length > 280) "…" else "",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
private fun AnswerCard(answer: DocumentsAnswer) {
    AstraCard(
        title = "ASTRA answer",
        subtitle = "Based exclusively on your document content.",
        status = "LOCAL",
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
    AstraCard(title = "Metrics") {
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
