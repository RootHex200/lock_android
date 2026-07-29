package com.example.lock.data.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * U10 / KTD-3: the device's EC secp256r1 (SHA256withECDSA) key lives in the
 * Android Keystore. The private key is non-exportable; only the public key
 * leaves the device (registered at enrollment). Every device→server request is
 * signed with this key.
 */
class DeviceKeyStore(
    private val alias: String = KEY_ALIAS,
) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun hasKey(): Boolean = keyStore.containsAlias(alias)

    /** Generate the keypair if absent; returns the public key as PEM (SPKI). */
    fun ensureKeyPair(): String {
        if (!hasKey()) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE,
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            generator.generateKeyPair()
        }
        return publicKeyPem()
    }

    fun publicKeyPem(): String {
        val cert = keyStore.getCertificate(alias)
            ?: error("Device key not initialised")
        val encoded = (cert.publicKey as ECPublicKey).encoded
        val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(encoded)
        return "-----BEGIN PUBLIC KEY-----\n$body\n-----END PUBLIC KEY-----\n"
    }

    /** Sign the canonical request string; returns a DER ECDSA signature. */
    fun sign(canonical: ByteArray): ByteArray {
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(entry.privateKey)
            update(canonical)
            sign()
        }
    }

    fun signBase64(canonical: ByteArray): String =
        Base64.getEncoder().encodeToString(sign(canonical))

    fun deleteKey() {
        if (hasKey()) keyStore.deleteEntry(alias)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "device_financing_lock_device_key"
    }
}
