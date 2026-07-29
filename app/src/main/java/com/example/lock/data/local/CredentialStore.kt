package com.example.lock.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * U10 / KTD-3: stores the opaque per-device credential id and the PINNED server
 * Ed25519 signing public key (AND-006) in EncryptedSharedPreferences. The
 * device private key itself never leaves the Keystore; this only holds the
 * credential handle + the server public key.
 */
/** Read/write contract so the sync graph is unit-testable without Android. */
interface DeviceCredentials {
    fun deviceId(): String?
    fun credentialId(): String?
    fun serverSigningPublicKey(): String?
    fun isEnrolled(): Boolean
    fun save(deviceId: String, credentialId: String, serverSigningPublicKeyB64: String)
}

class CredentialStore(context: Context) : DeviceCredentials {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun save(deviceId: String, credentialId: String, serverSigningPublicKeyB64: String) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_CREDENTIAL_ID, credentialId)
            .putString(KEY_SERVER_PUBKEY, serverSigningPublicKeyB64)
            .apply()
    }

    override fun deviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    override fun credentialId(): String? = prefs.getString(KEY_CREDENTIAL_ID, null)

    /** The pinned server Ed25519 public key used to verify commands (AND-006). */
    override fun serverSigningPublicKey(): String? = prefs.getString(KEY_SERVER_PUBKEY, null)

    override fun isEnrolled(): Boolean = credentialId() != null

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val FILE_NAME = "device_credentials"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CREDENTIAL_ID = "credential_id"
        private const val KEY_SERVER_PUBKEY = "server_signing_public_key"
    }
}
