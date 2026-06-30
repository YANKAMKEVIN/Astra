package com.kevin.astra.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.domain.export.ExportFormat
import com.kevin.astra.domain.history.ChatConversation
import com.kevin.astra.domain.history.ChatMessage

@Composable
fun ConversationHistoryScreen(
    viewModel: ConversationHistoryViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(ConversationHistoryIntent.LoadHistory)
    }

    if (state.selectedConversation != null) {
        AstraScreen(
            title = state.selectedConversation!!.title,
            description = "${state.selectedConversation!!.modelName} · ${state.selectedConversation!!.industry}",
            contentPadding = contentPadding,
            showDemoIndicator = false,
        ) {
            ConversationDetailContent(
                conversation = state.selectedConversation!!,
                onClose = { viewModel.dispatch(ConversationHistoryIntent.CloseDetail) },
                onDelete = {
                    viewModel.dispatch(ConversationHistoryIntent.DeleteConversation(state.selectedConversation!!.id))
                },
                onExport = { format ->
                    viewModel.dispatch(ConversationHistoryIntent.ExportConversation(state.selectedConversation!!.id, format))
                },
            )
        }
    } else {
        AstraScreen(
            title = "History",
            description = "Your past ASTRA conversations.",
            contentPadding = contentPadding,
            showDemoIndicator = false,
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.dispatch(ConversationHistoryIntent.UpdateSearch(it)) },
            )
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Loading…", style = AstraTypography.Body, color = AstraColors.TextSecondary)
                }
            } else if (state.conversations.isEmpty()) {
                EmptyHistoryView(hasQuery = state.searchQuery.isNotBlank())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                    state.conversations.forEach { conversation ->
                        ConversationListItem(
                            conversation = conversation,
                            onClick = {
                                viewModel.dispatch(ConversationHistoryIntent.SelectConversation(conversation.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search conversations…", color = AstraColors.TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AstraColors.Primary,
            unfocusedBorderColor = AstraColors.Border,
            cursorColor = AstraColors.Primary,
            focusedTextColor = AstraColors.TextPrimary,
            unfocusedTextColor = AstraColors.TextPrimary,
        ),
    )
}

@Composable
private fun ConversationListItem(conversation: ChatConversation, onClick: () -> Unit) {
    AstraCard(
        title = conversation.title,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            MetaChip(conversation.modelName)
            MetaChip(conversation.industry)
        }
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = conversation.createdAt,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
private fun MetaChip(label: String) {
    if (label.isBlank()) return
    Text(
        text = label,
        style = AstraTypography.Caption,
        color = AstraColors.Primary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AstraColors.Primary.copy(alpha = 0.12f))
            .padding(horizontal = AstraSpacing.S, vertical = 2.dp),
    )
}

@Composable
private fun ConversationDetailContent(
    conversation: ChatConversation,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onExport: (ExportFormat) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AstraButton(text = "← Back", onClick = onClose, style = AstraButtonStyle.Secondary)
        AstraButton(text = "Delete", onClick = onDelete, style = AstraButtonStyle.Danger)
    }
    Spacer(Modifier.height(AstraSpacing.S))
    Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        ExportFormat.entries.forEach { format ->
            AstraButton(
                text = "↗ ${format.label}",
                onClick = { onExport(format) },
                style = AstraButtonStyle.Ghost,
            )
        }
    }
    Spacer(Modifier.height(AstraSpacing.M))
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.M)) {
        conversation.messages.forEach { message ->
            MessageBubble(message)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bgColor = if (isUser) AstraColors.Primary.copy(alpha = 0.15f) else AstraColors.SurfaceElevated
    val label = if (isUser) "You" else "ASTRA"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = if (isUser) AstraColors.Primary else AstraColors.TextSecondary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = message.content,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
        )
    }
}

@Composable
private fun EmptyHistoryView(hasQuery: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasQuery) "No conversations match your search."
            else "No conversations yet.\nAsk ASTRA something to get started.",
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
        )
    }
}
