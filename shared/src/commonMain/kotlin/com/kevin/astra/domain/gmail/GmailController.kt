package com.kevin.astra.domain.gmail

import com.kevin.astra.domain.documents.LoadedEmailDocument

/**
 * Source of Gmail messages, expressed at the domain level so the UI/ViewModel never touch the
 * HTTP client directly. [GmailRepository] is the production implementation.
 */
interface GmailMessageSource {
    suspend fun fetchAsSingleDocument(
        query: String? = null,
        maxResults: Int = 20,
        label: String = "Gmail",
    ): LoadedEmailDocument
}

/**
 * Platform integration point for the interactive parts of Gmail (OAuth consent, connection state).
 * The Android/iOS app provides an implementation and publishes it via [GmailIntegration]; shared
 * code stays free of platform OAuth SDKs.
 */
interface GmailController {
    /** True when a Gmail client id is configured and the platform supports the flow. */
    val isSupported: Boolean

    /** True once the user has authorized read-only Gmail access. */
    fun isConnected(): Boolean

    /** Launches the interactive consent screen. */
    fun connect()

    /** Clears the stored authorization. */
    fun disconnect()

    /** The token provider backing API calls once connected, or null when unavailable. */
    fun tokenProvider(): GmailTokenProvider?
}

/** Holder set by the app at startup so shared code can reach the platform [GmailController]. */
object GmailIntegration {
    var controller: GmailController? = null
}
