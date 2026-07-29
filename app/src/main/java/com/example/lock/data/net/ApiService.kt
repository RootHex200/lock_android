package com.example.lock.data.net

import com.example.lock.data.net.dto.ActivateRequest
import com.example.lock.data.net.dto.ActivateResponse
import com.example.lock.data.net.dto.HeartbeatRequest
import com.example.lock.data.net.dto.HeartbeatResponse
import com.example.lock.data.net.dto.ReceiptRequest
import com.example.lock.data.net.dto.ReceiptResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Device→server API (v1). Endpoints + shapes are the frozen Cross-Repo
 * contract (owner: lock_admin U8/U9/U15).
 */
interface ApiService {

    @POST("api/v1/enrollments/{token}/activate")
    suspend fun activate(
        @Path("token") token: String,
        @Body body: ActivateRequest,
    ): Response<ActivateResponse>

    @POST("api/v1/devices/{deviceId}/heartbeat")
    suspend fun heartbeat(
        @Path("deviceId") deviceId: String,
        @Body body: HeartbeatRequest,
    ): Response<HeartbeatResponse>

    /**
     * Returns the RAW signed command bytes (200) or 204 when none. The caller
     * must verify the detached signature (X-Command-Signature header) over the
     * exact received bytes — never re-serialize (KTD-4).
     */
    @GET("api/v1/devices/{deviceId}/commands/next")
    suspend fun nextCommand(
        @Path("deviceId") deviceId: String,
    ): Response<ResponseBody>

    @POST("api/v1/devices/{deviceId}/commands/{commandId}/receipt")
    suspend fun sendReceipt(
        @Path("deviceId") deviceId: String,
        @Path("commandId") commandId: Long,
        @Body body: ReceiptRequest,
    ): Response<ReceiptResponse>
}
