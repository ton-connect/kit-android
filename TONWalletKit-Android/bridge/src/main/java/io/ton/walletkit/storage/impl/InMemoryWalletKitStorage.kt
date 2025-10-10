package io.ton.walletkit.storage.impl

import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.data.model.StoredBridgeConfig
import io.ton.walletkit.data.model.StoredSessionData
import io.ton.walletkit.data.model.StoredUserPreferences
import io.ton.walletkit.data.model.StoredWalletRecord
import java.util.HashMap

class InMemoryWalletKitStorage : WalletKitStorage {
    private val store = HashMap<String, StoredWalletRecord>()
    private val sessionStore = HashMap<String, StoredSessionData>()
    private var bridgeConfig: StoredBridgeConfig? = null
    private var userPreferences: StoredUserPreferences? = null

    override suspend fun saveWallet(
        accountId: String,
        record: StoredWalletRecord,
    ) {
        store[accountId] = record
    }

    override suspend fun loadWallet(accountId: String): StoredWalletRecord? = store[accountId]

    override suspend fun loadAllWallets(): Map<String, StoredWalletRecord> = HashMap(store)

    override suspend fun clear(accountId: String) {
        store.remove(accountId)
    }

    override suspend fun saveSessionData(
        sessionId: String,
        session: StoredSessionData,
    ) {
        sessionStore[sessionId] = session
    }

    override suspend fun loadSessionData(sessionId: String): StoredSessionData? = sessionStore[sessionId]

    override suspend fun loadAllSessionData(): Map<String, StoredSessionData> = HashMap(sessionStore)

    override suspend fun clearSessionData(sessionId: String) {
        sessionStore.remove(sessionId)
    }

    override suspend fun updateSessionActivity(
        sessionId: String,
        timestamp: String,
    ) {
        sessionStore[sessionId]?.let { session ->
            sessionStore[sessionId] = session.copy(lastActivityAt = timestamp)
        }
    }

    override suspend fun saveBridgeConfig(config: StoredBridgeConfig) {
        bridgeConfig = config
    }

    override suspend fun loadBridgeConfig(): StoredBridgeConfig? = bridgeConfig

    override suspend fun clearBridgeConfig() {
        bridgeConfig = null
    }

    override suspend fun saveUserPreferences(prefs: StoredUserPreferences) {
        userPreferences = prefs
    }

    override suspend fun loadUserPreferences(): StoredUserPreferences? = userPreferences

    override suspend fun clearUserPreferences() {
        userPreferences = null
    }
}
