package com.kevin.astra.domain.documents

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Platform-agnostic RFC 2822 email parser. All logic is pure Kotlin (regex + string
 * manipulation) plus the multiplatform [Base64], so a single implementation serves both
 * Android and iOS via their `createEmailExtractor()` actuals.
 */
class DefaultEmailExtractor : EmailExtractor {

    override fun extractEml(bytes: ByteArray, fileName: String): LoadedEmailDocument {
        // Decode byte-for-byte (Latin-1) so the charset can be applied later; decoding the whole
        // message as UTF-8 upfront would corrupt legacy 8-bit bodies before we know their charset.
        val text = latin1Decode(bytes)
        val parsed = parseEmlMessage(text)
        return LoadedEmailDocument(
            fileName = fileName,
            emailCount = 1,
            rawText = parsed,
        )
    }

    override fun extractMbox(bytes: ByteArray, fileName: String): LoadedEmailDocument {
        val text = latin1Decode(bytes)
        // mbox messages are separated by lines starting with "From "
        val messages = text.split(Regex("(?m)^From .+$"))
            .filter { it.isNotBlank() }
        val parsed = messages.joinToString("\n\n---\n\n") { parseEmlMessage(it) }
        return LoadedEmailDocument(
            fileName = fileName,
            emailCount = messages.size.coerceAtLeast(1),
            rawText = parsed,
        )
    }

    // Parses a raw RFC 2822 message and returns a readable text block.
    private fun parseEmlMessage(raw: String): String {
        val lines = raw.lines()

        // Split at the first blank line separating headers from body
        val blankIndex = lines.indexOfFirst { it.isBlank() }
        val headerLines = if (blankIndex >= 0) lines.subList(0, blankIndex) else lines
        val bodyLines = if (blankIndex >= 0 && blankIndex + 1 < lines.size)
            lines.subList(blankIndex + 1, lines.size) else emptyList()

        val headerMap = parseHeaders(headerLines)

        val subject = decodeEncodedWords(headerMap["subject"] ?: "")
        val from = decodeEncodedWords(headerMap["from"] ?: "")
        val to = decodeEncodedWords(headerMap["to"] ?: "")
        val date = headerMap["date"] ?: ""

        val contentType = headerMap["content-type"] ?: "text/plain"
        val boundary = Regex("boundary=\"?([^\"\\s;]+)\"?", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1)
        val charset = charsetOf(contentType)

        val body = when {
            boundary != null -> extractMultipartBody(bodyLines.joinToString("\n"), boundary, depth = 0).best()
            contentType.contains("base64", ignoreCase = true) ||
                headerMap["content-transfer-encoding"]?.contains("base64", ignoreCase = true) == true ->
                decodeBase64Body(bodyLines.joinToString("\n"), charset)
            contentType.contains("quoted-printable", ignoreCase = true) ||
                headerMap["content-transfer-encoding"]?.contains("quoted-printable", ignoreCase = true) == true ->
                decodeQuotedPrintable(bodyLines.joinToString("\n"), charset)
            // Unencoded (possibly 8-bit) body: re-decode the raw bytes with the declared charset.
            else -> decodeBytes(latin1Encode(bodyLines.joinToString("\n")), charset).trim()
        }

        val strippedBody = stripHtml(body).trim()

        return buildString {
            if (subject.isNotBlank()) appendLine("Subject: $subject")
            if (from.isNotBlank()) appendLine("From: $from")
            if (to.isNotBlank()) appendLine("To: $to")
            if (date.isNotBlank()) appendLine("Date: $date")
            if (strippedBody.isNotBlank()) {
                appendLine()
                append(strippedBody)
            }
        }.trim()
    }

