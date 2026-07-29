package com.example.lock.crypto

/**
 * U19 / KTD-4: Ed25519 detached-signature verification over the RAW received
 * command bytes. Abstracted so the production path uses libsodium (works on
 * minSdk 26 where the platform Ed25519 Signature only exists from API 33) while
 * unit tests can inject a JVM verifier.
 */
interface SignatureVerifier {
    fun verify(payload: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}
