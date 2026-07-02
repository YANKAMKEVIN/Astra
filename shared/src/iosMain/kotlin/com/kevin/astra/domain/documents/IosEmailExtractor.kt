package com.kevin.astra.domain.documents

// The RFC 2822 parsing lives in the shared [DefaultEmailExtractor]; iOS just wires the actual.
actual fun createEmailExtractor(): EmailExtractor = DefaultEmailExtractor()
