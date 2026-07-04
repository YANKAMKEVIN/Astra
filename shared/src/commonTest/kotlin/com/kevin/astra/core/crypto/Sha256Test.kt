package com.kevin.astra.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256Test {
    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { ((it.toInt() and 0xff) + 0x100).toString(16).substring(1) }

    @Test
    fun matchesKnownVectors() {
        // FIPS 180-4 / RFC test vectors
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(Sha256.hash("".encodeToByteArray())),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(Sha256.hash("abc".encodeToByteArray())),
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex(Sha256.hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray())),
        )
    }

    @Test
    fun handlesBlockBoundaryLengths() {
        // 55/56/64 bytes exercise the padding edge cases.
        assertEquals(64, hex(Sha256.hash(ByteArray(55) { 'a'.code.toByte() })).length)
        assertEquals(64, hex(Sha256.hash(ByteArray(56) { 'a'.code.toByte() })).length)
        assertEquals(64, hex(Sha256.hash(ByteArray(64) { 'a'.code.toByte() })).length)
    }
}
