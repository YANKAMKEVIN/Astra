package com.kevin.astra.domain.modelmanager

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Streams a URL into [File] with resume and retry.
 *
 * Multi-hundred-MB model files used to restart from zero on any dropped connection (Wi-Fi
 * hand-off, a lock-screen network cut…). The partial file is kept and each attempt asks for
 * the remaining bytes with an HTTP `Range` header; a server that ignores it (200 instead of
 * 206) restarts the file cleanly.
 */
internal class ResumableDownloader(
    private val maxAttempts: Int = 4,
    private val backoffMillis: (attempt: Int) -> Long = { attempt -> 2_000L shl (attempt - 1) },
    private val openConnection: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection },
) {
    sealed interface Result {
        data object Completed : Result
        /** Non-retryable HTTP status (auth, not found…). */
        data class HttpError(val code: Int) : Result
    }

    /**
     * Downloads into [partFile], appending to whatever it already holds.
     * @throws IOException when every attempt failed on the network.
     */
    suspend fun download(
        url: String,
        authToken: String?,
        partFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result {
        var lastError: IOException? = null
        for (attempt in 1..maxAttempts) {
            currentCoroutineContext().ensureActive()
            try {
                return attemptOnce(url, authToken, partFile, onProgress)
            } catch (e: IOException) {
                lastError = e
                if (attempt < maxAttempts) delay(backoffMillis(attempt))
            }
        }
        throw lastError ?: IOException("Download failed")
    }

    private suspend fun attemptOnce(
        url: String,
        authToken: String?,
        partFile: File,
        onProgress: (Long, Long) -> Unit,
    ): Result {
        val alreadyHave = if (partFile.exists()) partFile.length() else 0L
        val connection = openConnection(url).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            if (authToken != null) setRequestProperty("Authorization", "Bearer $authToken")
            if (alreadyHave > 0) setRequestProperty("Range", "bytes=$alreadyHave-")
        }
        try {
            val code = connection.responseCode
            val append = when (code) {
                HttpURLConnection.HTTP_PARTIAL -> true
                HttpURLConnection.HTTP_OK -> false // Range ignored (or fresh start): rewrite the file.
                416 -> return Result.Completed // Nothing left to fetch: the part file is complete.
                in 500..599 -> throw IOException("Server returned HTTP $code")
                else -> return Result.HttpError(code)
            }
            val offset = if (append) alreadyHave else 0L
            val remaining = connection.contentLengthLong
            val total = if (remaining > 0) offset + remaining else -1L

            var downloaded = offset
            onProgress(downloaded, total)
            connection.inputStream.buffered(BUFFER_SIZE).use { input ->
                FileOutputStream(partFile, append).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            if (total > 0 && downloaded < total) {
                throw IOException("Connection closed early ($downloaded / $total bytes)")
            }
            return Result.Completed
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BUFFER_SIZE = 65_536
    }
}
