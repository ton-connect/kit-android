package io.ton.walletkit.storage.impl

import android.content.Context
import androidx.core.content.edit
import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.model.StoredSessionHint
import io.ton.walletkit.storage.model.StoredWalletRecord
import io.ton.walletkit.storage.util.toJson
import io.ton.walletkit.storage.util.toStoredSessionHint
import io.ton.walletkit.storage.util.toStoredWalletRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DebugSharedPrefsStorage(
    context: Context,
) : WalletKitStorage {
    private val prefs = context.getSharedPreferences("walletkit-demo", Context.MODE_PRIVATE)

    private fun walletKey(accountId: String) = "wallet:$accountId"

    private fun hintKey(key: String) = "hint:$key"

    override suspend fun saveWallet(accountId: String, record: StoredWalletRecord) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putString(walletKey(accountId), record.toJson().toString())
            }
        }
    }

    override suspend fun loadWallet(accountId: String): StoredWalletRecord? {
        val raw =
            withContext(Dispatchers.IO) {
                prefs.getString(walletKey(accountId), null)
                    ?: prefs.getString(accountId, null)
            } ?: return null
        return raw.toStoredWalletRecord()
    }

    override suspend fun loadAllWallets(): Map<String, StoredWalletRecord> = withContext(Dispatchers.IO) {
        prefs.all
            .mapNotNull { (key, value) ->
                if (!key.startsWith("wallet:")) return@mapNotNull null
                val record = (value as? String)?.toStoredWalletRecord()
                if (record != null) key.removePrefix("wallet:") to record else null
            }.toMap()
    }

    override suspend fun clear(accountId: String) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                remove(walletKey(accountId))
                remove(accountId)
            }
        }
    }

    override suspend fun saveSessionHint(key: String, hint: StoredSessionHint) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putString(hintKey(key), hint.toJson().toString())
            }
        }
    }

    override suspend fun loadSessionHints(): Map<String, StoredSessionHint> = withContext(Dispatchers.IO) {
        prefs.all
            .mapNotNull { (key, value) ->
                if (!key.startsWith("hint:")) return@mapNotNull null
                val hint = (value as? String)?.toStoredSessionHint()
                if (hint != null) key.removePrefix("hint:") to hint else null
            }.toMap()
    }

    override suspend fun clearSessionHint(key: String) {
        withContext(Dispatchers.IO) {
            prefs.edit { remove(hintKey(key)) }
        }
    }
}
