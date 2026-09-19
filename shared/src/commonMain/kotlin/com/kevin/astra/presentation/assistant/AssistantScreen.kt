package com.kevin.astra.presentation.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraErrorView
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.design.DemoModeBanner
import com.kevin.astra.core.design.MarkdownText
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.domain.assistant.PromptTemplate
import com.kevin.astra.domain.demo.DemoScenario
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.settings.DemoModeHolder
import com.kevin.astra.presentation.documents.rememberEmailPickerLauncher
import com.kevin.astra.presentation.documents.rememberPdfPickerLauncher
import com.kevin.astra.presentation.vision.rememberImageCaptureLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    contentPadding: PaddingValues,
    viewModel: AssistantViewModel,
    onNavigate: (AstraDestination) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Refresh Gmail connection state on resume (e.g. returning from the consent screen).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(AssistantIntent.RefreshGmailState)
    }

    LaunchedEffect(state.isGenerating) {
        if (!state.isGenerating && state.messages.lastOrNull()?.role == ChatRole.Assistant) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    var lastHapticLength by remember { mutableStateOf(0) }
    LaunchedEffect(state.streamingText.length) {
        if (state.streamingText.length - lastHapticLength >= 50) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticLength = state.streamingText.length
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AssistantDrawer(
                state = state,
                onNewChat = {
                    scope.launch { drawerState.close() }
                    viewModel.dispatch(AssistantIntent.ClearConversation)
                },
                onLoadConversation = { id ->
                    scope.launch { drawerState.close() }
                    viewModel.dispatch(AssistantIntent.LoadConversation(id))
                },
                onSelectIndustry = { viewModel.dispatch(AssistantIntent.SelectIndustry(it)) },
                onSelectModel = { viewModel.dispatch(AssistantIntent.SelectSessionModel(it)) },
                onSelectTemplate = { viewModel.dispatch(AssistantIntent.SelectTemplate(it)) },
                onSelectScenario = {
                    scope.launch { drawerState.close() }
                    viewModel.dispatch(AssistantIntent.SelectScenario(it))
                },
            )
        },
    ) {
        AssistantContent(
            state = state,
            contentPadding = contentPadding,
            onIntent = viewModel::dispatch,
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onNavigate = onNavigate,
        )
    }
}

// ── Drawer ────────────────────────────────────────────────────────────────────

