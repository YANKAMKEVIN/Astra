package com.kevin.astra.presentation.documents

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPdfPickerLauncher(onPdfPicked: (bytes: ByteArray, fileName: String) -> Unit): () -> Unit
