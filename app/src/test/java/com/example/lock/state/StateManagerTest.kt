package com.example.lock.state

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** In-memory store so StateManager logic is tested without Android DataStore. */
private class FakeStore(
    var state: DeviceState = DeviceState.UNLOCKED,
    var seq: Long = 0,
) : LockStateStore {
    override suspend fun confirmedState() = state
    override suspend fun lastAppliedSequence() = seq
    override suspend fun setConfirmedState(state: DeviceState, sequence: Long) {
        this.state = state; this.seq = sequence
    }
}

/**
 * U11 / AND-004/011: state invariants.
 */
class StateManagerTest {

    @Test
    fun `defaults to unlocked`() = runTest {
        val manager = StateManager(FakeStore())
        assertEquals(DeviceState.UNLOCKED, manager.currentState())
        assertEquals(false, manager.isLocked())
    }

    @Test
    fun `confirm persists state and sequence`() = runTest {
        val store = FakeStore()
        val manager = StateManager(store)
        manager.confirm(DeviceState.LOCKED, 7)

        assertEquals(DeviceState.LOCKED, manager.currentState())
        assertEquals(7L, manager.lastAppliedSequence())
        assertEquals(true, manager.isLocked())
    }

    @Test
    fun `connectivity change never mutates state`() = runTest {
        val store = FakeStore(state = DeviceState.LOCKED, seq = 3)
        val manager = StateManager(store)

        manager.onConnectivityChanged(connected = false)
        manager.onConnectivityChanged(connected = true)

        assertEquals(DeviceState.LOCKED, manager.currentState())
        assertEquals(3L, manager.lastAppliedSequence())
    }

    @Test
    fun `last confirmed state is restored across manager instances`() = runTest {
        val store = FakeStore()
        StateManager(store).confirm(DeviceState.LOCKED, 5)

        // Simulate cold start: a new manager over the same (persisted) store.
        val restored = StateManager(store)
        assertEquals(DeviceState.LOCKED, restored.currentState())
    }
}
