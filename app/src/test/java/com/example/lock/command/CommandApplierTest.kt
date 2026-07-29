package com.example.lock.command

import com.example.lock.data.local.CommandDao
import com.example.lock.data.local.CommandLogEntity
import com.example.lock.data.net.dto.SignedCommand
import com.example.lock.lock.LockActions
import com.example.lock.state.DeviceState
import com.example.lock.state.LockStateStore
import com.example.lock.state.StateManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeLockActions : LockActions {
    var entered = 0
    var exited = 0
    override fun enterLock() { entered++ }
    override fun exitLock() { exited++ }
}

private class FakeCommandDao : CommandDao {
    val log = mutableMapOf<Long, CommandLogEntity>()
    override suspend fun insert(entry: CommandLogEntity): Long {
        return if (log.containsKey(entry.commandId)) -1 else { log[entry.commandId] = entry; entry.commandId }
    }
    override suspend fun find(commandId: Long) = log[commandId]
    override suspend fun maxSequence() = log.values.maxOfOrNull { it.sequence }
    override suspend fun recent() = log.values.toList()
}

private class FakeStore : LockStateStore {
    var state = DeviceState.UNLOCKED
    var seq = 0L
    override suspend fun confirmedState() = state
    override suspend fun lastAppliedSequence() = seq
    override suspend fun setConfirmedState(state: DeviceState, sequence: Long) { this.state = state; this.seq = sequence }
}

/**
 * U19 / U20 / U22: apply-once semantics and lock/unlock side effects.
 */
class CommandApplierTest {

    private fun command(id: Long, action: String, seq: Long) =
        SignedCommand(id, "dev", seq, action, "2026-07-29T12:00:00+00:00", "2999-01-01T00:00:00+00:00")

    @Test
    fun `lock applies once and confirms LOCKED`() = runTest {
        val lock = FakeLockActions()
        val store = FakeStore()
        val applier = CommandApplier(lock, StateManager(store), FakeCommandDao())

        val result = applier.apply(command(1, "LOCK", 5))

        assertEquals(CommandApplier.RESULT_SUCCESS, result)
        assertEquals(1, lock.entered)
        assertEquals(DeviceState.LOCKED, store.state)
        assertEquals(5L, store.seq)
    }

    @Test
    fun `re-applying the same command is idempotent`() = runTest {
        val lock = FakeLockActions()
        val dao = FakeCommandDao()
        val applier = CommandApplier(lock, StateManager(FakeStore()), dao)

        applier.apply(command(1, "LOCK", 5))
        val second = applier.apply(command(1, "LOCK", 5))

        assertEquals(CommandApplier.RESULT_DUPLICATE, second)
        assertEquals(1, lock.entered) // side effect happened exactly once
    }

    @Test
    fun `unlock exits lock and confirms UNLOCKED`() = runTest {
        val lock = FakeLockActions()
        val store = FakeStore().apply { state = DeviceState.LOCKED }
        val applier = CommandApplier(lock, StateManager(store), FakeCommandDao())

        applier.apply(command(2, "UNLOCK", 6))

        assertEquals(1, lock.exited)
        assertEquals(DeviceState.UNLOCKED, store.state)
    }

    @Test
    fun `reapply confirmed state re-locks when LOCKED`() = runTest {
        val lock = FakeLockActions()
        val store = FakeStore().apply { state = DeviceState.LOCKED }
        val applier = CommandApplier(lock, StateManager(store), FakeCommandDao())

        applier.reapplyConfirmedState()

        assertEquals(1, lock.entered)
    }
}
