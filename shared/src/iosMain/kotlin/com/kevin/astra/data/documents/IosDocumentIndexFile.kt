package com.kevin.astra.data.documents

// Not persisted on iOS yet (like the conversation history): the library stays in memory.
actual fun createDocumentIndexFile(): DocumentIndexFile = NoOpDocumentIndexFile