@Composable
private fun AssistantDrawer(
    state: AssistantState,
    onNewChat: () -> Unit,
    onLoadConversation: (String) -> Unit,
    onSelectIndustry: (AssistantIndustry) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onSelectScenario: (DemoScenario) -> Unit,
) {
    var paramsExpanded by remember { mutableStateOf(true) }
    var historyExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(AstraColors.Surface)
            .border(width = 1.dp, color = AstraColors.Border)
            .verticalScroll(rememberScrollState())
            .padding(AstraSpacing.L),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        // Header
        Text(
            text = "ASTRA",
            style = AstraTypography.Headline,
            color = AstraColors.Primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Local Edge AI",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )

        Spacer(Modifier.height(AstraSpacing.S))

        // New chat
        AstraButton(
            text = "+ New chat",
            onClick = onNewChat,
            style = AstraButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        DrawerDivider()

        // ── History ───────────────────────────────────────────────────────
        DrawerSectionHeader(
            title = "History",
            expanded = historyExpanded,
            onToggle = { historyExpanded = !historyExpanded },
        )
        AnimatedVisibility(visible = historyExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
                if (state.recentHistory.isEmpty()) {
                    Text(
                        text = "No conversations yet.",
                        style = AstraTypography.Caption,
                        color = AstraColors.TextDisabled,
                        modifier = Modifier.padding(start = AstraSpacing.S),
                    )
                } else {
                    state.recentHistory.forEach { conv ->
                        HistoryItem(
                            conversation = conv,
                            isActive = state.messages.firstOrNull()?.id?.contains(conv.messages.firstOrNull()?.timestamp?.replace(Regex("[^0-9]"), "") ?: "") == true,
                            onClick = { onLoadConversation(conv.id) },
                        )
                    }
                }
            }
        }

        DrawerDivider()

        // ── Chat parameters ───────────────────────────────────────────────
        DrawerSectionHeader(
            title = "Chat parameters",
            expanded = paramsExpanded,
            onToggle = { paramsExpanded = !paramsExpanded },
        )
        AnimatedVisibility(visible = paramsExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.L)) {

                // Industry
                DrawerParamSection(title = "Domain") {
                    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
                        AssistantIndustry.entries.forEach { industry ->
                            val selected = industry == state.selectedIndustry
                            DrawerOptionRow(
                                label = industry.label,
                                selected = selected,
                                onClick = { onSelectIndustry(industry) },
                            )
                        }
                    }
                }

                // Model
                if (state.installedModels.size > 1) {
                    DrawerParamSection(title = "AI model") {
                        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
                            state.installedModels.forEach { model ->
                                val selected = model.id == state.sessionModel?.id
                                DrawerOptionRow(
                                    label = model.displayName,
                                    sublabel = "${model.parameterCount} · ${model.quantization}",
                                    selected = selected,
                                    onClick = { onSelectModel(model.id) },
                                )
                            }
                        }
                    }
                }

                // Templates
                if (state.promptTemplates.isNotEmpty()) {
                    DrawerParamSection(title = "Prompt templates") {
                        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
                            state.promptTemplates.forEach { template ->
                                val selected = template.id == state.activeTemplate?.id
                                DrawerOptionRow(
                                    label = "${template.icon}  ${template.label}",
                                    selected = selected,
                                    onClick = { onSelectTemplate(template) },
                                )
                            }
                        }
                    }
                }

                // Demo scenarios
                if (state.availableScenarios.isNotEmpty()) {
                    DrawerParamSection(title = "Demo scenarios") {
                        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
                            state.availableScenarios.take(6).forEach { scenario ->
                                DrawerOptionRow(
                                    label = scenario.title,
                                    sublabel = scenario.industry.label,
                                    selected = false,
                                    onClick = { onSelectScenario(scenario) },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(AstraSpacing.XL))
    }
}

@Composable
private fun DrawerDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AstraColors.Border),
    )
}

@Composable
private fun DrawerSectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = AstraSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = AstraTypography.Caption,
            color = AstraColors.TextDisabled,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (expanded) "▲" else "▼",
            style = AstraTypography.Caption,
            color = AstraColors.TextDisabled,
        )
    }
}

@Composable
private fun DrawerParamSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        Text(
            text = title,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun DrawerOptionRow(
    label: String,
    sublabel: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) AstraColors.Primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AstraColors.Secondary, CircleShape),
            )
        } else {
            Spacer(Modifier.size(6.dp))
        }
        Column {
            Text(
                text = label,
                style = AstraTypography.Caption,
                color = if (selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = AstraTypography.Caption,
                    color = AstraColors.TextDisabled,
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    conversation: ChatConversation,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) AstraColors.Primary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "💬",
            style = AstraTypography.Caption,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                style = AstraTypography.Caption,
                color = if (isActive) AstraColors.TextPrimary else AstraColors.TextSecondary,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = conversation.createdAt,
                style = AstraTypography.Caption,
                color = AstraColors.TextDisabled,
            )
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AstraColors.Secondary, CircleShape)
                    .align(Alignment.CenterVertically),
            )
        }
    }
}

