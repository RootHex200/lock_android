package com.example.lock.data.net

import java.security.MessageDigest

/**
 * U10 / KTD-3: builds the exact canonical string the server verifies:
 *   METHOD "\n" "/"+path "\n" timestamp "\n" sha256hex(body)
 * Pure function (JVM-testable) so both sides stay byte-identical.
 */
object RequestSigner {

    fun canonicalString(method: String, path: String, timestamp: String, body: ByteArray): String {
        val normalisedPath = "/" + path.trimStart('/')
        return listOf(
            method.uppercase(),
            normalisedPath,
            timestamp,
            sha256Hex(body),
        ).joinToString("\n")
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
