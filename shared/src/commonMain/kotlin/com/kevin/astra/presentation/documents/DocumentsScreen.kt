package com.kevin.astra.presentation.documents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraEmptyView
import com.kevin.astra.core.design.AstraErrorView
import com.kevin.astra.core.design.MarkdownText
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.domain.documents.DocumentStatus
import com.kevin.astra.domain.documents.RetrievedDocumentContext
import androidx.compose.ui.Alignment

@Composable
fun DocumentsScreen(
    contentPadding: PaddingValues,
    viewModel: DocumentsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Refresh on every resume so returning from the Gmail consent screen updates the connection state.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(DocumentsIntent.RefreshGmailState)
    }

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
    val emailLauncher = rememberEmailPickerLauncher { bytes, fileName ->
        onIntent(DocumentsIntent.EmailLoaded(bytes, fileName))
    }

    AstraScreen(
        title = "RAG Document Assistant",
        description = "On-device retrieval — your documents never leave the phone",
        contentPadding = contentPadding,
    ) {
        DocumentCard(
            fileName = state.loadedFileName,
            pageCount = state.pageCount,
            emailCount = state.emailCount,
            sourceType = state.sourceType,
            status = state.documentStatus,
            chunksCount = state.indexedChunks.size,
            isLoading = state.isLoading,
            isIndexing = state.isIndexing,
            onPickPdf = pdfLauncher,
            onPickEmail = emailLauncher,
            onClear = { onIntent(DocumentsIntent.ClearDocument) },
        )

        if (state.gmailSupported) {
            GmailCard(
                connected = state.gmailConnected,
                query = state.gmailQuery,
                isFetching = state.isFetchingGmail,
                enabled = !state.isFetchingGmail && !state.isIndexing && !state.isLoading,
                onConnect = { onIntent(DocumentsIntent.ConnectGmail) },
                onDisconnect = { onIntent(DocumentsIntent.DisconnectGmail) },
                onQueryChange = { onIntent(DocumentsIntent.UpdateGmailQuery(it)) },
                onFetchRecent = { onIntent(DocumentsIntent.FetchGmailRecent) },
                onFetchSearch = { onIntent(DocumentsIntent.FetchGmailSearch) },
            )
        }

        if (state.availableModels.size > 1) {
            RagModelSelector(
                models = state.availableModels,
                selectedModel = state.sessionModel,
                enabled = !state.isGenerating && !state.isIndexing,
                onSelectModel = { onIntent(DocumentsIntent.SelectSessionModel(it)) },
            )
        }

        AnimatedVisibility(visible = state.isLoading || state.isIndexing || state.isSummarizing) {
            Column {
                Spacer(Modifier.height(AstraSpacing.S))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AstraColors.Secondary,
                    trackColor = AstraColors.Border,
                )
                Spacer(Modifier.height(AstraSpacing.XS))
                Text(
                    text = when {
                        state.isLoading -> "Extracting text from PDF…"
                        state.isIndexing -> "Chunking & indexing document…"
                        else -> "Generating document summary…"
                    },
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            }
        }

        if (state.loadedFileName == null && !state.isLoading && !state.isIndexing) {
            AstraEmptyView(
                title = "No document loaded",
                message = "Load a PDF to start querying it with on-device RAG. Your files never leave the device.",
            )
        }

        AnimatedVisibility(visible = state.documentSummary != null && !state.isSummarizing) {
            state.documentSummary?.let { summary ->
                SummaryCard(
                    summary = summary,
                    onAskAboutDocument = {
                        onIntent(DocumentsIntent.UpdateQuestion("What is this document about?"))
                        onIntent(DocumentsIntent.AskDocument)
                    },
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
    emailCount: Int,
    sourceType: DocumentSourceType,
    status: DocumentStatus,
    chunksCount: Int,
    isLoading: Boolean,
    isIndexing: Boolean,
    onPickPdf: () -> Unit,
    onPickEmail: () -> Unit,
    onClear: () -> Unit,
) {
    val isEmail = sourceType == DocumentSourceType.Email
    val subtitle = when {
        fileName != null -> fileName
        else -> "No source loaded — import a PDF or email file"
    }
    AstraCard(
        title = if (isEmail) "Email Source" else "Document",
        subtitle = subtitle,
        status = if (isLoading) "LOADING" else if (isIndexing) "INDEXING" else status.label.uppercase(),
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            if (isEmail) {
                AstraMetricCard(
                    value = if (emailCount > 0) emailCount.toString() else "—",
                    unit = "",
                    label = if (emailCount == 1) "Email" else "Emails",
                    modifier = Modifier.weight(1f),
                )
            } else {
                AstraMetricCard(
                    value = if (pageCount > 0) pageCount.toString() else "—",
                    unit = "",
                    label = "Pages",
                    modifier = Modifier.weight(1f),
                )
            }
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
            AstraChip(label = if (isEmail) "EMAIL" else "PDF", color = AstraColors.Primary.copy(alpha = 0.7f))
            AstraChip(label = "ON-DEVICE", color = AstraColors.Secondary)
        }
        Spacer(Modifier.height(AstraSpacing.M))
        if (fileName == null) {
            // No source loaded — show both import options
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraButton(
                    text = "📄 PDF",
                    onClick = onPickPdf,
                    enabled = !isLoading && !isIndexing,
                    modifier = Modifier.weight(1f),
                )
                AstraButton(
                    text = "📧 Email",
                    onClick = onPickEmail,
                    enabled = !isLoading && !isIndexing,
                    modifier = Modifier.weight(1f),
                    style = AstraButtonStyle.Secondary,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraButton(
                    text = if (isLoading || isIndexing) "Processing…" else if (isEmail) "📧 New email" else "📄 New PDF",
                    onClick = if (isEmail) onPickEmail else onPickPdf,
                    enabled = !isLoading && !isIndexing,
                    modifier = Modifier.weight(1f),
                )
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
private fun GmailCard(
    connected: Boolean,
    query: String,
    isFetching: Boolean,
    enabled: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFetchRecent: () -> Unit,
    onFetchSearch: () -> Unit,
) {
    AstraCard(
        title = "Gmail",
        subtitle = "Fetched from Google, then analyzed only on this device.",
        status = if (connected) "CONNECTED" else "OFF",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        if (!connected) {
            Text(
                text = "Connect your Gmail (read-only) to query your inbox with on-device RAG. " +
                    "Messages are retrieved from Google, then processed locally — nothing is sent anywhere else.",
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
            Spacer(Modifier.height(AstraSpacing.M))
            AstraButton(
                text = "🔗 Connect Gmail",
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.64f),
                textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
                cursorBrush = SolidColor(AstraColors.Secondary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
                            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
                            .padding(AstraSpacing.M),
                    ) {
                        if (query.isBlank()) {
                            Text(
                                text = "Gmail search — e.g. from:bank invoice",
                                style = AstraTypography.Body,
                                color = AstraColors.TextDisabled,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            AnimatedVisibility(visible = isFetching) {
                Column {
                    Spacer(Modifier.height(AstraSpacing.S))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AstraColors.Secondary,
                        trackColor = AstraColors.Border,
                    )
                    Spacer(Modifier.height(AstraSpacing.XS))
                    Text(
                        text = "Fetching from Gmail…",
                        style = AstraTypography.Caption,
                        color = AstraColors.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(AstraSpacing.M))
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraButton(
                    text = if (isFetching) "Fetching…" else "📥 Latest 20",
                    onClick = onFetchRecent,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
                AstraButton(
                    text = "🔍 Search",
                    onClick = onFetchSearch,
                    enabled = enabled && query.isNotBlank(),
                    style = AstraButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(AstraSpacing.S))
            AstraButton(
                text = "Disconnect",
                onClick = onDisconnect,
                enabled = enabled,
                style = AstraButtonStyle.Ghost,
            )
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
        MarkdownText(text = answer.body)
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

@Composable
private fun SummaryCard(summary: String, onAskAboutDocument: () -> Unit) {
    AstraCard(
        title = "Document Summary",
        subtitle = "Auto-generated from the first pages of your document.",
        status = "AI",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        MarkdownText(text = summary)
        Spacer(Modifier.height(AstraSpacing.M))
        AstraButton(
            text = "Ask about this document",
            onClick = onAskAboutDocument,
            style = AstraButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RagModelSelector(
    models: List<LocalModel>,
    selectedModel: LocalModel?,
    enabled: Boolean,
    onSelectModel: (String) -> Unit,
) {
    AstraCard(
        title = "Generation Model",
        subtitle = "Model used to answer questions from retrieved context.",
        status = selectedModel?.displayName ?: "—",
    ) {
        Spacer(Modifier.height(AstraSpacing.S))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            models.forEach { model ->
                val selected = model.id == selectedModel?.id
                AstraChip(
                    label = model.displayName,
                    color = if (selected) AstraColors.Secondary else AstraColors.Border,
                    modifier = Modifier
                        .then(if (enabled) Modifier.clickable { onSelectModel(model.id) } else Modifier),
                )
            }
        }
    }
}
