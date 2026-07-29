package com.example.lock.boot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lock.state.DeviceState
import com.example.lock.state.StateManager
import com.example.lock.state.StateStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * U23 / AND-010 — the last CONFIRMED state survives process death and is
 * reapplied on boot. Full reboot-to-relock is validated on the U1 matrix
 * (docs/decisions/device-matrix.md, boot-to-relock latency). This asserts the
 * persistence contract: a fresh StateManager over the same store restores the
 * confirmed state, and connectivity change never mutates it.
 */
@RunWith(AndroidJUnit4::class)
class RebootPersistenceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun confirmedLockedStateSurvivesProcessDeath() = runBlocking {
        StateManager(StateStore(context)).confirm(DeviceState.LOCKED, 3)

        // New instances simulate a cold start after reboot/process death.
        val restored = StateManager(StateStore(context))
        assertEquals(DeviceState.LOCKED, restored.currentState())
        assertEquals(3L, restored.lastAppliedSequence())
    }

    @Test
    fun connectivityLossDoesNotChangeState() = runBlocking {
        val manager = StateManager(StateStore(context))
        manager.confirm(DeviceState.LOCKED, 4)
        manager.onConnectivityChanged(false)
        assertEquals(DeviceState.LOCKED, manager.currentState())
    }
}
