package com.example.lock.data.net.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the device→server API. Field names match the FROZEN fixtures in
 * app/src/test/resources/api/ (copied verbatim from lock_admin). Do not rename
 * without a coordinated change to the paired lock_admin owner unit.
 */

// POST /api/v1/enrollments/{token}/activate (U8 owns fixture)
data class ActivateRequest(
    @SerializedName("public_key_pem") val publicKeyPem: String,
    @SerializedName("algorithm") val algorithm: String = "EC_secp256r1_SHA256",
    @SerializedName("app_version") val appVersion: String? = null,
    @SerializedName("fcm_token") val fcmToken: String? = null,
)

data class ActivateResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("credential") val credential: CredentialDto,
    @SerializedName("server_signing_public_key") val serverSigningPublicKey: String,
    @SerializedName("server_signing_public_key_algorithm") val serverSigningPublicKeyAlgorithm: String,
    @SerializedName("state") val state: StateDto,
    @SerializedName("settings") val settings: SettingsDto?,
    @SerializedName("poll_interval_seconds") val pollIntervalSeconds: Int,
    @SerializedName("api_version") val apiVersion: String,
)

data class CredentialDto(
    @SerializedName("credential_id") val credentialId: String,
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("issued_at") val issuedAt: String,
)

data class StateDto(
    @SerializedName("enrollment_status") val enrollmentStatus: String,
    @SerializedName("desired_state") val desiredState: String,
    @SerializedName("reported_state") val reportedState: String,
)

data class SettingsDto(
    @SerializedName("shop_name") val shopName: String?,
    @SerializedName("support_phone") val supportPhone: String?,
    @SerializedName("lock_screen_message") val lockScreenMessage: String?,
    @SerializedName("reconnect_instructions") val reconnectInstructions: String?,
)

// POST /devices/{id}/heartbeat (U9 owns fixture)
data class HeartbeatRequest(
    @SerializedName("reported_state") val reportedState: String?,
    @SerializedName("app_version") val appVersion: String?,
    @SerializedName("android_version") val androidVersion: String?,
    @SerializedName("last_sequence") val lastSequence: Long?,
    @SerializedName("health") val health: Map<String, Any?>? = null,
)

data class HeartbeatResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("desired_state") val desiredState: String,
    @SerializedName("reported_state") val reportedState: String,
    @SerializedName("offline") val offline: Boolean,
    @SerializedName("poll_interval_seconds") val pollIntervalSeconds: Int,
    @SerializedName("server_time") val serverTime: String,
)

// GET /devices/{id}/commands/next — body IS the exact signed command bytes;
// signature travels in the X-Command-Signature header (KTD-4).
data class SignedCommand(
    @SerializedName("id") val id: Long,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("sequence") val sequence: Long,
    @SerializedName("action") val action: String,
    @SerializedName("issued_at") val issuedAt: String,
    @SerializedName("expires_at") val expiresAt: String,
)

// POST /devices/{id}/commands/{command}/receipt (U15 owns fixture)
data class ReceiptRequest(
    @SerializedName("result") val result: String,
    @SerializedName("reported_state") val reportedState: String?,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("executed_at") val executedAt: String? = null,
)

data class ReceiptResponse(
    @SerializedName("receipt_id") val receiptId: Long,
    @SerializedName("command_status") val commandStatus: String,
    @SerializedName("reported_state") val reportedState: String,
    @SerializedName("desired_state") val desiredState: String,
    @SerializedName("accepted") val accepted: Boolean,
)
