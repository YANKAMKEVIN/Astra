package com.kevin.astra.presentation.vision

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImageCaptureLauncher(onImageCaptured: (ByteArray) -> Unit): () -> Unit

/** Opens the system photo picker; [onImagePicked] receives the chosen image as JPEG/PNG bytes. */
@Composable
expect fun rememberGalleryImageLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit
