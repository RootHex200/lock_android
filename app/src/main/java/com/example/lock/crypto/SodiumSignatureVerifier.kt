package com.example.lock.crypto

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Sign

/**
 * U19 / KTD-4/KTD-7: production Ed25519 verifier backed by libsodium
 * (lazysodium-android). Byte-compatible with the server's PHP libsodium signer.
 */
class SodiumSignatureVerifier(
    private val sodium: LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid()),
) : SignatureVerifier {

    override fun verify(payload: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        if (signature.size != Sign.BYTES || publicKey.size != Sign.PUBLICKEYBYTES) return false
        return runCatching {
            sodium.cryptoSignVerifyDetached(signature, payload, payload.size, publicKey)
        }.getOrDefault(false)
    }
}
