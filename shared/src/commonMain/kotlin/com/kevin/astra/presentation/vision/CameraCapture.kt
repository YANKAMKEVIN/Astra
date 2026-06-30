package com.kevin.astra.presentation.vision

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImageCaptureLauncher(onImageCaptured: (ByteArray) -> Unit): () -> Unit
