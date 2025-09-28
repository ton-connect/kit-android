package io.ton.walletkit.storage

import io.ton.walletkit.storage.model.StoredSessionHint
import io.ton.walletkit.storage.model.StoredWalletRecord

interface WalletKitStorage {
    suspend fun saveWallet(accountId: String, record: StoredWalletRecord)

    suspend fun loadWallet(accountId: String): StoredWalletRecord?

    suspend fun loadAllWallets(): Map<String, StoredWalletRecord>

    suspend fun clear(accountId: String)

    suspend fun saveSessionHint(key: String, hint: StoredSessionHint)

    suspend fun loadSessionHints(): Map<String, StoredSessionHint>

    suspend fun clearSessionHint(key: String)
}
