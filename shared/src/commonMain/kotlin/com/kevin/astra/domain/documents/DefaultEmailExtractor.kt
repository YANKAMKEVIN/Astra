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
        val text = bytes.decodeToString()
        val parsed = parseEmlMessage(text)
        return LoadedEmailDocument(
            fileName = fileName,
            emailCount = 1,
            rawText = parsed,
        )
    }

    override fun extractMbox(bytes: ByteArray, fileName: String): LoadedEmailDocument {
        val text = bytes.decodeToString()
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

        // Unfold headers (continuation lines start with whitespace)
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

        val headerMap = headers.mapNotNull { line ->
            val colon = line.indexOf(':')
            if (colon > 0) line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
            else null
        }.toMap()

        val subject = decodeEncodedWords(headerMap["subject"] ?: "")
        val from = decodeEncodedWords(headerMap["from"] ?: "")
        val to = decodeEncodedWords(headerMap["to"] ?: "")
        val date = headerMap["date"] ?: ""

        val contentType = headerMap["content-type"] ?: "text/plain"
        val boundary = Regex("boundary=\"?([^\"\\s;]+)\"?", RegexOption.IGNORE_CASE)
            .find(contentType)?.groupValues?.get(1)

        val body = when {
            boundary != null -> extractMultipartBody(bodyLines.joinToString("\n"), boundary)
            contentType.contains("base64", ignoreCase = true) ||
                headerMap["content-transfer-encoding"]?.contains("base64", ignoreCase = true) == true ->
                decodeBase64Body(bodyLines.joinToString("\n"))
            contentType.contains("quoted-printable", ignoreCase = true) ||
                headerMap["content-transfer-encoding"]?.contains("quoted-printable", ignoreCase = true) == true ->
                decodeQuotedPrintable(bodyLines.joinToString("\n"))
            else -> bodyLines.joinToString("\n").trim()
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

    private fun extractMultipartBody(body: String, boundary: String): String {
        val parts = body.split("--$boundary")
        return parts.mapNotNull { part ->
            val blankIdx = part.indexOf("\n\n").takeIf { it >= 0 }
                ?: part.indexOf("\r\n\r\n").takeIf { it >= 0 }
                ?: return@mapNotNull null
            val partHeaders = part.substring(0, blankIdx).lowercase()
            val partBody = part.substring(blankIdx).trim()

            when {
                // Prefer text/plain parts
                partHeaders.contains("text/plain") -> {
                    val decoded = when {
                        partHeaders.contains("base64") -> decodeBase64Body(partBody)
                        partHeaders.contains("quoted-printable") -> decodeQuotedPrintable(partBody)
                        else -> partBody
                    }
                    decoded.trim()
                }
                // Fall back to text/html if no plain part found
                partHeaders.contains("text/html") -> {
                    val decoded = when {
                        partHeaders.contains("base64") -> decodeBase64Body(partBody)
                        partHeaders.contains("quoted-printable") -> decodeQuotedPrintable(partBody)
                        else -> partBody
                    }
                    stripHtml(decoded).trim()
                }
                else -> null
            }
        }.firstOrNull { it.isNotBlank() } ?: ""
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64Body(encoded: String): String {
        return try {
            // Base64.Mime tolerates the line breaks/whitespace common in email bodies.
            Base64.Mime.decode(encoded).decodeToString()
        } catch (e: Exception) {
            encoded
        }
    }

    private fun decodeQuotedPrintable(encoded: String): String {
        // Collect raw bytes first, then decode as UTF-8 so multi-byte sequences like
        // "=C3=A9" (é) are reassembled correctly instead of turning into mojibake.
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
        return bytes.toByteArray().decodeToString()
    }

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
}
