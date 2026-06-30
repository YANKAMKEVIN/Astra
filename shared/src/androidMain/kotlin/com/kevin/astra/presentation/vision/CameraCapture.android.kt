package com.kevin.astra.presentation.vision

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImageCaptureLauncher(onImageCaptured: (ByteArray) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val out = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 85, out)
            onImageCaptured(out.toByteArray())
        }
    }
    return { launcher.launch(null) }
}
