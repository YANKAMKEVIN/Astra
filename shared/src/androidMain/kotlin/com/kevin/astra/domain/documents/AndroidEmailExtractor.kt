package com.kevin.astra.domain.documents

class AndroidEmailExtractor : EmailExtractor {

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

        val subject = headerMap["subject"] ?: ""
        val from = headerMap["from"] ?: ""
        val to = headerMap["to"] ?: ""
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

    private fun decodeBase64Body(encoded: String): String {
        return try {
            val cleaned = encoded.replace(Regex("\\s"), "")
            android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT).decodeToString()
        } catch (e: Exception) {
            encoded
        }
    }

    private fun decodeQuotedPrintable(encoded: String): String {
        val sb = StringBuilder()
        var i = 0
        val clean = encoded.replace("=\r\n", "").replace("=\n", "")
        while (i < clean.length) {
            if (clean[i] == '=' && i + 2 < clean.length) {
                try {
                    val hex = clean.substring(i + 1, i + 3)
                    sb.append(hex.toInt(16).toChar())
                    i += 3
                } catch (e: Exception) {
                    sb.append(clean[i++])
                }
            } else {
                sb.append(clean[i++])
            }
        }
        return sb.toString()
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

actual fun createEmailExtractor(): EmailExtractor = AndroidEmailExtractor()
