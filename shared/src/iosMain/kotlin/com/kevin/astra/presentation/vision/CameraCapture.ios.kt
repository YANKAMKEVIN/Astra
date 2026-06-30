package com.kevin.astra.presentation.vision

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImageCaptureLauncher(onImageCaptured: (ByteArray) -> Unit): () -> Unit = { }