// ── Main chat content ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantContent(
    state: AssistantState,
    contentPadding: PaddingValues,
    onIntent: (AssistantIntent) -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigate: (AstraDestination) -> Unit,
) {
    val isDemoMode by DemoModeHolder.enabled.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showToolsSheet by remember { mutableStateOf(false) }
    var shareTargetBubbleId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.messages.size, state.streamingText) {
        if (state.messages.isNotEmpty() || state.streamingText.isNotEmpty()) {
            listState.animateScrollToItem(Int.MAX_VALUE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        // ── Header ────────────────────────────────────────────────────────
        if (isDemoMode) {
            DemoModeBanner(modifier = Modifier.padding(horizontal = AstraSpacing.L).padding(top = AstraSpacing.M))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AstraSpacing.L)
                .padding(top = if (isDemoMode) AstraSpacing.S else AstraSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
        ) {
            // ☰ Hamburger
            HeaderIconButton(icon = "☰", onClick = onOpenDrawer)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ASTRA Assistant",
                    style = AstraTypography.Title,
                    color = AstraColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
                ) {
                    Text(
                        text = "${state.selectedIndustry?.label ?: "General"} · ${state.sessionModel?.displayName ?: "Local AI"}",
                        style = AstraTypography.Caption,
                        color = AstraColors.TextSecondary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    HeaderLocalBadge()
                }
            }

            if (state.messages.isNotEmpty()) {
                AstraButton(
                    text = "Clear",
                    onClick = { onIntent(AssistantIntent.ClearConversation) },
                    style = AstraButtonStyle.Ghost,
                )
            }

            // ⚙ Tools
            HeaderIconButton(icon = "⚙", onClick = { showToolsSheet = true })
        }

    // ── Tools bottom sheet ─────────────────────────────────────────────
    if (showToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToolsSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.55f),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AstraSpacing.S)
                    .navigationBarsPadding()
                    .padding(bottom = AstraSpacing.S)
                    .shadow(30.dp, RoundedCornerShape(32.dp), clip = false)
                    .clip(RoundedCornerShape(32.dp))
                    .background(AstraColors.SurfaceElevated.copy(alpha = 0.94f))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
                    .padding(horizontal = AstraSpacing.L)
                    .padding(top = AstraSpacing.M, bottom = AstraSpacing.L),
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AstraColors.TextSecondary.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.height(AstraSpacing.XS))
                Text(
                    text = "Tools",
                    style = AstraTypography.Title,
                    color = AstraColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(AstraSpacing.XS))
                AstraDestination.secondaryNavDestinations.forEach { dest ->
                    val icon = when (dest) {
                        AstraDestination.VoiceAssistant -> "🎤"
                        AstraDestination.VisionAssistant -> "📷"
                        AstraDestination.History -> "🕐"
                        AstraDestination.Demo -> "🚀"
                        else -> "›"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AstraColors.Surface)
                            .border(1.dp, AstraColors.Border, RoundedCornerShape(18.dp))
                            .clickable {
                                showToolsSheet = false
                                onNavigate(dest)
                            }
                            .padding(horizontal = AstraSpacing.M, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AstraColors.Secondary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = icon, style = AstraTypography.Body)
                        }
                        Text(
                            text = dest.label,
                            style = AstraTypography.Body,
                            color = AstraColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "›",
                            style = AstraTypography.Title,
                            color = AstraColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }

    // ── Share format picker ────────────────────────────────────────────────
    if (shareTargetBubbleId != null) {
        ModalBottomSheet(
            onDismissRequest = { shareTargetBubbleId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AstraColors.Surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AstraSpacing.L)
                    .padding(bottom = AstraSpacing.XL),
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
            ) {
                Text(
                    text = "Share as…",
                    style = AstraTypography.Title,
                    color = AstraColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(AstraSpacing.S))
                ExportFormat.entries.forEach { format ->
                    val icon = when (format) {
                        ExportFormat.PlainText -> "📝"
                        ExportFormat.Markdown -> "✍️"
                        ExportFormat.Pdf -> "📄"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AstraColors.SurfaceElevated, RoundedCornerShape(14.dp))
                            .border(1.dp, AstraColors.Border, RoundedCornerShape(14.dp))
                            .clickable {
                                val id = shareTargetBubbleId ?: return@clickable
                                shareTargetBubbleId = null
                                onIntent(AssistantIntent.ShareBubble(id, format))
                            }
                            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.M),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
                    ) {
                        Text(text = icon, style = AstraTypography.Body)
                        Text(
                            text = format.label,
                            style = AstraTypography.Body,
                            color = AstraColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }

        Spacer(Modifier.height(AstraSpacing.M))

        // ── Messages / empty state ────────────────────────────────────────
        if (state.isEmpty) {
            EmptyChat(
                modifier = Modifier.weight(1f),
                industry = state.selectedIndustry,
                onSuggestionSelected = { onIntent(AssistantIntent.UpdateQuestion(it)) },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = AstraSpacing.L, vertical = AstraSpacing.M),
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
            ) {
                items(state.messages, key = { it.id }) { bubble ->
                    SwipeableBubble(onDismiss = { onIntent(AssistantIntent.RemoveMessage(bubble.id)) }) {
                        MessageBubble(
                            bubble = bubble,
                            onShare = { shareTargetBubbleId = bubble.id },
                        )
                    }
                }
                if (state.isGenerating && !state.isStreaming) {
                    item { ThinkingBubble() }
                }
                if (state.isStreaming) {
                    item { StreamingBubble(text = state.streamingText) }
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────
        InputBar(
            question = state.question,
            canAsk = state.canAsk,
            isGenerating = state.isGenerating,
            isListening = state.isListening,
            voiceState = state.voiceState,
            attachedPdf = state.attachedPdf,
            attachedImage = state.attachedImage,
            attachedEmail = state.attachedEmail,
            gmailSupported = state.gmailSupported,
            error = state.error,
            onQuestionChanged = { onIntent(AssistantIntent.UpdateQuestion(it)) },
            onAsk = { onIntent(AssistantIntent.AskQuestion) },
            onStop = { onIntent(AssistantIntent.CancelGeneration) },
            onPdfAttached = { bytes, name -> onIntent(AssistantIntent.PdfAttached(bytes, name)) },
            onImageAttached = { bytes -> onIntent(AssistantIntent.ImageAttached(bytes)) },
            onEmailAttached = { bytes, name -> onIntent(AssistantIntent.EmailFileAttached(bytes, name)) },
            onAttachGmail = { onIntent(AssistantIntent.AttachGmail) },
            onRemovePdf = { onIntent(AssistantIntent.RemovePdf) },
            onRemoveImage = { onIntent(AssistantIntent.RemoveImage) },
            onRemoveEmail = { onIntent(AssistantIntent.RemoveEmail) },
            onToggleVoice = { onIntent(AssistantIntent.ToggleVoiceInput) },
        )
    }
}

// ── Header icon button ────────────────────────────────────────────────────────

@Composable
private fun HeaderIconButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = icon, style = AstraTypography.Body, color = AstraColors.TextPrimary)
    }
}

