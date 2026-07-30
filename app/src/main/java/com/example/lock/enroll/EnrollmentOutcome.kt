package com.example.lock.enroll

/**
 * U2 / R6: the enrollment result, chosen by a pure mapper (no Android deps) so
 * the copy decision is unit-testable. EnrollmentActivity maps each value to
 * string resources for display.
 */
enum class EnrollmentOutcome(val isSuccess: Boolean, val promptBattery: Boolean) {
    SUCCESS(isSuccess = true, promptBattery = true),
    ALREADY_ENROLLED(isSuccess = true, promptBattery = true),
    NOT_DEVICE_OWNER(isSuccess = false, promptBattery = false),
    TOKEN_REJECTED(isSuccess = false, promptBattery = false),
    NETWORK_ERROR(isSuccess = false, promptBattery = false),
    FAILED(isSuccess = false, promptBattery = false);

    companion object {
        /**
         * Device-Owner status is checked FIRST: if provisioning did not make us
         * Device Owner, the only fix is factory-reset-and-rescan regardless of
         * what the activation call returned (the reachable used-phone case, R3).
         */
        fun from(result: EnrollmentManager.Result, isDeviceOwner: Boolean): EnrollmentOutcome {
            if (!isDeviceOwner) return NOT_DEVICE_OWNER

            return when (result) {
                is EnrollmentManager.Result.Success -> SUCCESS
                is EnrollmentManager.Result.Failure -> when {
                    result.reason.startsWith("already_enrolled") -> ALREADY_ENROLLED
                    result.reason.startsWith("activation_rejected") -> TOKEN_REJECTED
                    result.reason.startsWith("network_error") -> NETWORK_ERROR
                    else -> FAILED
                }
            }
        }
    }
}
