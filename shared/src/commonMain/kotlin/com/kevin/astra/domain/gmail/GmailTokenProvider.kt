package com.kevin.astra.domain.gmail

/**
 * Supplies a valid Gmail OAuth access token. The platform-specific OAuth flow (Phase B)
 * implements this; it is abstracted here so the API client and repository stay
 * platform-agnostic and fully testable with a fake token.
 */
fun interface GmailTokenProvider {
    suspend fun accessToken(): String
}
