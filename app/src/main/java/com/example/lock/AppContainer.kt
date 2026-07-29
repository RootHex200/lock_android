package com.example.lock

import android.content.Context
import com.example.lock.command.CommandApplier
import com.example.lock.command.CommandVerifier
import com.example.lock.command.ReceiptSender
import com.example.lock.crypto.SodiumSignatureVerifier
import com.example.lock.data.keystore.DeviceKeyStore
import com.example.lock.data.local.AppDatabase
import com.example.lock.data.local.CredentialStore
import com.example.lock.data.net.ApiClient
import com.example.lock.data.net.ApiService
import com.example.lock.lock.LockController
import com.example.lock.state.StateManager
import com.example.lock.state.StateStore
import com.example.lock.sync.CommandPoller

/**
 * Minimal service locator wiring the sync graph from a Context. Keeps the
 * Android entry points (workers, receivers, FCM service) thin and the core
 * logic (CommandPoller/Verifier/Applier) plain + unit-testable.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val api: ApiService by lazy { ApiClient.create(appContext) }
    val credentials: CredentialStore by lazy { CredentialStore(appContext) }
    val keyStore: DeviceKeyStore by lazy { DeviceKeyStore() }
    private val db: AppDatabase by lazy { AppDatabase.get(appContext) }
    val stateManager: StateManager by lazy { StateManager(StateStore(appContext)) }
    val lockController: LockController by lazy { LockController(appContext) }

    val commandApplier: CommandApplier by lazy {
        CommandApplier(lockController, stateManager, db.commandDao())
    }

    val commandPoller: CommandPoller by lazy {
        CommandPoller(
            api = api,
            credentials = credentials,
            verifier = CommandVerifier(SodiumSignatureVerifier()),
            applier = commandApplier,
            receipts = ReceiptSender(api),
            stateManager = stateManager,
        )
    }

    fun isEnrolled(): Boolean = credentials.isEnrolled()
}
