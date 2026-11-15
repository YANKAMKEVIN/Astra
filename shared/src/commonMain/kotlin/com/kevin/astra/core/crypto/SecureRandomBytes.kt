package com.kevin.astra.core.crypto

/**
 * [size] bytes from the platform's cryptographically secure generator, for secrets such as PKCE
 * verifiers. kotlin.random.Random is not suitable: it is not a CSPRNG on every platform.
 */
internal expect fun secureRandomBytes(size: Int): ByteArray
