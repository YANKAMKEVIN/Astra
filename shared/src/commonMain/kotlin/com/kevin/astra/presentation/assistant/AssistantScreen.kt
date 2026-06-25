package com.kevin.astra.presentation.assistant

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing

@Composable
fun AssistantScreen(contentPadding: PaddingValues) {
    AstraScreen(
        title = "Assistant",
        description = "A private workspace for future on-device AI conversations.",
        contentPadding = contentPadding,
    ) {
        AstraCard(
            title = "Ask ASTRA",
            subtitle = "Prompt composition and local inference are not enabled yet.",
            status = "OFFLINE",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            AstraButton(
                text = "Local inference coming soon",
                onClick = {},
                style = AstraButtonStyle.Ghost,
            )
        }
        AstraCard(
            title = "Response metrics",
            subtitle = "Latency, throughput and memory metrics will appear here.",
        )
    }
}
