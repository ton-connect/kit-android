package io.ton.walletkit.storage.impl

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.model.StoredBridgeConfig
import io.ton.walletkit.storage.model.StoredSessionData
import io.ton.walletkit.storage.model.StoredUserPreferences
import io.ton.walletkit.storage.model.StoredWalletRecord
import io.ton.walletkit.storage.util.toJson
import io.ton.walletkit.storage.util.toStoredWalletRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DebugSharedPrefsStorage(
    context: Context,
) : WalletKitStorage {
    private val prefs = context.getSharedPreferences("walletkit-demo", Context.MODE_PRIVATE)

    private fun walletKey(accountId: String) = "wallet:$accountId"

    private fun sessionKey(sessionId: String) = "session:$sessionId"

    override suspend fun saveWallet(
        accountId: String,
        record: StoredWalletRecord,
    ) {
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

    // Session data methods (INSECURE - stores private keys in plain text!)
    override suspend fun saveSessionData(
        sessionId: String,
        session: StoredSessionData,
    ) {
        withContext(Dispatchers.IO) {
            Log.w(TAG, "WARNING: Storing session private key in PLAIN TEXT (Debug mode only)")
            val sessionJson =
                JSONObject().apply {
                    put("sessionId", session.sessionId)
                    put("walletAddress", session.walletAddress)
                    put("createdAt", session.createdAt)
                    put("lastActivityAt", session.lastActivityAt)
                    put("privateKey", session.privateKey) // PLAIN TEXT - INSECURE!
                    put("publicKey", session.publicKey)
                    put("dAppName", session.dAppName)
                    put("dAppDescription", session.dAppDescription)
                    put("domain", session.domain)
                    put("dAppIconUrl", session.dAppIconUrl)
                }
            prefs.edit {
                putString(sessionKey(sessionId), sessionJson.toString())
            }
        }
    }

    override suspend fun loadSessionData(sessionId: String): StoredSessionData? = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(sessionKey(sessionId), null) ?: return@withContext null
        try {
            val json = JSONObject(jsonString)
            StoredSessionData(
                sessionId = json.getString("sessionId"),
                walletAddress = json.getString("walletAddress"),
                createdAt = json.getString("createdAt"),
                lastActivityAt = json.getString("lastActivityAt"),
                privateKey = json.getString("privateKey"),
                publicKey = json.getString("publicKey"),
                dAppName = json.getString("dAppName"),
                dAppDescription = json.getString("dAppDescription"),
                domain = json.getString("domain"),
                dAppIconUrl = json.getString("dAppIconUrl"),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse session data", e)
            null
        }
    }

    override suspend fun loadAllSessionData(): Map<String, StoredSessionData> = withContext(Dispatchers.IO) {
        prefs.all
            .mapNotNull { (key, value) ->
                if (!key.startsWith("session:")) return@mapNotNull null
                val sessionId = key.removePrefix("session:")
                val sessionData = (value as? String)?.let { jsonString ->
                    try {
                        val json = JSONObject(jsonString)
                        StoredSessionData(
                            sessionId = json.getString("sessionId"),
                            walletAddress = json.getString("walletAddress"),
                            createdAt = json.getString("createdAt"),
                            lastActivityAt = json.getString("lastActivityAt"),
                            privateKey = json.getString("privateKey"),
                            publicKey = json.getString("publicKey"),
                            dAppName = json.getString("dAppName"),
                            dAppDescription = json.getString("dAppDescription"),
                            domain = json.getString("domain"),
                            dAppIconUrl = json.getString("dAppIconUrl"),
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse session data: $sessionId", e)
                        null
                    }
                }
                if (sessionData != null) sessionId to sessionData else null
            }.toMap()
    }

    override suspend fun clearSessionData(sessionId: String) {
        withContext(Dispatchers.IO) {
            prefs.edit { remove(sessionKey(sessionId)) }
        }
    }

    override suspend fun updateSessionActivity(
        sessionId: String,
        timestamp: String,
    ) {
        withContext(Dispatchers.IO) {
            val session = loadSessionData(sessionId)
            if (session != null) {
                saveSessionData(sessionId, session.copy(lastActivityAt = timestamp))
            }
        }
    }

    // Bridge configuration
    override suspend fun saveBridgeConfig(config: StoredBridgeConfig) {
        withContext(Dispatchers.IO) {
            Log.w(TAG, "WARNING: Storing API key in PLAIN TEXT (Debug mode only)")
            val configJson =
                JSONObject().apply {
                    put("network", config.network)
                    config.tonClientEndpoint?.let { put("tonClientEndpoint", it) }
                    config.tonApiUrl?.let { put("tonApiUrl", it) }
                    config.apiKey?.let { put("apiKey", it) } // PLAIN TEXT - INSECURE!
                    config.bridgeUrl?.let { put("bridgeUrl", it) }
                    config.bridgeName?.let { put("bridgeName", it) }
                }
            prefs.edit {
                putString("bridge_config", configJson.toString())
            }
        }
    }

    override suspend fun loadBridgeConfig(): StoredBridgeConfig? = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString("bridge_config", null) ?: return@withContext null
        try {
            val json = JSONObject(jsonString)
            StoredBridgeConfig(
                network = json.getString("network"),
                tonClientEndpoint = json.optString("tonClientEndpoint", null),
                tonApiUrl = json.optString("tonApiUrl", null),
                apiKey = json.optString("apiKey", null),
                bridgeUrl = json.optString("bridgeUrl", null),
                bridgeName = json.optString("bridgeName", null),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse bridge config", e)
            null
        }
    }

    override suspend fun clearBridgeConfig() {
        withContext(Dispatchers.IO) {
            prefs.edit { remove("bridge_config") }
        }
    }

    // User preferences
    override suspend fun saveUserPreferences(prefs: StoredUserPreferences) {
        withContext(Dispatchers.IO) {
            val prefsJson =
                JSONObject().apply {
                    prefs.activeWalletAddress?.let { put("activeWalletAddress", it) }
                    prefs.lastSelectedNetwork?.let { put("lastSelectedNetwork", it) }
                }
            this@DebugSharedPrefsStorage.prefs.edit {
                putString("user_preferences", prefsJson.toString())
            }
        }
    }

    override suspend fun loadUserPreferences(): StoredUserPreferences? = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString("user_preferences", null) ?: return@withContext null
        try {
            val json = JSONObject(jsonString)
            StoredUserPreferences(
                activeWalletAddress = json.optString("activeWalletAddress", null),
                lastSelectedNetwork = json.optString("lastSelectedNetwork", null),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user preferences", e)
            null
        }
    }

    override suspend fun clearUserPreferences() {
        withContext(Dispatchers.IO) {
            prefs.edit { remove("user_preferences") }
        }
    }

    companion object {
        private const val TAG = "DebugSharedPrefsStorage"
    }
}
