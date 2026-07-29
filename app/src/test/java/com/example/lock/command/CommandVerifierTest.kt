package com.example.lock.command

import com.example.lock.crypto.JvmEd25519
import com.example.lock.crypto.JvmEd25519Verifier
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Base64

/**
 * U19 / AND-006: every command is verified over the RAW received bytes before
 * it is trusted. Uses real JVM Ed25519 to mirror the server's libsodium signer.
 */
class CommandVerifierTest {

    private lateinit var keys: JvmEd25519.Keys
    private lateinit var pubB64: String
    private val verifier = CommandVerifier(JvmEd25519Verifier())
    private val deviceId = "11111111-1111-4111-8111-111111111111"

    @Before
    fun setUp() {
        keys = JvmEd25519.generate()
        pubB64 = Base64.getEncoder().encodeToString(keys.publicRaw)
    }

    /** Canonical command bytes — SAME key order the server signs (KTD-4). */
    private fun payload(
        id: Long = 123,
        device: String = deviceId,
        sequence: Long = 5,
        action: String = "LOCK",
        issuedAt: String = "2026-07-29T12:00:00+00:00",
        expiresAt: String = "2999-01-01T00:00:00+00:00",
    ): ByteArray = (
        """{"id":$id,"device_id":"$device","sequence":$sequence,""" +
            """"action":"$action","issued_at":"$issuedAt","expires_at":"$expiresAt"}"""
        ).toByteArray()

    private fun sign(body: ByteArray): String =
        Base64.getEncoder().encodeToString(JvmEd25519.sign(keys.privateEncoded, body))

    @Test
    fun `valid command verifies and parses`() {
        val body = payload(sequence = 5)
        val result = verifier.verify(body, sign(body), pubB64, deviceId, lastAppliedSequence = 4)

        assertEquals(CommandVerifier.Verdict.VALID, result.verdict)
        assertEquals(5L, result.command?.sequence)
        assertEquals("LOCK", result.command?.action)
    }

    @Test
    fun `tampered payload fails signature`() {
        val body = payload(action = "LOCK")
        val signature = sign(body)
        val tampered = payload(action = "UNLOCK") // different bytes, same signature

        val result = verifier.verify(tampered, signature, pubB64, deviceId, lastAppliedSequence = 0)
        assertEquals(CommandVerifier.Verdict.REJECTED_SIGNATURE, result.verdict)
    }

    @Test
    fun `wrong device is rejected`() {
        val body = payload(device = "99999999-9999-4999-8999-999999999999")
        val result = verifier.verify(body, sign(body), pubB64, deviceId, lastAppliedSequence = 0)
        assertEquals(CommandVerifier.Verdict.REJECTED_DEVICE, result.verdict)
    }

    @Test
    fun `expired command is rejected`() {
        val body = payload(sequence = 5, expiresAt = "2000-01-01T00:00:00+00:00")
        val result = verifier.verify(
            body, sign(body), pubB64, deviceId,
            lastAppliedSequence = 0, now = Instant.parse("2026-07-29T12:00:10Z"),
        )
        assertEquals(CommandVerifier.Verdict.EXPIRED, result.verdict)
    }

    @Test
    fun `duplicate sequence yields duplicate verdict`() {
        val body = payload(sequence = 5)
        val result = verifier.verify(body, sign(body), pubB64, deviceId, lastAppliedSequence = 5)
        assertEquals(CommandVerifier.Verdict.DUPLICATE, result.verdict)
    }

    @Test
    fun `out of order lower sequence is rejected`() {
        val body = payload(sequence = 3)
        val result = verifier.verify(body, sign(body), pubB64, deviceId, lastAppliedSequence = 5)
        assertEquals(CommandVerifier.Verdict.REJECTED_OUT_OF_ORDER, result.verdict)
    }

    @Test
    fun `missing signature is rejected`() {
        val body = payload()
        val result = verifier.verify(body, null, pubB64, deviceId, lastAppliedSequence = 0)
        assertEquals(CommandVerifier.Verdict.REJECTED_SIGNATURE, result.verdict)
    }
}
