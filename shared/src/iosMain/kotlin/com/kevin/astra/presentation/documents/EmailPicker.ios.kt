package com.kevin.astra.presentation.documents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberEmailPickerLauncher(onEmailPicked: (bytes: ByteArray, fileName: String) -> Unit): () -> Unit =
    remember { {} } // iOS email import not yet implemented
