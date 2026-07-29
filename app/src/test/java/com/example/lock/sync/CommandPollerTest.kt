package com.example.lock.sync

import com.example.lock.command.CommandApplier
import com.example.lock.command.CommandVerifier
import com.example.lock.command.ReceiptSender
import com.example.lock.crypto.JvmEd25519
import com.example.lock.crypto.JvmEd25519Verifier
import com.example.lock.data.local.CommandDao
import com.example.lock.data.local.CommandLogEntity
import com.example.lock.data.local.DeviceCredentials
import com.example.lock.data.net.ApiService
import com.example.lock.lock.LockActions
import com.example.lock.state.DeviceState
import com.example.lock.state.LockStateStore
import com.example.lock.state.StateManager
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Base64

/**
 * U12 / U19: end-to-end poll → verify (real Ed25519) → apply → receipt against a
 * MockWebServer serving the frozen commands/next wire shape.
 */
class CommandPollerTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService
    private lateinit var keys: JvmEd25519.Keys
    private val deviceId = "11111111-1111-4111-8111-111111111111"

    private class FakeCreds(val dev: String, val pub: String) : DeviceCredentials {
        override fun deviceId() = dev
        override fun credentialId() = "cred"
        override fun serverSigningPublicKey() = pub
        override fun isEnrolled() = true
        override fun save(deviceId: String, credentialId: String, serverSigningPublicKeyB64: String) {}
    }

    private class FakeLock : LockActions {
        var entered = 0; var exited = 0
        override fun enterLock() { entered++ }
        override fun exitLock() { exited++ }
    }

    private class FakeDao : CommandDao {
        val log = mutableMapOf<Long, CommandLogEntity>()
        override suspend fun insert(entry: CommandLogEntity) =
            if (log.containsKey(entry.commandId)) -1 else { log[entry.commandId] = entry; entry.commandId }
        override suspend fun find(commandId: Long) = log[commandId]
        override suspend fun maxSequence() = log.values.maxOfOrNull { it.sequence }
        override suspend fun recent() = log.values.toList()
    }

    private class FakeStore : LockStateStore {
        var state = DeviceState.UNLOCKED; var seq = 0L
        override suspend fun confirmedState() = state
        override suspend fun lastAppliedSequence() = seq
        override suspend fun setConfirmedState(state: DeviceState, sequence: Long) { this.state = state; this.seq = sequence }
    }

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        keys = JvmEd25519.generate()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun signedCommandBody(action: String = "LOCK", sequence: Long = 1): Pair<String, String> {
        val body = """{"id":123,"device_id":"$deviceId","sequence":$sequence,""" +
            """"action":"$action","issued_at":"2026-07-29T12:00:00+00:00","expires_at":"2999-01-01T00:00:00+00:00"}"""
        val sig = Base64.getEncoder().encodeToString(JvmEd25519.sign(keys.privateEncoded, body.toByteArray()))
        return body to sig
    }

    private fun poller(store: FakeStore, lock: FakeLock, dao: FakeDao): CommandPoller {
        val stateManager = StateManager(store)
        return CommandPoller(
            api = api,
            credentials = FakeCreds(deviceId, Base64.getEncoder().encodeToString(keys.publicRaw)),
            verifier = CommandVerifier(JvmEd25519Verifier()),
            applier = CommandApplier(lock, stateManager, dao),
            receipts = ReceiptSender(api),
            stateManager = stateManager,
        )
    }

    @Test
    fun `valid signed command is applied and a success receipt is posted`() = runTest {
        val (body, sig) = signedCommandBody("LOCK", 1)
        server.enqueue(MockResponse().setResponseCode(200).setBody(body).addHeader("X-Command-Signature", sig))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"receipt_id":1,"command_status":"CONFIRMED","reported_state":"LOCKED","desired_state":"LOCKED","accepted":true}"""))

        val store = FakeStore(); val lock = FakeLock(); val dao = FakeDao()
        val outcome = poller(store, lock, dao).pollOnce()

        assertEquals(CommandPoller.Outcome.APPLIED, outcome)
        assertEquals(1, lock.entered)
        assertEquals(DeviceState.LOCKED, store.state)

        // First request = poll; second = receipt with SUCCESS.
        server.takeRequest()
        val receipt = server.takeRequest()
        assertTrue(receipt.path!!.endsWith("/commands/123/receipt"))
        assertTrue(receipt.body.readUtf8().contains("SUCCESS"))
    }

    @Test
    fun `204 no content yields NO_COMMAND`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val outcome = poller(FakeStore(), FakeLock(), FakeDao()).pollOnce()
        assertEquals(CommandPoller.Outcome.NO_COMMAND, outcome)
    }

    @Test
    fun `tampered signature is rejected and reported`() = runTest {
        val (body, _) = signedCommandBody("LOCK", 1)
        val badSig = Base64.getEncoder().encodeToString(ByteArray(64))
        server.enqueue(MockResponse().setResponseCode(200).setBody(body).addHeader("X-Command-Signature", badSig))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"receipt_id":1,"command_status":"PENDING","reported_state":"UNLOCKED","desired_state":"LOCK_REQUESTED","accepted":true}"""))

        val lock = FakeLock()
        val outcome = poller(FakeStore(), lock, FakeDao()).pollOnce()

        assertEquals(CommandPoller.Outcome.REJECTED, outcome)
        assertEquals(0, lock.entered) // never applied a bad-signature command
    }
}
