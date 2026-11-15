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
    suspend fun listMessageIds(query: String? = null, maxResults: Int = 20): List<String> {
        val token = tokenProvider.accessToken()
        val response: GmailListResponse = gmailCall {
            httpClient.get("$baseUrl/users/me/messages") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("maxResults", maxResults)
                if (!query.isNullOrBlank()) parameter("q", query)
            }.body()
        }
        return response.messages.map { it.id }
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

/** A Gmail API call answered with an error status (401: the token is no longer valid). */
class GmailApiException(val status: HttpStatusCode, cause: Throwable? = null) :
    Exception("Gmail request failed: ${status.value} ${status.description}.", cause)
