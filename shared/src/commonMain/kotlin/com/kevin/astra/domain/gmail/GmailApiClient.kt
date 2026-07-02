package com.kevin.astra.domain.gmail

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
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
        val response: GmailListResponse = httpClient.get("$baseUrl/users/me/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("maxResults", maxResults)
            if (!query.isNullOrBlank()) parameter("q", query)
        }.body()
        return response.messages.map { it.id }
    }

    /** Fetches one message in raw RFC 2822 form (base64url-decoded to bytes). */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getRawMessage(id: String): ByteArray {
        val token = tokenProvider.accessToken()
        val message: GmailRawMessage = httpClient.get("$baseUrl/users/me/messages/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("format", "raw")
        }.body()
        // Gmail encodes `raw` as base64url, usually without padding.
        return Base64.UrlSafe
            .withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
            .decode(message.raw)
    }
}

/** Builds an [HttpClient] configured for the Gmail JSON API using the platform's default engine. */
fun defaultGmailHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
