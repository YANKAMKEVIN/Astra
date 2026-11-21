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
    fun decodesLatin1QuotedPrintableBody() {
        // "Caf=E9" is 'é' (0xE9) in ISO-8859-1 — a single byte, invalid as standalone UTF-8.
        val eml = "Subject: Legacy\n" +
            "Content-Type: text/plain; charset=ISO-8859-1\n" +
            "Content-Transfer-Encoding: quoted-printable\n\n" +
            "Caf=E9 au lait"
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Café au lait")
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun decodesLatin1Base64Body() {
        val latin1 = byteArrayOf(0x43, 0x61, 0x66, 0xE9.toByte()) // "Café" in ISO-8859-1
        val encoded = Base64.encode(latin1)
        val eml = "Subject: LegacyB64\n" +
            "Content-Type: text/plain; charset=windows-1252\n" +
            "Content-Transfer-Encoding: base64\n\n" +
            encoded
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Café")
    }

    @Test
    fun decodesRawEightBitLatin1Body() {
        // No transfer-encoding: raw 8-bit body. 0xE9 = 'é' in ISO-8859-1.
        val headers = "Subject: Raw8bit\nContent-Type: text/plain; charset=ISO-8859-1\n\n"
        val bytes = headers.encodeToByteArray() + byteArrayOf(0x43, 0x61, 0x66, 0xE9.toByte()) // "Café"
        val result = extractor.extractEml(bytes, "mail.eml")
        assertContains(result.rawText, "Café")
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

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun decodesRfc2047Base64Subject() {
        val encoded = Base64.encode("Contrat confidentiel".encodeToByteArray())
        val eml = "Subject: =?UTF-8?B?$encoded?=\nFrom: a@b.com\n\nBody."
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Subject: Contrat confidentiel")
    }

    @Test
    fun decodesRfc2047QEncodedSubjectWithUnderscoreAsSpace() {
        // "Caf=C3=A9_r=C3=A9union" → "Café réunion" (underscore is a space in Q-encoding)
        val eml = "Subject: =?UTF-8?Q?Caf=C3=A9_r=C3=A9union?=\nFrom: a@b.com\n\nBody."
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Subject: Café réunion")
    }

    @Test
    fun concatenatesAdjacentEncodedWords() {
        val eml = "Subject: =?UTF-8?Q?Rapport_?= =?UTF-8?Q?trimestriel?=\n\nBody."
        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")
        assertContains(result.rawText, "Subject: Rapport trimestriel")
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

    // A mail with an attachment: multipart/mixed > multipart/alternative > text/plain + text/html,
    // then a PDF. The body used to be lost because only the first level was read.
    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun readsTheBodyOfANestedMultipartWithAnAttachment() {
        val pdf = Base64.encode("%PDF-1.4 fake".encodeToByteArray())
        val eml = """
            Subject: Facture
            Content-Type: multipart/mixed; boundary="MixedBoundary"

            This is a multi-part message in MIME format.
            --MixedBoundary
            Content-Type: multipart/alternative; boundary="AltBoundary"

            --AltBoundary
            Content-Type: text/plain; charset="UTF-8"
            Content-Transfer-Encoding: quoted-printable

            Votre facture de 42 =E2=82=AC est jointe.
            --AltBoundary
            Content-Type: text/html; charset="UTF-8"

            <p>Votre facture HTML</p>
            --AltBoundary--

            --MixedBoundary
            Content-Type: application/pdf; name="facture.pdf"
            Content-Disposition: attachment; filename="facture.pdf"
            Content-Transfer-Encoding: base64

            $pdf
            --MixedBoundary--
        """.trimIndent()

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertContains(result.rawText, "Votre facture de 42 € est jointe.")
        assertFalse(result.rawText.contains("HTML"))
        assertFalse(result.rawText.contains("PDF"))
        assertFalse(result.rawText.contains("MIME format"))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun fallsBackToConvertedHtmlDeepInTheTree() {
        val html = Base64.encode("<html><body><p>Bonjour&nbsp;Kevin</p><br>Rendez-vous lundi</body></html>".encodeToByteArray())
        val eml = """
            Subject: Invitation
            Content-Type: multipart/mixed; boundary=outer

            --outer
            Content-Type: multipart/related; boundary=related

            --related
            Content-Type: multipart/alternative; boundary=alt

            --alt
            Content-Type: text/html; charset=utf-8
            Content-Transfer-Encoding: base64

            $html
            --alt--
            --related
            Content-Type: image/png
            Content-Disposition: inline; filename="logo.png"

            iVBORw0KGgo=
            --related--
            --outer
            Content-Type: text/calendar; method=REQUEST

            BEGIN:VCALENDAR
            --outer--
        """.trimIndent()

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertContains(result.rawText, "Bonjour Kevin")
        assertContains(result.rawText, "Rendez-vous lundi")
        assertFalse(result.rawText.contains("<"))
        assertFalse(result.rawText.contains("VCALENDAR"))
    }

    @Test
    fun prefersPlainTextEvenWhenTheHtmlPartComesFirst() {
        val eml = """
            Subject: Order
            Content-Type: multipart/alternative; boundary="b1"

            --b1
            Content-Type: text/html

            <p>HTML first</p>
            --b1
            Content-Type: text/plain

            Plain second.
            --b1--
        """.trimIndent()

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertContains(result.rawText, "Plain second.")
        assertFalse(result.rawText.contains("HTML first"))
    }

    @Test
    fun keepsTheCaseOfTheBoundaryAndReadsCrlfMessages() {
        val eml = "Subject: Crlf\r\nContent-Type: multipart/mixed; boundary=\"AbC\"\r\n\r\n" +
            "--AbC\r\nContent-Type: multipart/alternative; boundary=\"DeF\"\r\n\r\n" +
            "--DeF\r\nContent-Type: text/plain\r\n\r\nCorps en CRLF.\r\n--DeF--\r\n" +
            "--AbC--\r\n"

        val result = extractor.extractEml(eml.encodeToByteArray(), "mail.eml")

        assertContains(result.rawText, "Corps en CRLF.")
    }
}
