package com.kevin.astra.domain.gmail

import com.kevin.astra.core.crypto.Sha256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/** A PKCE verifier/challenge pair for the OAuth authorization-code flow (S256 method). */
data class PkcePair(
    val verifier: String,
    val challenge: String,
    val method: String = "S256",
)

object Pkce {
    /** Generates a PKCE pair. [randomBytes] is injectable so the derivation is testable. */
    @OptIn(ExperimentalEncodingApi::class)
    fun generate(randomBytes: ByteArray = Random.nextBytes(32)): PkcePair {
        val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        val verifier = b64.encode(randomBytes)
        val challenge = b64.encode(Sha256.hash(verifier.encodeToByteArray()))
        return PkcePair(verifier = verifier, challenge = challenge)
    }
}
