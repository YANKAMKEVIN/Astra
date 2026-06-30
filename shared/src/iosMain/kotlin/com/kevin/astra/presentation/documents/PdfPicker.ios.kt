package com.kevin.astra.presentation.documents

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPdfPickerLauncher(onPdfPicked: (bytes: ByteArray, fileName: String) -> Unit): () -> Unit = {}
