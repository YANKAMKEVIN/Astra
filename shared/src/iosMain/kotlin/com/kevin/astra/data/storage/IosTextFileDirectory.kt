package com.kevin.astra.data.storage

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * `Library/Application Support/<subdirectory>` in the app's sandbox: private to the app, not shown
 * in the Files app, removed with the app (the iOS counterpart of Android's `filesDir`).
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosTextFileDirectory(private val subdirectory: String) : TextFileDirectory {

    private val fileManager: NSFileManager get() = NSFileManager.defaultManager

    private val path: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: NSTemporaryDirectory()
        "$base/$subdirectory"
    }

    override fun read(name: String): String? =
        NSString.stringWithContentsOfFile("$path/$name", encoding = NSUTF8StringEncoding, error = null)

    override fun write(name: String, text: String): Boolean {
        fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        // atomically = true: Foundation writes a temporary file, then renames it over the old one.
        @Suppress("CAST_NEVER_SUCCEEDS")
        return (text as NSString).writeToFile("$path/$name", atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun list(): List<String> =
        fileManager.contentsOfDirectoryAtPath(path, error = null)?.filterIsInstance<String>() ?: emptyList()

    override fun delete(name: String): Boolean = fileManager.removeItemAtPath("$path/$name", error = null)
}
