package com.kevin.astra.presentation.vision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject

@Composable
actual fun rememberImageCaptureLauncher(onImageCaptured: (ByteArray) -> Unit): () -> Unit {
    val callback = remember { onImageCaptured }
    return remember {
        { showImagePicker(callback) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun showImagePicker(onImageCaptured: (ByteArray) -> Unit) {
    val config = PHPickerConfiguration()
    config.filter = PHPickerFilter.imagesFilter
    config.selectionLimit = 1

    val picker = PHPickerViewController(configuration = config)
    val delegate = ImagePickerDelegate(onImageCaptured)
    picker.delegate = delegate

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootVC?.presentViewController(picker, animated = true, completion = null)

    retainedDelegates[picker] = delegate
}

private val retainedDelegates = mutableMapOf<Any, Any>()

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onImageCaptured: (ByteArray) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        retainedDelegates.remove(picker)

        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return

        // Load raw JPEG data directly — avoids ObjC class type mismatch with loadObjectOfClass
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.jpeg") { data, _ ->
            if (data != null) {
                onImageCaptured(data.toByteArray())
                return@loadDataRepresentationForTypeIdentifier
            }
            // Fallback to PNG if JPEG not available
            result.itemProvider.loadDataRepresentationForTypeIdentifier("public.png") { pngData, _ ->
                pngData?.let { onImageCaptured(it.toByteArray()) }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
