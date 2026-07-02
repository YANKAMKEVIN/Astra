package com.kevin.astra.domain.documents

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultEmailExtractorTest {
    private val extractor = DefaultEmailExtractor()

    @Test
    fun extractsHeadersAndPlainBody() {
        val eml = """
            Subject: Pump maintenance
            From: alice@example.com
            To: bob@example.com
            Date: Mon, 1 Jan 2026 10:00:00 +0000

            Please restart pump A before noon.
        """.trimIndent()

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertEquals(1, result.emailCount)
        assertContains(result.rawText, "Subject: Pump maintenance")
        assertContains(result.rawText, "From: alice@example.com")
        assertContains(result.rawText, "Please restart pump A before noon.")
    }

    @Test
    fun unfoldsMultiLineHeaders() {
        val eml = "Subject: A very long subject that\n continues on the next line\nFrom: a@b.com\n\nBody."
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Subject: A very long subject that continues on the next line")
    }

    @Test
    fun decodesQuotedPrintableBody() {
        val eml = "Subject: QP\nContent-Transfer-Encoding: quoted-printable\n\nCaf=C3=A9 =3D power"
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Café = power")
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun decodesBase64Body() {
        val payload = "Confidential contract clause 7."
        val encoded = Base64.encode(payload.encodeToByteArray())
        val eml = "Subject: B64\nContent-Transfer-Encoding: base64\n\n$encoded"

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertContains(result.rawText, payload)
    }

    @Test
    fun prefersPlainTextPartInMultipart() {
        val eml = """
            Subject: Multipart
            Content-Type: multipart/alternative; boundary="XYZ"

            --XYZ
            Content-Type: text/plain

            Plain version wins.
            --XYZ
            Content-Type: text/html

            <html><body><p>HTML version</p></body></html>
            --XYZ--
        """.trimIndent()

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertContains(result.rawText, "Plain version wins.")
        assertFalse(result.rawText.contains("<html>"))
    }

    @Test
    fun stripsHtmlWhenOnlyHtmlPartPresent() {
        val eml = "Subject: HTML\nContent-Type: text/html\n\n<html><body><p>Hello</p><br>World</body></html>"
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Hello")
        assertContains(result.rawText, "World")
        assertFalse(result.rawText.contains("<"))
    }

    @Test
    fun splitsMboxIntoMultipleMessages() {
        val mbox = """
            From alice@example.com Mon Jan 01 10:00:00 2026
            Subject: First
            From: alice@example.com

            First body.
            From bob@example.com Mon Jan 01 11:00:00 2026
            Subject: Second
            From: bob@example.com

            Second body.
        """.trimIndent()

        val result = extractor.extractMbox(mbox.encodeToByteArray(), "inbox.mbox")

        assertEquals(2, result.emailCount)
        assertContains(result.rawText, "First body.")
        assertContains(result.rawText, "Second body.")
        assertTrue(result.rawText.contains("---")) // messages joined by separator
    }
}
