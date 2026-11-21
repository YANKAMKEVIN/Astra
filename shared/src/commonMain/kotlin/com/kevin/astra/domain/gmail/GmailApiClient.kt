package com.kevin.astra.domain.gmail

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json

/**
 * Thin client over the Gmail REST API. It never handles OAuth itself — it asks the injected
 * [GmailTokenProvider] for a bearer token per request, which keeps it platform-agnostic and
 * testable with Ktor's MockEngine.
 */
class GmailApiClient(
    private val httpClient: HttpClient,
    private val tokenProvider: GmailTokenProvider,
    private val baseUrl: String = "https://gmail.googleapis.com/gmail/v1",
) {
    /** Lists message ids for the authenticated user, optionally filtered by a Gmail search [query]. */
    suspend fun listMessageIds(query: String? = null, maxResults: Int = 20): List<String> =
        listMessages(query, maxResults).ids

    /**
     * Lists up to [maxResults] message ids, following `nextPageToken` across pages: Gmail returns
     * at most [MAX_PAGE_SIZE] ids per page (and often fewer than requested), so a single call
     * capped the inbox at its first page. [GmailIdPage.hasMore] says whether older ids remain.
     */
    suspend fun listMessages(query: String? = null, maxResults: Int = 20): GmailIdPage {
        val ids = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val token = tokenProvider.accessToken()
            val remaining = maxResults - ids.size
            val response: GmailListResponse = gmailCall {
                httpClient.get("$baseUrl/users/me/messages") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("maxResults", remaining.coerceAtMost(MAX_PAGE_SIZE))
                    if (!query.isNullOrBlank()) parameter("q", query)
                    if (pageToken != null) parameter("pageToken", pageToken)
                }.body()
            }
            ids += response.messages.map { it.id }
            pageToken = response.nextPageToken?.takeIf { it.isNotBlank() }
            // An empty page with a token would loop forever: stop there.
        } while (pageToken != null && ids.size < maxResults && response.messages.isNotEmpty())
        return GmailIdPage(ids = ids.take(maxResults), hasMore = pageToken != null || ids.size > maxResults)
    }

    /** Fetches one message in raw RFC 2822 form (base64url-decoded to bytes). */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getRawMessage(id: String): ByteArray {
        val token = tokenProvider.accessToken()
        val message: GmailRawMessage = gmailCall {
            httpClient.get("$baseUrl/users/me/messages/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("format", "raw")
            }.body()
        }
        // Gmail encodes `raw` as base64url, usually without padding.
        return Base64.UrlSafe
            .withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
            .decode(message.raw)
    }
}

private suspend fun <T> gmailCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ResponseException) {
        throw GmailApiException(e.response.status, e)
    }

/** Builds an [HttpClient] configured for the Gmail JSON API using the platform's default engine. */
fun defaultGmailHttpClient(): HttpClient = HttpClient { gmailDefaults() }

internal fun HttpClientConfig<*>.gmailDefaults() {
    // Error statuses (401 expired token, 403, 5xx) must fail the call, not decode into empty data.
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

/** Ids returned by [GmailApiClient.listMessages]; [hasMore] is true when Gmail has older messages. */
data class GmailIdPage(val ids: List<String>, val hasMore: Boolean)

/** Gmail's documented upper bound for `maxResults` on `users.messages.list`. */
internal const val MAX_PAGE_SIZE = 500

/**
 * A Gmail API call answered with an error status. Its [message] is meant for the user: a 401 or a
 * 403 is not an empty inbox, it asks for a reconnection or for the missing permission.
 */
class GmailApiException(val status: HttpStatusCode, cause: Throwable? = null) :
    Exception(userMessageFor(status), cause) {
    /** True when reconnecting the Gmail account is the way out (expired, revoked or refused access). */
    val needsReconnect: Boolean
        get() = status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden

    private companion object {
        fun userMessageFor(status: HttpStatusCode): String = when (status) {
            HttpStatusCode.Unauthorized ->
                "Your Gmail session has expired. Reconnect your Gmail account and try again."
            HttpStatusCode.Forbidden ->
                "Gmail refused access. Reconnect your account and allow Astra to read your mail."
            HttpStatusCode.TooManyRequests ->
                "Gmail is limiting requests right now. Wait a minute and try again."
            else -> "Gmail request failed (${status.value} ${status.description})."
        }
    }
}
