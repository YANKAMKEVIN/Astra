package com.kevin.astra.data.documents

import android.content.Context
import com.kevin.astra.app.di.androidAppContext
import java.io.File

actual fun createDocumentIndexFile(): DocumentIndexFile = AndroidDocumentIndexFile(androidAppContext())

/** The saved RAG index, in the app's private files directory (removed with the app's data). */
private class AndroidDocumentIndexFile(private val context: Context) : DocumentIndexFile {

    private val file: File get() = context.filesDir.resolve("rag").also { it.mkdirs() }.resolve("index.json")

    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        // Write then rename, so a crash mid-write never leaves a truncated index behind.
        val tmp = File(file.parentFile, "index.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    override fun delete() {
        file.delete()
    }
}
