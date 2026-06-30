package com.kevin.astra.presentation.documents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfPicked: (bytes: ByteArray, fileName: String) -> Unit,
): () -> Unit {
    val callback = remember { onPdfPicked }
    return remember {
        { showPdfPicker(callback) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun showPdfPicker(onPdfPicked: (ByteArray, String) -> Unit) {
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypePDF))
    picker.allowsMultipleSelection = false

    val delegate = PdfDocumentPickerDelegate(onPdfPicked)
    picker.delegate = delegate

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(picker, animated = true, completion = null)

    retainedDelegates[picker] = delegate
}

private val retainedDelegates = mutableMapOf<Any, Any>()

@OptIn(ExperimentalForeignApi::class)
private class PdfDocumentPickerDelegate(
    private val onPdfPicked: (ByteArray, String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        retainedDelegates.remove(controller)
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val fileName = url.lastPathComponent ?: "document.pdf"

        url.startAccessingSecurityScopedResource()
        val data = NSData.dataWithContentsOfURL(url)
        url.stopAccessingSecurityScopedResource()

        data ?: return

        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        onPdfPicked(bytes, fileName)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        retainedDelegates.remove(controller)
    }
}