/** Compact on-device indicator shown in the Chat header next to the model name. */
@Composable
private fun HeaderLocalBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AstraColors.Secondary.copy(alpha = 0.12f))
            .border(1.dp, AstraColors.Secondary.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = AstraSpacing.S, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "●", fontSize = 8.sp, color = AstraColors.Secondary)
        Text(
            text = "LOCAL",
            style = AstraTypography.Caption.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
            color = AstraColors.Secondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyChat(
    modifier: Modifier,
    industry: AssistantIndustry?,
    onSuggestionSelected: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AstraSpacing.L)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        Spacer(Modifier.height(AstraSpacing.L))
        // ── Hero: ASTRA Core (breathing orb + rings + radial halo) ──────────
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AstraCore(coreSize = 96.dp)
        }
        Text(
            text = "How can I help you?",
            style = AstraTypography.Title.copy(fontSize = 28.sp, lineHeight = 34.sp),
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Private AI. Running entirely on this device.",
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = AstraSpacing.S),
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        // ── On-device status pills ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S, Alignment.CenterHorizontally),
        ) {
            EmptyStatePill(glyph = "●", label = "OFFLINE READY", tint = AstraColors.Success)
            EmptyStatePill(glyph = "●", label = "PRIVATE", tint = AstraColors.Secondary)
            EmptyStatePill(glyph = "●", label = "LOCAL", tint = AstraColors.Primary)
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = "SUGGESTIONS",
            style = AstraTypography.Caption.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
            ),
            color = AstraColors.TextDisabled,
            fontWeight = FontWeight.Bold,
        )
        quickSuggestions(industry).forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AstraColors.SurfaceElevated.copy(alpha = 0.55f))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .clickable { onSuggestionSelected(suggestion) }
                    .padding(horizontal = AstraSpacing.M, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            AstraColors.Secondary.copy(alpha = 0.14f),
                            RoundedCornerShape(11.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✦", fontSize = 15.sp, color = AstraColors.Secondary)
                }
                Text(
                    text = suggestion,
                    style = AstraTypography.Body,
                    color = AstraColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "›", fontSize = 22.sp, color = AstraColors.TextDisabled)
            }
        }
        Spacer(Modifier.height(AstraSpacing.M))
    }
}