    // Unfolds header lines (continuations start with whitespace) into a lowercase-keyed map.
    private fun parseHeaders(headerLines: List<String>): Map<String, String> {
        val headers = mutableListOf<String>()
        val currentHeader = StringBuilder()
        for (line in headerLines) {
            if (line.isNotEmpty() && (line[0] == ' ' || line[0] == '\t')) {
                currentHeader.append(' ').append(line.trim())
            } else {
                if (currentHeader.isNotBlank()) headers.add(currentHeader.toString())
                currentHeader.clear()
                currentHeader.append(line)
            }
        }
        if (currentHeader.isNotBlank()) headers.add(currentHeader.toString())
        return headers.mapNotNull { line ->
            val colon = line.indexOf(':')
            if (colon > 0) line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
            else null
        }.toMap()
    }

    // The boundary keeps its original case: lowercasing it would no longer match the delimiters.
    private fun boundaryOf(contentType: String): String? =
        Regex("boundary=\"?([^\"\\s;]+)\"?", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1)

    /** Readable text found in a MIME tree: the first text/plain part and the first text/html part. */
    private class BodyText(val plain: String?, val html: String?) {
        // Plain text wins; HTML (already converted to text) is the fallback.
        fun best(): String = plain?.takeIf { it.isNotBlank() } ?: html.orEmpty()
    }

    /**
     * Walks a multipart body recursively: `multipart/mixed` (a mail with an attachment) usually
     * wraps a `multipart/alternative` that holds the text/plain and text/html versions, sometimes
     * itself inside a `multipart/related`. Attachments are skipped.
     */
    private fun extractMultipartBody(body: String, boundary: String, depth: Int): BodyText {
        var plain: String? = null
        var html: String? = null
        val parts = body.split("--$boundary").drop(1) // drop the preamble
        for (rawPart in parts) {
            if (rawPart.startsWith("--")) break // closing delimiter: the rest is the epilogue
            val part = rawPart.removePrefix("\r").removePrefix("\n")
            val lines = part.lines()
            val blank = lines.indexOfFirst { it.isBlank() }
            val headers = if (blank >= 0) parseHeaders(lines.subList(0, blank)) else parseHeaders(lines)
            val partBody = if (blank >= 0) lines.subList(blank + 1, lines.size).joinToString("\n") else ""
            val found = extractPart(headers, partBody, depth + 1)
            if (plain == null && !found.plain.isNullOrBlank()) plain = found.plain
            if (html == null && !found.html.isNullOrBlank()) html = found.html
        }
        return BodyText(plain, html)
    }

