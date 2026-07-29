package com.example.lock.command

import com.example.lock.data.local.CommandDao
import com.example.lock.data.local.CommandLogEntity
import com.example.lock.data.net.dto.SignedCommand
import com.example.lock.lock.LockActions
import com.example.lock.state.DeviceState
import com.example.lock.state.StateManager

/**
 * U19 / U20 / U22: apply a VERIFIED command exactly once (idempotent), drive
 * the lock/unlock behaviour, and persist the confirmed state. Returns the
 * receipt result to report back to the server (U15).
 */
class CommandApplier(
    private val lockController: LockActions,
    private val stateManager: StateManager,
    private val commandDao: CommandDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    /** Apply a command whose verdict is VALID. */
    suspend fun apply(command: SignedCommand): String {
        // Idempotency guard: never apply the same command twice.
        if (commandDao.find(command.id) != null) {
            return RESULT_DUPLICATE
        }

        val target = when (command.action) {
            ACTION_LOCK -> DeviceState.LOCKED
            ACTION_UNLOCK -> DeviceState.UNLOCKED
            else -> return RESULT_REJECTED
        }

        when (target) {
            DeviceState.LOCKED -> lockController.enterLock()
            DeviceState.UNLOCKED -> lockController.exitLock()
            else -> {}
        }

        stateManager.confirm(target, command.sequence)
        commandDao.insert(
            CommandLogEntity(
                commandId = command.id,
                sequence = command.sequence,
                action = command.action,
                result = RESULT_SUCCESS,
                appliedAt = now(),
            ),
        )
        return RESULT_SUCCESS
    }

    /** Re-apply the last confirmed state on boot without logging a new command. */
    suspend fun reapplyConfirmedState() {
        if (stateManager.isLocked()) {
            lockController.enterLock()
        }
    }

    companion object {
        const val ACTION_LOCK = "LOCK"
        const val ACTION_UNLOCK = "UNLOCK"
        const val RESULT_SUCCESS = "SUCCESS"
        const val RESULT_DUPLICATE = "DUPLICATE"
        const val RESULT_REJECTED = "REJECTED"
    }
}