/**
 * ASTRA Core — the signature "living" orb: a Primary→Secondary gradient core
 * inside two ultra-subtle rings, over a soft radial halo, with a very slow
 * breathing + halo pulse. No backdrop blur required.
 */
@Composable
private fun AstraCore(coreSize: Dp = 96.dp) {
    val transition = rememberInfiniteTransition(label = "astra-core")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "core-scale",
    )
    val halo by transition.animateFloat(
        initialValue = 0.07f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "core-halo",
    )
    Box(
        modifier = Modifier.size(coreSize * 1.9f),
        contentAlignment = Alignment.Center,
    ) {
        // radial halo
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(AstraColors.Secondary.copy(alpha = halo), Color.Transparent),
                    ),
                ),
        )
        // outer ring
        Box(
            modifier = Modifier
                .size(coreSize * 1.48f)
                .border(1.dp, AstraColors.Primary.copy(alpha = 0.10f), CircleShape),
        )
        // inner ring
        Box(
            modifier = Modifier
                .size(coreSize * 1.23f)
                .border(1.dp, AstraColors.Secondary.copy(alpha = 0.16f), CircleShape),
        )
        // core
        Box(
            modifier = Modifier
                .size(coreSize)
                .scale(scale)
                .shadow(
                    elevation = 26.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = AstraColors.Primary,
                    spotColor = AstraColors.Secondary,
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AstraColors.Primary, AstraColors.Secondary)))
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✦", fontSize = 34.sp, color = Color.White)
        }
    }
}

