package com.kevin.astra.domain.gmail

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The Gmail client must surface HTTP errors: with an expired token Gmail answers 401 with an error
 * body, which used to decode into an empty message list ("no e-mails") instead of an error.
 */
class GmailApiClientTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(status: HttpStatusCode, body: String): GmailApiClient {
        val engine = MockEngine { respond(body, status, jsonHeaders) }
        // The same configuration as defaultGmailHttpClient(), on a mock engine.
        return GmailApiClient(HttpClient(engine) { gmailDefaults() }, tokenProvider = { "expired-token" })
    }

    private val unauthorizedBody =
        """{"error":{"code":401,"message":"Request had invalid authentication credentials.","status":"UNAUTHENTICATED"}}"""

    @Test
    fun anUnauthorizedListIsAnErrorNotAnEmptyInbox() = runBlocking {
        val error = assertFailsWith<GmailApiException> {
            client(HttpStatusCode.Unauthorized, unauthorizedBody).listMessageIds()
        }
        assertEquals(HttpStatusCode.Unauthorized, error.status)
    }

    @Test
    fun aServerErrorOnAMessageIsAnError() = runBlocking {
        val error = assertFailsWith<GmailApiException> {
            client(HttpStatusCode.ServiceUnavailable, """{"error":{"code":503}}""").getRawMessage("m1")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, error.status)
    }

    @Test
    fun aSuccessfulListIsDecoded() = runBlocking {
        val ids = client(HttpStatusCode.OK, """{"messages":[{"id":"m1"}]}""").listMessageIds()
        assertEquals(listOf("m1"), ids)
    }
}
