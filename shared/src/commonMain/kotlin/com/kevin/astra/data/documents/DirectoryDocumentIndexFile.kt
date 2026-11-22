package com.kevin.astra.data.documents

import com.kevin.astra.data.storage.TextFileDirectory

/** The saved RAG index as `index.json` in [directory] (same file name as on Android). */
class DirectoryDocumentIndexFile(private val directory: TextFileDirectory) : DocumentIndexFile {

    override fun read(): String? = directory.read(FILE_NAME)

    override fun write(text: String) {
        check(directory.write(FILE_NAME, text)) { "Could not save the document index" }
    }

    override fun delete() {
        directory.delete(FILE_NAME)
    }

    companion object {
        const val FILE_NAME = "index.json"
    }
}