@Composable
private fun EmptyStatePill(glyph: String, label: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AstraColors.SurfaceElevated.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(horizontal = AstraSpacing.S + 2.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = glyph, fontSize = 9.sp, color = tint)
        Text(
            text = label,
            style = AstraTypography.Caption.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
            color = AstraColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun quickSuggestions(industry: AssistantIndustry?): List<String> = when (industry) {
    AssistantIndustry.IndustrialMaintenance -> listOf(
        "Diagnose hydraulic pump failure symptoms",
        "What are the FMEA steps for a conveyor belt?",
        "Explain predictive maintenance benefits",
    )
    AssistantIndustry.Aerospace -> listOf(
        "Explain the V-cycle in avionics development",
        "What are DO-178C certification levels?",
        "Compare SAF vs. conventional jet fuel",
    )
    AssistantIndustry.Defense -> listOf(
        "What is NATO STANAG 4586?",
        "Explain the OODA loop decision framework",
        "What is electronic countermeasure jamming?",
    )
    AssistantIndustry.Energy -> listOf(
        "How does SCADA monitor power grids?",
        "Explain capacity factor for wind farms",
        "What is frequency regulation in smart grids?",
    )
    AssistantIndustry.Healthcare -> listOf(
        "Explain HL7 FHIR data standards",
        "What are DICOM imaging best practices?",
        "How does federated learning improve diagnostics?",
    )
    null -> listOf(
        "Hello, what can you do?",
        "Explain edge AI in simple terms",
        "What makes on-device inference private?",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBubble(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        },
        positionalThreshold = { it * 0.45f },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val alpha by animateFloatAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) 1f else 0f,
                label = "dismiss-bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = AstraSpacing.S)
                    .alpha(alpha),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AstraColors.Error.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, AstraColors.Error.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🗑", style = AstraTypography.Caption)
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        content()
    }
}

// ── Message bubbles ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    bubble: ChatBubble,
    onShare: () -> Unit = {},
) {
    val isUser = bubble.role == ChatRole.User
    var showMetrics by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            AstraAvatar()
            Spacer(Modifier.width(AstraSpacing.S))
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            val bubbleShape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp,
            )
            Box(
                modifier = Modifier
                    .then(
                        if (isUser) {
                            Modifier.background(
                                Brush.linearGradient(
                                    listOf(AstraColors.Primary, AstraColors.Secondary),
                                ),
                                bubbleShape,
                            )
                        } else {
                            Modifier
                                .background(AstraColors.SurfaceElevated, bubbleShape)
                                .border(1.dp, AstraColors.Border, bubbleShape)
                        },
                    )
                    .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
            ) {
                if (isUser) {
                    Text(text = bubble.text, style = AstraTypography.Body, color = Color.White)
                } else {
                    MarkdownText(text = bubble.text)
                }
            }
            if (!isUser) {
                Spacer(Modifier.height(AstraSpacing.XS))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
                    modifier = Modifier.padding(horizontal = AstraSpacing.XS),
                ) {
                    if (bubble.metrics != null) {
                        Text(
                            text = if (showMetrics) "▲ hide" else "${bubble.metrics.latency} · ${bubble.metrics.backend} ▼",
                            style = AstraTypography.Caption,
                            color = AstraColors.TextDisabled,
                            modifier = Modifier.clickable { showMetrics = !showMetrics },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    BubbleActionButton(label = "⎘", onClick = {
                        clipboard.setText(AnnotatedString(bubble.text))
                    })
                    BubbleActionButton(label = "↗", onClick = onShare)
                }
                if (bubble.metrics != null) {
                    AnimatedVisibility(visible = showMetrics) {
                        MetricsInline(metrics = bubble.metrics)
                    }
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(AstraSpacing.S))
            UserAvatar()
        }
    }
}

@Composable
private fun AstraAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(AstraColors.Primary.copy(alpha = 0.18f), CircleShape)
            .border(1.dp, AstraColors.Primary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "A", style = AstraTypography.Caption, color = AstraColors.Primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UserAvatar() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(AstraColors.Secondary.copy(alpha = 0.18f), CircleShape)
            .border(1.dp, AstraColors.Secondary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "U", style = AstraTypography.Caption, color = AstraColors.Secondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricsInline(metrics: AssistantMetrics) {
    Row(
        modifier = Modifier
            .padding(top = AstraSpacing.XS, start = AstraSpacing.XS)
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(10.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(10.dp))
            .padding(AstraSpacing.S),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        MetricPill("⏱", metrics.latency)
        MetricPill("⚡", metrics.tokensPerSecond + " t/s")
        MetricPill("🧠", metrics.model)
    }
}

@Composable
private fun BubbleActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(8.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
    }
}

@Composable
private fun MetricPill(icon: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, style = AstraTypography.Caption)
        Text(text = value, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
    }
}

// ── Thinking + streaming bubbles ──────────────────────────────────────────────