    private fun extractPart(headers: Map<String, String>, body: String, depth: Int): BodyText {
        // A part without Content-Type is text/plain (RFC 2046 §5.1).
        val contentType = headers["content-type"] ?: "text/plain"
        val type = contentType.substringBefore(';').trim().lowercase()
        val disposition = headers["content-disposition"].orEmpty().lowercase()
        if (disposition.startsWith("attachment")) return BodyText(null, null)
        if (type.startsWith("multipart/")) {
            val boundary = boundaryOf(contentType)
            return if (boundary != null && depth < MAX_MIME_DEPTH) {
                extractMultipartBody(body, boundary, depth)
            } else {
                BodyText(null, null)
            }
        }
        if (type != "text/plain" && type != "text/html") return BodyText(null, null)
        val charset = charsetOf(contentType)
        val encoding = headers["content-transfer-encoding"].orEmpty().lowercase()
        val decoded = when {
            encoding.contains("base64") -> decodeBase64Body(body, charset)
            encoding.contains("quoted-printable") -> decodeQuotedPrintable(body, charset)
            else -> decodeBytes(latin1Encode(body), charset)
        }.trim()
        return if (type == "text/plain") BodyText(decoded, null) else BodyText(null, stripHtml(decoded).trim())
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64Body(encoded: String, charset: String = "utf-8"): String {
        return try {
            // Base64.Mime tolerates the line breaks/whitespace common in email bodies.
            decodeBytes(Base64.Mime.decode(encoded), charset)
        } catch (e: Exception) {
            encoded
        }
    }

    private fun decodeQuotedPrintable(encoded: String, charset: String = "utf-8"): String {
        // Collect raw bytes first, then decode with the declared charset — so multi-byte UTF-8
        // sequences ("=C3=A9") AND single-byte Latin-1 bytes ("=E9") both render correctly.
        val bytes = ArrayList<Byte>(encoded.length)
        var i = 0
        val clean = encoded.replace("=\r\n", "").replace("=\n", "")
        while (i < clean.length) {
            val c = clean[i]
            if (c == '=' && i + 2 < clean.length) {
                val code = clean.substring(i + 1, i + 3).toIntOrNull(16)
                if (code != null) {
                    bytes.add(code.toByte())
                    i += 3
                } else {
                    bytes.add(c.code.toByte())
                    i++
                }
            } else {
                for (b in c.toString().encodeToByteArray()) bytes.add(b)
                i++
            }
        }
        return decodeBytes(bytes.toByteArray(), charset)
    }

    private fun charsetOf(headers: String): String =
        Regex("charset=\"?([^\";\\s]+)\"?", RegexOption.IGNORE_CASE)
            .find(headers)?.groupValues?.get(1) ?: "utf-8"

    // Byte-preserving decode/encode: every byte 0..255 maps to the code point of the same value,
    // so the original bytes survive until the real charset is applied to the body.
    private fun latin1Decode(bytes: ByteArray): String =
        buildString(bytes.size) { for (b in bytes) append((b.toInt() and 0xFF).toChar()) }

    private fun latin1Encode(text: String): ByteArray =
        ByteArray(text.length) { (text[it].code and 0xFF).toByte() }

    /**
     * Decodes RFC 2047 "encoded-words" found in headers, e.g. `=?UTF-8?B?Q29udHJhdA==?=`
     * or `=?UTF-8?Q?Caf=C3=A9?=`, so non-ASCII subjects/senders render as real text instead
     * of raw tokens. Adjacent encoded-words separated only by whitespace are concatenated.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeEncodedWords(text: String): String {
        if (!text.contains("=?")) return text
        // Per RFC 2047, whitespace between two encoded-words is not significant.
        val joined = text.replace(Regex("\\?=\\s+=\\?"), "?==?")
        val regex = Regex("=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=")
        return regex.replace(joined) { match ->
            val charset = match.groupValues[1]
            val encoding = match.groupValues[2].uppercase()
            val data = match.groupValues[3]
            try {
                val bytes = when (encoding) {
                    "B" -> Base64.Mime.decode(data)
                    "Q" -> decodeQEncoding(data)
                    else -> return@replace match.value
                }
                decodeBytes(bytes, charset)
            } catch (e: Exception) {
                match.value
            }
        }
    }

    // RFC 2047 "Q" encoding: like quoted-printable but '_' stands for a space.
    private fun decodeQEncoding(s: String): ByteArray {
        val out = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '_' -> { out.add(0x20); i++ }
                c == '=' && i + 2 < s.length -> {
                    val code = s.substring(i + 1, i + 3).toIntOrNull(16)
                    if (code != null) { out.add(code.toByte()); i += 3 }
                    else { out.add(c.code.toByte()); i++ }
                }
                else -> { for (b in c.toString().encodeToByteArray()) out.add(b); i++ }
            }
        }
        return out.toByteArray()
    }

    private fun decodeBytes(bytes: ByteArray, charset: String): String {
        val cs = charset.lowercase()
        return if (cs.contains("8859-1") || cs.contains("latin1") || cs.contains("windows-1252")) {
            // Single-byte Latin charsets map each byte directly to a code point.
            buildString(bytes.size) { for (b in bytes) append((b.toInt() and 0xFF).toChar()) }
        } else {
            bytes.decodeToString() // UTF-8 (and ASCII) — the common case
        }
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    private companion object {
        // Real mails nest 2-3 levels; the cap only guards against malformed or hostile input.
        const val MAX_MIME_DEPTH = 8
    }
}
