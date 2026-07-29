package com.example.lock.state

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "device_state_prefs")

/**
 * U11 / U23: small, durable prefs for the last CONFIRMED lock state and the
 * last applied command sequence. Survives process death; connectivity changes
 * never touch it (only a confirmed command transition writes here).
 */
class StateStore(private val context: Context) : LockStateStore {

    override suspend fun confirmedState(): DeviceState {
        val name = context.dataStore.data.map { it[KEY_STATE] }.first()
        return DeviceState.fromOrNull(name) ?: DeviceState.UNLOCKED
    }

    override suspend fun lastAppliedSequence(): Long =
        context.dataStore.data.map { it[KEY_SEQ] ?: 0L }.first()

    override suspend fun setConfirmedState(state: DeviceState, sequence: Long) {
        context.dataStore.edit {
            it[KEY_STATE] = state.name
            it[KEY_SEQ] = sequence
        }
    }

    companion object {
        private val KEY_STATE = stringPreferencesKey("confirmed_state")
        private val KEY_SEQ = longPreferencesKey("last_applied_sequence")
    }
}
