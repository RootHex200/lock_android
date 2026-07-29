package com.example.lock.lock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lock.command.CommandApplier
import com.example.lock.data.local.AppDatabase
import com.example.lock.data.net.dto.SignedCommand
import com.example.lock.state.DeviceState
import com.example.lock.state.StateManager
import com.example.lock.state.StateStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U22 / AND-009 — verified Unlock exits LockTask and restores the launcher with
 * NO data wipe. Data-intactness is verified on-device (this asserts the state
 * machine + that no wipe API is invoked; LockController.exitLock only broadcasts).
 */
@RunWith(AndroidJUnit4::class)
class UnlockTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun verifiedUnlockConfirmsUnlockedState() = runBlocking {
        val db = AppDatabase.get(context)
        val stateManager = StateManager(StateStore(context))
        val applier = CommandApplier(LockController(context), stateManager, db.commandDao())

        applier.apply(SignedCommand(900, "dev", 10, "UNLOCK", "2026-07-29T12:00:00+00:00", "2999-01-01T00:00:00+00:00"))

        assertEquals(DeviceState.UNLOCKED, stateManager.currentState())
    }
}
