package com.example.lock.sync

import com.example.lock.command.CommandApplier
import com.example.lock.command.CommandVerifier
import com.example.lock.command.ReceiptSender
import com.example.lock.data.local.DeviceCredentials
import com.example.lock.data.net.ApiService
import com.example.lock.state.DeviceState
import com.example.lock.state.StateManager
import java.time.Instant

/**
 * U12 / U19: poll commands/next, VERIFY over the raw received bytes, apply once,
 * and report a receipt for every outcome. Durable delivery — the server keeps
 * returning a pending command until a receipt confirms/fails it (KTD-6).
 */
class CommandPoller(
    private val api: ApiService,
    private val credentials: DeviceCredentials,
    private val verifier: CommandVerifier,
    private val applier: CommandApplier,
    private val receipts: ReceiptSender,
    private val stateManager: StateManager,
    private val clock: () -> Instant = { Instant.now() },
) {

    enum class Outcome { NO_COMMAND, APPLIED, DUPLICATE, EXPIRED, REJECTED, NOT_ENROLLED, ERROR }

    suspend fun pollOnce(): Outcome {
        val deviceId = credentials.deviceId() ?: return Outcome.NOT_ENROLLED
        val serverPub = credentials.serverSigningPublicKey()

        val response = runCatching { api.nextCommand(deviceId) }.getOrElse { return Outcome.ERROR }

        if (response.code() == 204) return Outcome.NO_COMMAND
        if (!response.isSuccessful) return Outcome.ERROR

        val rawBody = response.body()?.bytes() ?: return Outcome.ERROR
        val signature = response.headers()["X-Command-Signature"]

        val result = verifier.verify(
            rawBody = rawBody,
            signatureB64 = signature,
            serverPublicKeyB64 = serverPub,
            expectedDeviceId = deviceId,
            lastAppliedSequence = stateManager.lastAppliedSequence(),
            now = clock(),
        )
        val command = result.command

        return when (result.verdict) {
            CommandVerifier.Verdict.VALID -> {
                val applied = applier.apply(command!!)
                val reported = if (command.action == CommandApplier.ACTION_LOCK)
                    DeviceState.LOCKED else DeviceState.UNLOCKED
                receipts.send(deviceId, command.id, applied, reported)
                if (applied == CommandApplier.RESULT_SUCCESS) Outcome.APPLIED else Outcome.DUPLICATE
            }

            CommandVerifier.Verdict.DUPLICATE -> {
                receipts.send(deviceId, command!!.id, "DUPLICATE", stateManager.currentState())
                Outcome.DUPLICATE
            }

            CommandVerifier.Verdict.EXPIRED -> {
                command?.let { receipts.send(deviceId, it.id, "EXPIRED", stateManager.currentState()) }
                Outcome.EXPIRED
            }

            else -> {
                // REJECTED_SIGNATURE / DEVICE / OUT_OF_ORDER / MALFORMED
                command?.let {
                    receipts.send(deviceId, it.id, "REJECTED", stateManager.currentState(), errorCode = result.verdict.name)
                }
                Outcome.REJECTED
            }
        }
    }
}
