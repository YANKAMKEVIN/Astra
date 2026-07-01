package com.kevin.astra.presentation.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberEmailPickerLauncher(onEmailPicked: (bytes: ByteArray, fileName: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = resolveEmailFileName(context, uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        onEmailPicked(bytes, fileName)
    }
    return remember {
        {
            launcher.launch(
                arrayOf(
                    "message/rfc822",   // .eml
                    "application/mbox", // .mbox
                    "text/plain",       // fallback for .eml / .mbox served as text
                )
            )
        }
    }
}

private fun resolveEmailFileName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        if (nameIndex >= 0) it.getString(nameIndex) else "email.eml"
    } ?: "email.eml"
}
