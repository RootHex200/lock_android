package com.example.lock.enroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * U2 / R6, R3: enrollment result copy is chosen by a pure mapper so it's
 * unit-testable without an emulator.
 */
class EnrollmentOutcomeTest {

    private fun success() = EnrollmentManager.Result.Success("dev-1")
    private fun failure(reason: String) = EnrollmentManager.Result.Failure(reason)

    @Test
    fun `success while device owner is SUCCESS and offers battery prompt`() {
        val outcome = EnrollmentOutcome.from(success(), isDeviceOwner = true)
        assertEquals(EnrollmentOutcome.SUCCESS, outcome)
        assertTrue(outcome.isSuccess)
        assertTrue(outcome.promptBattery)
    }

    @Test
    fun `success but not device owner asks for factory reset and rescan`() {
        val outcome = EnrollmentOutcome.from(success(), isDeviceOwner = false)
        assertEquals(EnrollmentOutcome.NOT_DEVICE_OWNER, outcome)
        assertFalse(outcome.isSuccess)
        assertFalse(outcome.promptBattery)
    }

    @Test
    fun `rejected token maps to TOKEN_REJECTED`() {
        assertEquals(
            EnrollmentOutcome.TOKEN_REJECTED,
            EnrollmentOutcome.from(failure("activation_rejected:422"), isDeviceOwner = true),
        )
    }

    @Test
    fun `network error maps to NETWORK_ERROR`() {
        assertEquals(
            EnrollmentOutcome.NETWORK_ERROR,
            EnrollmentOutcome.from(failure("network_error:timeout"), isDeviceOwner = true),
        )
    }

    @Test
    fun `already enrolled is treated as a success state`() {
        val outcome = EnrollmentOutcome.from(failure("already_enrolled"), isDeviceOwner = true)
        assertEquals(EnrollmentOutcome.ALREADY_ENROLLED, outcome)
        assertTrue(outcome.isSuccess)
    }

    @Test
    fun `unknown failure maps to FAILED`() {
        assertEquals(
            EnrollmentOutcome.FAILED,
            EnrollmentOutcome.from(failure("empty_activation_body"), isDeviceOwner = true),
        )
    }
}
