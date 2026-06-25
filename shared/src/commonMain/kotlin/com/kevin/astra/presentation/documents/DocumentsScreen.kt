package com.kevin.astra.presentation.documents

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
fun DocumentsScreen(contentPadding: PaddingValues) {
    AstraScreen(
        title = "Documents",
        description = "Secure local document operations will be managed here.",
        contentPadding = contentPadding,
    ) {
        AstraCard(
            title = "Document workspace",
            subtitle = "No documents are indexed. Import and parsing are out of scope.",
            status = "EMPTY",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            AstraButton(
                text = "Document import coming soon",
                onClick = {},
                style = AstraButtonStyle.Ghost,
            )
        }
        AstraCard(
            title = "Context preview",
            subtitle = "Extracted local context will remain on this device.",
        )
    }
}
