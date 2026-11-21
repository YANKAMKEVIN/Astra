package com.kevin.astra.domain.gmail

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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

    @Test
    fun unauthorizedAndForbiddenAskForAReconnection() = runBlocking {
        val unauthorized = assertFailsWith<GmailApiException> {
            client(HttpStatusCode.Unauthorized, unauthorizedBody).listMessageIds()
        }
        assertTrue(unauthorized.needsReconnect)
        assertContains(unauthorized.message.orEmpty(), "Reconnect your Gmail account")

        val forbidden = assertFailsWith<GmailApiException> {
            client(HttpStatusCode.Forbidden, """{"error":{"code":403,"status":"PERMISSION_DENIED"}}""").getRawMessage("m1")
        }
        assertTrue(forbidden.needsReconnect)
        assertContains(forbidden.message.orEmpty(), "refused access")
    }

    @Test
    fun otherErrorsDoNotAskForAReconnection() = runBlocking {
        val error = assertFailsWith<GmailApiException> {
            client(HttpStatusCode.InternalServerError, """{"error":{"code":500}}""").listMessageIds()
        }
        assertFalse(error.needsReconnect)
        assertContains(error.message.orEmpty(), "500")
    }

    /** Three pages of 20 ids, served according to the `pageToken` the client sends back. */
    private fun pagedClient(requests: MutableList<Map<String, String?>>): GmailApiClient {
        val pages = mapOf(
            null to Pair((1..20).map { "m$it" }, "p2"),
            "p2" to Pair((21..40).map { "m$it" }, "p3"),
            "p3" to Pair((41..50).map { "m$it" }, null),
        )
        val engine = MockEngine { request ->
            val token = request.url.parameters["pageToken"]
            requests += mapOf("pageToken" to token, "maxResults" to request.url.parameters["maxResults"])
            val (ids, next) = pages.getValue(token)
            val messages = ids.joinToString(",") { """{"id":"$it"}""" }
            val nextField = next?.let { ""","nextPageToken":"$it"""" } ?: ""
            respond("""{"messages":[$messages]$nextField}""", HttpStatusCode.OK, jsonHeaders)
        }
        return GmailApiClient(HttpClient(engine) { gmailDefaults() }, tokenProvider = { "token" })
    }

    @Test
    fun followsNextPageTokenBeyondTheFirstPage() = runBlocking {
        val requests = mutableListOf<Map<String, String?>>()
        val page = pagedClient(requests).listMessages(maxResults = 45)

        assertEquals((1..45).map { "m$it" }, page.ids)
        assertTrue(page.hasMore)
        assertEquals(listOf(null, "p2", "p3"), requests.map { it["pageToken"] })
        // Each page only asks for what is still missing.
        assertEquals(listOf("45", "25", "5"), requests.map { it["maxResults"] })
    }

    @Test
    fun stopsAtTheLastPage() = runBlocking {
        val requests = mutableListOf<Map<String, String?>>()
        val page = pagedClient(requests).listMessages(maxResults = 100)

        assertEquals(50, page.ids.size)
        assertFalse(page.hasMore)
        assertEquals(3, requests.size)
    }

    @Test
    fun doesNotRequestASecondPageWhenTheFirstIsEnough() = runBlocking {
        val requests = mutableListOf<Map<String, String?>>()
        val page = pagedClient(requests).listMessages(maxResults = 20)

        assertEquals(20, page.ids.size)
        assertTrue(page.hasMore)
        assertEquals(1, requests.size)
    }

    @Test
    fun anEmptyPageWithATokenDoesNotLoopForever() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{"nextPageToken":"again"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val client = GmailApiClient(HttpClient(engine) { gmailDefaults() }, tokenProvider = { "token" })

        val page = client.listMessages(maxResults = 20)

        assertTrue(page.ids.isEmpty())
        assertEquals(1, calls)
    }
}
