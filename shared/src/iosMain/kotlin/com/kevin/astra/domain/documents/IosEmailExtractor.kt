package com.kevin.astra.domain.documents

class IosEmailExtractor : EmailExtractor {
    override fun extractEml(bytes: ByteArray, fileName: String): LoadedEmailDocument =
        LoadedEmailDocument(fileName = fileName, emailCount = 1, rawText = bytes.decodeToString())

    override fun extractMbox(bytes: ByteArray, fileName: String): LoadedEmailDocument =
        LoadedEmailDocument(fileName = fileName, emailCount = 1, rawText = bytes.decodeToString())
}

actual fun createEmailExtractor(): EmailExtractor = IosEmailExtractor()
