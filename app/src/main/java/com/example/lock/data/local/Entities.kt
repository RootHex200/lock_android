package com.example.lock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Non-secret device state persisted for reboot/process-death restoration
 * (U11/U23). The single row (id=1) holds the last CONFIRMED lock state and the
 * last applied command sequence (idempotency, U19).
 */
@Entity(tableName = "device_state")
data class StateEntity(
    @PrimaryKey val id: Int = 1,
    val confirmedState: String,
    val lastAppliedSequence: Long,
    val updatedAt: Long,
)

/**
 * Applied-command log for idempotency + diagnostics (U19). One row per command
 * the device has processed.
 */
@Entity(tableName = "command_log")
data class CommandLogEntity(
    @PrimaryKey val commandId: Long,
    val sequence: Long,
    val action: String,
    val result: String,
    val appliedAt: Long,
)
