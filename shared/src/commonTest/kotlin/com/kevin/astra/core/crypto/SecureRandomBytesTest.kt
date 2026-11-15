package com.kevin.astra.core.crypto

import com.kevin.astra.domain.gmail.Pkce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class SecureRandomBytesTest {
    @Test
    fun returnsTheRequestedNumberOfBytes() {
        assertEquals(32, secureRandomBytes(32).size)
        assertEquals(0, secureRandomBytes(0).size)
    }

    @Test
    fun successiveDrawsDiffer() {
        val a = secureRandomBytes(32)
        val b = secureRandomBytes(32)
        assertFalse(a.contentEquals(b))
        assertFalse(a.all { it == 0.toByte() })
    }

    @Test
    fun defaultPkceVerifierIs43UrlSafeCharactersAndUnique() {
        val first = Pkce.generate()
        val second = Pkce.generate()
        // 32 bytes, base64url without padding: 43 characters (RFC 7636 allows 43..128).
        assertEquals(43, first.verifier.length)
        assertEquals(true, first.verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotEquals(first.verifier, second.verifier)
    }
}