@Composable
private fun ThinkingBubble() {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        AstraAvatar()
        Spacer(Modifier.width(AstraSpacing.S))
        Box(
            modifier = Modifier
                .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.M),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(0, 150, 300).forEach { delay ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delay),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "dot-$delay",
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(alpha)
                            .background(AstraColors.Secondary, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursor-alpha",
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        AstraAvatar()
        Spacer(Modifier.width(AstraSpacing.S))
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        ) {
            Row {
                Text(
                    text = text,
                    style = AstraTypography.Body,
                    color = AstraColors.TextPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "▌",
                    style = AstraTypography.Body,
                    color = AstraColors.Secondary,
                    modifier = Modifier.alpha(cursorAlpha),
                )
            }
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun InputBar(
    question: String,
    canAsk: Boolean,
    isGenerating: Boolean,
    isListening: Boolean,
    voiceState: com.kevin.astra.domain.voice.SpeechRecognitionState,
    attachedPdf: AttachedPdf?,
    attachedImage: AttachedImage?,
    attachedEmail: AttachedEmail?,
    gmailSupported: Boolean,
    error: String?,
    onQuestionChanged: (String) -> Unit,
    onAsk: () -> Unit,
    onStop: () -> Unit,
    onPdfAttached: (ByteArray, String) -> Unit,
    onImageAttached: (ByteArray) -> Unit,
    onEmailAttached: (ByteArray, String) -> Unit,
    onAttachGmail: () -> Unit,
    onRemovePdf: () -> Unit,
    onRemoveImage: () -> Unit,
    onRemoveEmail: () -> Unit,
    onToggleVoice: () -> Unit,
) {
    val pdfLauncher = rememberPdfPickerLauncher(onPdfPicked = onPdfAttached)
    val imageLauncher = rememberImageCaptureLauncher(onImageCaptured = onImageAttached)
    val emailLauncher = rememberEmailPickerLauncher(onEmailPicked = onEmailAttached)
    var showAttachmentOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.Surface)
            .border(width = 1.dp, color = AstraColors.Border)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.M),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        AnimatedVisibility(visible = error != null) {
            error?.let { AstraErrorView(title = "Error", message = it) }
        }

        // Attachment chips
        AnimatedVisibility(visible = attachedPdf != null || attachedImage != null || attachedEmail != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                attachedEmail?.let { email ->
                    AttachmentChip(
                        label = when (email.status) {
                            AttachmentStatus.Indexing -> "⏳ Fetching…"
                            AttachmentStatus.Ready -> "📧 ${email.label}"
                            else -> "⚠ Email"
                        },
                        color = if (email.status == AttachmentStatus.Ready) AstraColors.Secondary else AstraColors.Error,
                        onRemove = onRemoveEmail,
                    )
                }
                attachedPdf?.let { pdf ->
                    AttachmentChip(
                        label = when (pdf.status) {
                            AttachmentStatus.Indexing -> "⏳ Indexing…"
                            AttachmentStatus.Ready -> "📄 ${pdf.fileName}"
                            else -> "⚠ ${pdf.fileName}"
                        },
                        color = if (pdf.status == AttachmentStatus.Ready) AstraColors.Primary else AstraColors.Error,
                        onRemove = onRemovePdf,
                    )
                }
                attachedImage?.let { img ->
                    AttachmentChip(
                        label = when (img.status) {
                            AttachmentStatus.Indexing -> "⏳ Analyzing…"
                            AttachmentStatus.Ready -> "📷 ${img.classification?.labels?.firstOrNull()?.label ?: "Image"}"
                            else -> "⚠ Failed"
                        },
                        color = if (img.status == AttachmentStatus.Ready) AstraColors.Secondary else AstraColors.Error,
                        onRemove = onRemoveImage,
                    )
                }
            }
        }

        // Attachment options panel
        AnimatedVisibility(visible = showAttachmentOptions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AstraColors.SurfaceElevated, RoundedCornerShape(14.dp))
                    .border(1.dp, AstraColors.Border, RoundedCornerShape(14.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
            ) {
                AttachmentOption(
                    icon = "📄",
                    label = "PDF",
                    active = attachedPdf != null,
                    onClick = { pdfLauncher(); showAttachmentOptions = false },
                )
                AttachmentOption(
                    icon = "📷",
                    label = "Photo",
                    active = attachedImage != null,
                    onClick = { imageLauncher(); showAttachmentOptions = false },
                )
                AttachmentOption(
                    icon = "📧",
                    label = "Email",
                    active = attachedEmail != null,
                    onClick = { emailLauncher(); showAttachmentOptions = false },
                )
                if (gmailSupported) {
                    AttachmentOption(
                        icon = "🔗",
                        label = "Gmail",
                        active = false,
                        onClick = { onAttachGmail(); showAttachmentOptions = false },
                    )
                }
                AttachmentOption(
                    icon = if (isListening) "⏹" else "🎤",
                    label = if (isListening) "Stop" else "Voice",
                    active = isListening,
                    onClick = { onToggleVoice(); if (!isListening) showAttachmentOptions = false },
                )
            }
        }

        // Voice partial transcript
        AnimatedVisibility(visible = isListening) {
            val partial = (voiceState as? com.kevin.astra.domain.voice.SpeechRecognitionState.Partial)?.text
            if (!partial.isNullOrBlank()) {
                Text(text = partial, style = AstraTypography.Caption, color = AstraColors.Secondary)
            }
        }

        // Unified input box: [+] [text field] [send]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(AstraColors.SurfaceElevated)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(28.dp))
                .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
            verticalAlignment = Alignment.Bottom,
        ) {
            // + button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (showAttachmentOptions) AstraColors.Primary.copy(alpha = 0.18f) else Color.Transparent,
                        CircleShape,
                    )
                    .clickable { showAttachmentOptions = !showAttachmentOptions },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (showAttachmentOptions) "×" else "+",
                    style = AstraTypography.Title,
                    color = if (showAttachmentOptions) AstraColors.Primary else AstraColors.TextSecondary,
                )
            }

            BasicTextField(
                value = question,
                onValueChange = onQuestionChanged,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 140.dp)
                    .padding(horizontal = AstraSpacing.XS)
                    .alpha(if (isListening) 0.5f else 1f),
                enabled = !isListening && !isGenerating,
                textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
                cursorBrush = SolidColor(AstraColors.Secondary),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AstraSpacing.S),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (question.isBlank() && !isListening) {
                            Text(text = "Message ASTRA…", style = AstraTypography.Body, color = AstraColors.TextDisabled)
                        }
                        inner()
                    }
                },
            )

            if (isGenerating) {
                StopButton(onClick = onStop)
            } else {
                SendButton(canAsk = canAsk, onClick = onAsk)
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(
                if (active) AstraColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (active) AstraColors.Primary.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.XS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = icon, style = AstraTypography.Body)
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = if (active) AstraColors.Primary else AstraColors.TextSecondary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── Reusable small components ─────────────────────────────────────────────────

@Composable
private fun SendButton(canAsk: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .alpha(if (canAsk) 1f else 0.4f)
            .then(
                if (canAsk) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(AstraColors.Primary, AstraColors.Secondary),
                        ),
                        CircleShape,
                    )
                } else {
                    Modifier.background(AstraColors.SurfaceElevated, CircleShape)
                },
            )
            .clickable(enabled = canAsk, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "↑", style = AstraTypography.Title, color = Color.White)
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(AstraColors.Error.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .border(1.dp, AstraColors.Error.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(AstraColors.Error, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun IconActionButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                if (active) AstraColors.Primary.copy(alpha = 0.18f) else AstraColors.SurfaceElevated,
                RoundedCornerShape(10.dp),
            )
            .border(1.dp, if (active) AstraColors.Primary else AstraColors.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = AstraTypography.Caption)
    }
}

@Composable
private fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "mic-alpha",
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .alpha(if (isListening) pulse else 1f)
            .background(
                if (isListening) AstraColors.Secondary.copy(alpha = 0.22f) else AstraColors.SurfaceElevated,
                RoundedCornerShape(10.dp),
            )
            .border(1.dp, if (isListening) AstraColors.Secondary else AstraColors.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = if (isListening) "⏹" else "🎤", style = AstraTypography.Caption)
    }
}

@Composable
private fun AttachmentChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
    ) {
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextPrimary)
        Text(
            text = "✕",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
            modifier = Modifier.clickable(onClick = onRemove),
        )
    }
}
