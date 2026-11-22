package com.kevin.astra.data.documents

import com.kevin.astra.data.storage.IosTextFileDirectory

// Same layout as Android: rag/index.json in the app's private storage.
actual fun createDocumentIndexFile(): DocumentIndexFile = DirectoryDocumentIndexFile(IosTextFileDirectory("rag"))
