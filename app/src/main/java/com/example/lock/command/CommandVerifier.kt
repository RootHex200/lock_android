package com.example.lock.command

import com.example.lock.crypto.SignatureVerifier
import com.example.lock.data.net.dto.SignedCommand
import com.google.gson.Gson
import java.time.Instant
import java.util.Base64

/**
 * U19 / AND-006: verify EVERY command before applying. Checks (in order):
 * signature over the RAW received bytes, device binding, expiry, and
 * monotonic sequence (replay / out-of-order). The parsed [SignedCommand] is
 * only trusted AFTER the signature passes over the exact received bytes (KTD-4).
 */
class CommandVerifier(
    private val verifier: SignatureVerifier,
    private val gson: Gson = Gson(),
) {

    enum class Verdict { VALID, DUPLICATE, EXPIRED, REJECTED_SIGNATURE, REJECTED_DEVICE, REJECTED_OUT_OF_ORDER, MALFORMED }

    data class Result(val verdict: Verdict, val command: SignedCommand?)

    fun verify(
        rawBody: ByteArray,
        signatureB64: String?,
        serverPublicKeyB64: String?,
        expectedDeviceId: String,
        lastAppliedSequence: Long,
        now: Instant = Instant.now(),
    ): Result {
        if (signatureB64.isNullOrBlank() || serverPublicKeyB64.isNullOrBlank()) {
            return Result(Verdict.REJECTED_SIGNATURE, null)
        }

        val signature = decodeOrNull(signatureB64)
        val publicKey = decodeOrNull(serverPublicKeyB64)
        if (signature == null || publicKey == null) {
            return Result(Verdict.REJECTED_SIGNATURE, null)
        }

        // 1) Signature over the EXACT received bytes — before any parsing trust.
        if (!verifier.verify(rawBody, signature, publicKey)) {
            return Result(Verdict.REJECTED_SIGNATURE, null)
        }

        val command = runCatching { gson.fromJson(String(rawBody), SignedCommand::class.java) }
            .getOrNull() ?: return Result(Verdict.MALFORMED, null)

        // 2) Device binding.
        if (command.deviceId != expectedDeviceId) {
            return Result(Verdict.REJECTED_DEVICE, command)
        }

        // 3) Expiry.
        val expiresAt = parseInstant(command.expiresAt) ?: return Result(Verdict.MALFORMED, command)
        if (!expiresAt.isAfter(now)) {
            return Result(Verdict.EXPIRED, command)
        }

        // 4) Sequence (idempotency + ordering).
        return when {
            command.sequence == lastAppliedSequence -> Result(Verdict.DUPLICATE, command)
            command.sequence < lastAppliedSequence -> Result(Verdict.REJECTED_OUT_OF_ORDER, command)
            else -> Result(Verdict.VALID, command)
        }
    }

    private fun decodeOrNull(b64: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(b64) }.getOrNull()

    private fun parseInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrNull()
}
