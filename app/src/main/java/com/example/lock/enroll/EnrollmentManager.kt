package com.example.lock.enroll

import com.example.lock.data.keystore.DeviceKeyStore
import com.example.lock.data.local.DeviceCredentials
import com.example.lock.data.net.ApiService
import com.example.lock.data.net.dto.ActivateRequest
import com.example.lock.state.DeviceState
import com.example.lock.state.SettingsStore
import com.example.lock.state.StateManager

/**
 * U10 / AND-003/014: enrol from a single-use token. Generates the Keystore EC
 * keypair, registers the public key via activate, pins the server Ed25519
 * signing key, persists the credential + initial UNLOCKED state. Only a valid
 * single-use token proceeds; a used/expired token is refused by the server.
 */
class EnrollmentManager(
    private val api: ApiService,
    private val keyStore: DeviceKeyStore,
    private val credentials: DeviceCredentials,
    private val stateManager: StateManager,
    private val appVersion: String,
    private val settingsStore: SettingsStore? = null,
) {

    sealed interface Result {
        data class Success(val deviceId: String) : Result
        data class Failure(val reason: String) : Result
    }

    suspend fun enroll(token: String, fcmToken: String? = null): Result {
        if (credentials.isEnrolled()) {
            return Result.Failure("already_enrolled")
        }

        val publicKeyPem = keyStore.ensureKeyPair()

        val response = runCatching {
            api.activate(
                token,
                ActivateRequest(
                    publicKeyPem = publicKeyPem,
                    appVersion = appVersion,
                    fcmToken = fcmToken,
                ),
            )
        }.getOrElse { return Result.Failure("network_error:${it.message}") }

        if (!response.isSuccessful) {
            // Invalid/expired/used token, etc. Roll back the generated key so a
            // retry starts clean.
            keyStore.deleteKey()
            return Result.Failure("activation_rejected:${response.code()}")
        }

        val body = response.body() ?: return Result.Failure("empty_activation_body")

        credentials.save(
            deviceId = body.deviceId,
            credentialId = body.credential.credentialId,
            serverSigningPublicKeyB64 = body.serverSigningPublicKey,
        )

        // Cache lock-screen settings for offline display (U21).
        body.settings?.let { s ->
            settingsStore?.save(s.shopName, s.supportPhone, s.lockScreenMessage, s.reconnectInstructions)
        }

        // Initial state from the server (UNLOCKED on a fresh activation).
        val state = DeviceState.fromOrNull(body.state.reportedState) ?: DeviceState.UNLOCKED
        stateManager.confirm(state, sequence = 0)

        return Result.Success(body.deviceId)
    }
}
