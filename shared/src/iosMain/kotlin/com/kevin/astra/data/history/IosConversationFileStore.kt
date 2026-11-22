package com.kevin.astra.data.history

import com.kevin.astra.data.storage.IosTextFileDirectory

// Same layout as Android: conversations/<id>.json in the app's private storage.
actual fun createConversationFileStore(): ConversationFileStore =
    DirectoryConversationFileStore(IosTextFileDirectory("conversations"))
