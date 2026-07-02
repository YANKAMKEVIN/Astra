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
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

@Composable
actual fun rememberEmailPickerLauncher(
    onEmailPicked: (bytes: ByteArray, fileName: String) -> Unit,
): () -> Unit {
    val callback = remember { onEmailPicked }
    return remember {
        { showEmailPicker(callback) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun showEmailPicker(onEmailPicked: (ByteArray, String) -> Unit) {
    // .eml / .mbox have no system UTType; derive them from the filename extension and fall back
    // to plain text and generic data so the files are always selectable.
    val contentTypes = listOfNotNull(
        UTType.typeWithFilenameExtension("eml"),
        UTType.typeWithFilenameExtension("mbox"),
        UTTypePlainText,
        UTTypeData,
    )
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes)
    picker.allowsMultipleSelection = false

    val delegate = EmailDocumentPickerDelegate(onEmailPicked)
    picker.delegate = delegate

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(picker, animated = true, completion = null)

    retainedDelegates[picker] = delegate
}

private val retainedDelegates = mutableMapOf<Any, Any>()

@OptIn(ExperimentalForeignApi::class)
private class EmailDocumentPickerDelegate(
    private val onEmailPicked: (ByteArray, String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        retainedDelegates.remove(controller)
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val fileName = url.lastPathComponent ?: "email.eml"

        url.startAccessingSecurityScopedResource()
        val data = NSData.dataWithContentsOfURL(url)
        url.stopAccessingSecurityScopedResource()

        data ?: return

        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
        onEmailPicked(bytes, fileName)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        retainedDelegates.remove(controller)
    }
}
