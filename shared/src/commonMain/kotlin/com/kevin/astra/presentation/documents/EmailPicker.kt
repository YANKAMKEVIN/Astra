package com.kevin.astra.presentation.documents

import androidx.compose.runtime.Composable

@Composable
expect fun rememberEmailPickerLauncher(onEmailPicked: (bytes: ByteArray, fileName: String) -> Unit): () -> Unit
