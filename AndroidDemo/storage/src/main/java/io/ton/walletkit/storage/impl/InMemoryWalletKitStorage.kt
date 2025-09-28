package io.ton.walletkit.storage.impl

import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.model.StoredSessionHint
import io.ton.walletkit.storage.model.StoredWalletRecord
import java.util.HashMap

class InMemoryWalletKitStorage : WalletKitStorage {
    private val store = HashMap<String, StoredWalletRecord>()
    private val hintStore = HashMap<String, StoredSessionHint>()

    override suspend fun saveWallet(accountId: String, record: StoredWalletRecord) {
        store[accountId] = record
    }

    override suspend fun loadWallet(accountId: String): StoredWalletRecord? = store[accountId]

    override suspend fun loadAllWallets(): Map<String, StoredWalletRecord> = HashMap(store)

    override suspend fun clear(accountId: String) {
        store.remove(accountId)
    }

    override suspend fun saveSessionHint(key: String, hint: StoredSessionHint) {
        hintStore[key] = hint
    }

    override suspend fun loadSessionHints(): Map<String, StoredSessionHint> = HashMap(hintStore)

    override suspend fun clearSessionHint(key: String) {
        hintStore.remove(key)
    }
}
