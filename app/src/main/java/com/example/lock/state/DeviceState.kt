package com.example.lock.state

/**
 * Lock state machine — mirrors the server enum (KTD-5). OFFLINE is NOT a member;
 * it is derived server-side from last_seen_at and never stored on the device.
 */
enum class DeviceState {
    ENROLLMENT_PENDING,
    UNLOCKED,
    LOCK_REQUESTED,
    LOCKED,
    UNLOCK_REQUESTED,
    COMMAND_FAILED;

    companion object {
        fun fromOrNull(value: String?): DeviceState? =
            entries.firstOrNull { it.name == value }
    }
}
