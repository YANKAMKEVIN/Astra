package com.kevin.astra.domain.gmail

import kotlinx.serialization.Serializable

/** Reference returned by `users.messages.list` — only ids, no content. */
@Serializable
data class GmailMessageRef(
    val id: String,
    val threadId: String? = null,
)

/** Response of `users.messages.list`. */
@Serializable
data class GmailListResponse(
    val messages: List<GmailMessageRef> = emptyList(),
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int = 0,
)

/**
 * Message resource from `users.messages.get?format=raw`. The [raw] field is the whole
 * RFC 2822 message, base64url-encoded.
 */
@Serializable
data class GmailRawMessage(
    val id: String,
    val threadId: String? = null,
    val raw: String = "",
)
