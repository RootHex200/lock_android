package com.example.lock.crypto

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.KeyFactory

/**
 * JVM Ed25519 signer/verifier for unit tests (JDK 15+). Mirrors the server's
 * libsodium detached signature so CommandVerifier can be tested with REAL
 * crypto without loading the Android native libsodium in a JVM test.
 */
object JvmEd25519 {

    class Keys(val privateEncoded: ByteArray, val publicRaw: ByteArray)

    /** Generate a keypair; returns the raw 32-byte public key + PKCS8 private. */
    fun generate(): Keys {
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        // Extract the raw 32-byte public key from the X.509 SPKI (last 32 bytes).
        val spki = kp.public.encoded
        val rawPublic = spki.copyOfRange(spki.size - 32, spki.size)
        return Keys(kp.private.encoded, rawPublic)
    }

    fun sign(privateEncoded: ByteArray, message: ByteArray): ByteArray {
        val key = KeyFactory.getInstance("Ed25519")
            .generatePrivate(PKCS8EncodedKeySpec(privateEncoded))
        return Signature.getInstance("Ed25519").run {
            initSign(key)
            update(message)
            sign()
        }
    }
}

/** A SignatureVerifier backed by the JVM Ed25519 provider (test only). */
class JvmEd25519Verifier : SignatureVerifier {
    override fun verify(payload: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        // Rebuild an X.509 SPKI from the raw 32-byte key for the JCA verifier.
        val spkiPrefix = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00,
        )
        val spki = spkiPrefix + publicKey
        val key = java.security.KeyFactory.getInstance("Ed25519")
            .generatePublic(java.security.spec.X509EncodedKeySpec(spki))
        return java.security.Signature.getInstance("Ed25519").run {
            initVerify(key)
            update(payload)
            runCatching { verify(signature) }.getOrDefault(false)
        }
    }
}
