package com.example.lock.state

/**
 * U11 / AND-004/011, U23 / AND-010: single source of truth for the device's
 * confirmed lock state.
 *
 * Invariants:
 *  - UNLOCKED = fully normal phone (no restriction UI, no launcher replacement).
 *  - Connectivity changes NEVER mutate state.
 *  - Only a verified+confirmed command transition advances the state.
 *  - Last confirmed state is restored on cold start / reboot (via StateStore).
 */
/** Storage contract so StateManager is unit-testable without Android DataStore. */
interface LockStateStore {
    suspend fun confirmedState(): DeviceState
    suspend fun lastAppliedSequence(): Long
    suspend fun setConfirmedState(state: DeviceState, sequence: Long)
}

class StateManager(private val store: LockStateStore) {

    suspend fun currentState(): DeviceState = store.confirmedState()

    suspend fun lastAppliedSequence(): Long = store.lastAppliedSequence()

    suspend fun isLocked(): Boolean = currentState() == DeviceState.LOCKED

    /** Persist a confirmed transition (only called after successful apply). */
    suspend fun confirm(state: DeviceState, sequence: Long) {
        store.setConfirmedState(state, sequence)
    }

    /**
     * Connectivity change hook — intentionally a no-op on state. Exists so the
     * contract is explicit and testable: losing/gaining network must not change
     * the persisted lock state (AND-011).
     */
    suspend fun onConnectivityChanged(@Suppress("UNUSED_PARAMETER") connected: Boolean): DeviceState =
        currentState()
}
