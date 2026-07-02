package com.kevin.astra.domain.gmail

import com.kevin.astra.domain.documents.DefaultEmailExtractor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

@OptIn(ExperimentalEncodingApi::class)
class GmailRepositoryTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun rawMessageJson(id: String, rfc822: String): String {
        val encoded = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
            .encode(rfc822.encodeToByteArray())
        return """{"id":"$id","raw":"$encoded"}"""
    }

    private fun buildRepository(
        listResponse: String,
        rawByPath: Map<String, String>,
        capturedAuth: MutableList<String?> = mutableListOf(),
        capturedQueries: MutableList<String?> = mutableListOf(),
    ): GmailRepository {
        val engine = MockEngine { request ->
            capturedAuth += request.headers[HttpHeaders.Authorization]
            val url = request.url
            when {
                url.encodedPath.endsWith("/messages") -> {
                    capturedQueries += url.parameters["q"]
                    respond(ByteReadChannel(listResponse), HttpStatusCode.OK, jsonHeaders)
                }
                else -> {
                    val id = url.encodedPath.substringAfterLast('/')
                    respond(ByteReadChannel(rawByPath.getValue(id)), HttpStatusCode.OK, jsonHeaders)
                }
            }
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val apiClient = GmailApiClient(
            httpClient = httpClient,
            tokenProvider = { "test-access-token" },
        )
        return GmailRepository(apiClient, DefaultEmailExtractor())
    }

    @Test
    fun fetchesListsRawMessagesAndParsesThem() = runBlocking {
        val auth = mutableListOf<String?>()
        val repo = buildRepository(
            listResponse = """{"messages":[{"id":"m1"},{"id":"m2"}],"resultSizeEstimate":2}""",
            rawByPath = mapOf(
                "m1" to rawMessageJson("m1", "Subject: Invoice #1\nFrom: billing@shop.com\n\nAmount due: 42."),
                "m2" to rawMessageJson("m2", "Subject: Meeting\nFrom: boss@corp.com\n\nStandup at 9."),
            ),
            capturedAuth = auth,
        )

        val docs = repo.fetchMessages(maxResults = 20)

        assertEquals(2, docs.size)
        assertContains(docs[0].rawText, "Subject: Invoice #1")
        assertContains(docs[0].rawText, "Amount due: 42.")
        assertContains(docs[1].rawText, "Standup at 9.")
        // Bearer token is attached to every request (list + 2 gets).
        assertTrue(auth.all { it == "Bearer test-access-token" })
        assertEquals(3, auth.size)
    }

    @Test
    fun forwardsSearchQueryToTheApi() = runBlocking {
        val queries = mutableListOf<String?>()
        val repo = buildRepository(
            listResponse = """{"messages":[{"id":"m1"}]}""",
            rawByPath = mapOf("m1" to rawMessageJson("m1", "Subject: Bank\n\nBody.")),
            capturedQueries = queries,
        )

        repo.fetchMessages(query = "from:bank", maxResults = 5)

        assertEquals(listOf<String?>("from:bank"), queries)
    }

    @Test
    fun mergesMessagesIntoSingleDocument() = runBlocking {
        val repo = buildRepository(
            listResponse = """{"messages":[{"id":"m1"},{"id":"m2"}]}""",
            rawByPath = mapOf(
                "m1" to rawMessageJson("m1", "Subject: One\n\nFirst."),
                "m2" to rawMessageJson("m2", "Subject: Two\n\nSecond."),
            ),
        )

        val doc = repo.fetchAsSingleDocument(maxResults = 20)

        assertEquals(2, doc.emailCount)
        assertContains(doc.rawText, "First.")
        assertContains(doc.rawText, "Second.")
        assertContains(doc.rawText, "---") // mbox-style separator
    }

    @Test
    fun handlesEmptyInbox() = runBlocking {
        val repo = buildRepository(
            listResponse = """{"resultSizeEstimate":0}""",
            rawByPath = emptyMap(),
        )

        val docs = repo.fetchMessages()

        assertTrue(docs.isEmpty())
    }
}
