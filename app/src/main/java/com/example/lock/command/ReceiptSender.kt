package com.example.lock.command

import com.example.lock.data.net.ApiService
import com.example.lock.data.net.dto.ReceiptRequest
import com.example.lock.state.DeviceState

/**
 * U19 / AND-013: send a signed (per-device auth) receipt for every command
 * outcome — success, duplicate, expired, rejected, failed — so the server can
 * advance/mark reported_state honestly (U15).
 */
class ReceiptSender(private val api: ApiService) {

    suspend fun send(
        deviceId: String,
        commandId: Long,
        result: String,
        reportedState: DeviceState?,
        errorCode: String? = null,
        errorMessage: String? = null,
    ): Boolean {
        return runCatching {
            api.sendReceipt(
                deviceId,
                commandId,
                ReceiptRequest(
                    result = result,
                    reportedState = reportedState?.name,
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                ),
            ).isSuccessful
        }.getOrDefault(false)
    }
}
