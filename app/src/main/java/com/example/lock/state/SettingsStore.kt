package com.example.lock.state

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "lock_screen_settings")

/**
 * U21 / AND-008: shop identity + lock-screen content, synced from the server
 * (U3) at enrollment and refreshed on heartbeat. The lock screen renders from
 * here so a locked, offline device still shows current shop contact info.
 */
class SettingsStore(private val context: Context) {

    data class LockScreenSettings(
        val shopName: String?,
        val supportPhone: String?,
        val lockMessage: String?,
        val reconnectInstructions: String?,
    )

    suspend fun save(shopName: String?, supportPhone: String?, lockMessage: String?, reconnect: String?) {
        context.settingsDataStore.edit {
            shopName?.let { v -> it[KEY_SHOP] = v }
            supportPhone?.let { v -> it[KEY_PHONE] = v }
            lockMessage?.let { v -> it[KEY_MSG] = v }
            reconnect?.let { v -> it[KEY_RECONNECT] = v }
        }
    }

    suspend fun get(): LockScreenSettings {
        val prefs = context.settingsDataStore.data.first()
        return LockScreenSettings(
            shopName = prefs[KEY_SHOP],
            supportPhone = prefs[KEY_PHONE],
            lockMessage = prefs[KEY_MSG],
            reconnectInstructions = prefs[KEY_RECONNECT],
        )
    }

    companion object {
        private val KEY_SHOP = stringPreferencesKey("shop_name")
        private val KEY_PHONE = stringPreferencesKey("support_phone")
        private val KEY_MSG = stringPreferencesKey("lock_message")
        private val KEY_RECONNECT = stringPreferencesKey("reconnect")
    }
}
