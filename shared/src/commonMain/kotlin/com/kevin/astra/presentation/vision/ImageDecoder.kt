package com.kevin.astra.presentation.vision

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?
