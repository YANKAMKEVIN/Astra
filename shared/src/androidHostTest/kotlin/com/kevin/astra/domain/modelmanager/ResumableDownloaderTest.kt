package com.kevin.astra.domain.modelmanager

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Exercises the real HttpURLConnection path against a local server that misbehaves on purpose. */
class ResumableDownloaderTest {
    private val payload = Random(42).nextBytes(1_000_000)
    private val rangeHeaders = CopyOnWriteArrayList<String?>()
    private var server: HttpServer? = null
    private val partFile = File.createTempFile("model", ".part").apply { delete() }

    @AfterTest
    fun tearDown() {
        server?.stop(0)
        partFile.delete()
    }

    /**
     * @param dropFirstAfter if set, the first response announces the full length but the
     *   connection is closed after this many bytes (a lock-screen network cut).
     */
    private fun serve(
        dropFirstAfter: Int? = null,
        honorRange: Boolean = true,
        status: Int? = null,
    ): String {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/model.litertlm") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")
            rangeHeaders += range
            if (status != null) {
                exchange.sendResponseHeaders(status, -1)
                exchange.close()
                return@createContext
            }
            val from = range?.removePrefix("bytes=")?.removeSuffix("-")?.toInt()
            when {
                from != null && honorRange && from >= payload.size -> {
                    exchange.sendResponseHeaders(416, -1)
                    exchange.close()
                }
                from != null && honorRange -> {
                    val rest = payload.copyOfRange(from, payload.size)
                    exchange.responseHeaders.add("Content-Range", "bytes $from-${payload.size - 1}/${payload.size}")
                    exchange.sendResponseHeaders(206, rest.size.toLong())
                    exchange.responseBody.use { it.write(rest) }
                }
                else -> {
                    exchange.sendResponseHeaders(200, payload.size.toLong())
                    val cut = if (rangeHeaders.size == 1) dropFirstAfter else null
                    exchange.responseBody.use { it.write(if (cut != null) payload.copyOf(cut) else payload) }
                }
            }
        }
        http.start()
        server = http
        return "http://127.0.0.1:${http.address.port}/model.litertlm"
    }

    private val downloader = ResumableDownloader(maxAttempts = 3, backoffMillis = { 0L })

    private fun download(url: String) = runBlocking {
        downloader.download(url, authToken = null, partFile = partFile) { _, _ -> }
    }

    @Test
    fun resumesFromTheBytesAlreadyOnDiskAfterADroppedConnection() {
        val url = serve(dropFirstAfter = 300_000)

        assertEquals(ResumableDownloader.Result.Completed, download(url))

        assertContentEquals(payload, partFile.readBytes())
        assertEquals(2, rangeHeaders.size)
        assertNull(rangeHeaders[0])
        assertEquals("bytes=300000-", rangeHeaders[1], "Second attempt must ask only for the rest")
    }

    @Test
    fun aPartialFileFromAnEarlierSessionIsResumedNotRestarted() {
        partFile.writeBytes(payload.copyOf(600_000))
        val url = serve()

        assertEquals(ResumableDownloader.Result.Completed, download(url))

        assertContentEquals(payload, partFile.readBytes())
        assertEquals(listOf<String?>("bytes=600000-"), rangeHeaders.toList())
    }

    @Test
    fun aServerIgnoringRangeRewritesTheFileInsteadOfCorruptingIt() {
        partFile.writeBytes(ByteArray(600_000) { 7 })
        val url = serve(honorRange = false)

        assertEquals(ResumableDownloader.Result.Completed, download(url))

        assertContentEquals(payload, partFile.readBytes())
    }

    @Test
    fun anAlreadyCompletePartFileFinishesWithoutDownloading() {
        partFile.writeBytes(payload)
        val url = serve()

        assertEquals(ResumableDownloader.Result.Completed, download(url))
        assertContentEquals(payload, partFile.readBytes())
    }

    @Test
    fun authErrorsAreReportedOnceWithoutRetrying() {
        val url = serve(status = 401)

        assertEquals(ResumableDownloader.Result.HttpError(401), download(url))
        assertEquals(1, rangeHeaders.size)
        assertFalse(partFile.exists() && partFile.length() > 0)
    }

    @Test
    fun retriesServerErrorsThenGivesUp() {
        val url = serve(status = 503)

        assertFailsWith<IOException> { download(url) }
        assertEquals(3, rangeHeaders.size)
    }
}
