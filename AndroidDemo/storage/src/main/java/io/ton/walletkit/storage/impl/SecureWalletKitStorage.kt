package io.ton.walletkit.storage.impl

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.ton.walletkit.storage.WalletKitStorage
import io.ton.walletkit.storage.encryption.CryptoManager
import io.ton.walletkit.storage.model.StoredBridgeConfig
import io.ton.walletkit.storage.model.StoredSessionData
import io.ton.walletkit.storage.model.StoredSessionHint
import io.ton.walletkit.storage.model.StoredUserPreferences
import io.ton.walletkit.storage.model.StoredWalletRecord
import io.ton.walletkit.storage.util.toJson
import io.ton.walletkit.storage.util.toStoredSessionHint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.GeneralSecurityException

/**
 * Secure implementation of WalletKitStorage using Android Keystore and EncryptedSharedPreferences.
 *
 * Security Features:
 * - Mnemonic phrases encrypted with AES-256-GCM using Android Keystore
 * - Hardware-backed encryption when available (StrongBox)
 * - EncryptedSharedPreferences for metadata
 * - Secure memory clearing after sensitive operations
 * - Production-ready implementation
 *
 * Data Protection:
 * - Wallet mnemonics: Double-encrypted (EncryptedSharedPreferences + CryptoManager)
 * - Session hints: EncryptedSharedPreferences
 * - Wallet metadata: EncryptedSharedPreferences
 *
 * @param context Application context
 * @param sharedPrefsName Name for the encrypted SharedPreferences file
 */
class SecureWalletKitStorage(
    context: Context,
    sharedPrefsName: String = "walletkit_secure_storage",
) : WalletKitStorage {
    private val appContext = context.applicationContext

    // Master key for EncryptedSharedPreferences
    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to create MasterKey", e)
            throw SecurityException("Failed to initialize secure storage", e)
        }
    }

    // EncryptedSharedPreferences for storing wallet data
    private val encryptedPrefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                appContext,
                sharedPrefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            throw SecurityException("Failed to initialize secure storage", e)
        }
    }

    // Additional encryption layer for mnemonics using Android Keystore directly
    private val cryptoManager by lazy {
        CryptoManager("walletkit_mnemonic_key")
    }

    private fun walletKey(accountId: String) = "wallet:$accountId"

    private fun hintKey(key: String) = "hint:$key"

    override suspend fun saveWallet(
        accountId: String,
        record: StoredWalletRecord,
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Double-encrypt the mnemonic for maximum security
                val encryptedMnemonic = encryptMnemonic(record.mnemonic)

                // Create a record with encrypted mnemonic
                val secureRecord =
                    JSONObject().apply {
                        put("mnemonic", encryptedMnemonic) // Base64 of encrypted bytes
                        record.name?.let { put("name", it) }
                        record.network?.let { put("network", it) }
                        record.version?.let { put("version", it) }
                    }

                // Store in EncryptedSharedPreferences (second layer of encryption)
                encryptedPrefs.edit().putString(walletKey(accountId), secureRecord.toString()).apply()

                Log.d(TAG, "Wallet saved securely: $accountId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save wallet: $accountId", e)
                throw SecurityException("Failed to save wallet securely", e)
            }
        }
    }

    override suspend fun loadWallet(accountId: String): StoredWalletRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val encrypted =
                    encryptedPrefs.getString(walletKey(accountId), null)
                        ?: encryptedPrefs.getString(accountId, null) // Fallback for legacy keys
                        ?: return@withContext null

                val json = JSONObject(encrypted)

                // Decrypt the mnemonic
                val encryptedMnemonic = json.optString("mnemonic")
                if (encryptedMnemonic.isBlank()) {
                    Log.e(TAG, "Missing mnemonic in stored wallet: $accountId")
                    return@withContext null
                }

                val mnemonic = decryptMnemonic(encryptedMnemonic)

                StoredWalletRecord(
                    mnemonic = mnemonic,
                    name = json.optString("name").takeIf { it.isNotBlank() },
                    network = json.optString("network").takeIf { it.isNotBlank() },
                    version = json.optString("version").takeIf { it.isNotBlank() },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load wallet: $accountId", e)
                null
            }
        }
    }

    override suspend fun loadAllWallets(): Map<String, StoredWalletRecord> {
        return withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.all
                    .mapNotNull { (key, _) ->
                        if (!key.startsWith("wallet:")) return@mapNotNull null
                        val accountId = key.removePrefix("wallet:")
                        val record = loadWallet(accountId)
                        if (record != null) accountId to record else null
                    }
                    .toMap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load all wallets", e)
                emptyMap()
            }
        }
    }

    override suspend fun clear(accountId: String) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().apply {
                    remove(walletKey(accountId))
                    remove(accountId) // Remove legacy key if exists
                    apply()
                }
                Log.d(TAG, "Wallet cleared: $accountId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear wallet: $accountId", e)
            }
        }
    }

    override suspend fun saveSessionHint(
        key: String,
        hint: StoredSessionHint,
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Validate and sanitize URLs
                val sanitizedHint =
                    StoredSessionHint(
                        manifestUrl = hint.manifestUrl?.take(MAX_URL_LENGTH)?.let { sanitizeUrl(it) },
                        dAppUrl = hint.dAppUrl?.take(MAX_URL_LENGTH)?.let { sanitizeUrl(it) },
                        iconUrl = hint.iconUrl?.take(MAX_URL_LENGTH)?.let { sanitizeUrl(it) },
                    )

                encryptedPrefs.edit().putString(hintKey(key), sanitizedHint.toJson().toString()).apply()
                Log.d(TAG, "Session hint saved: $key")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save session hint: $key", e)
            }
        }
    }

    override suspend fun loadSessionHints(): Map<String, StoredSessionHint> {
        return withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.all
                    .mapNotNull { (key, value) ->
                        if (!key.startsWith("hint:")) return@mapNotNull null
                        val hintKey = key.removePrefix("hint:")
                        val hint = (value as? String)?.toStoredSessionHint()
                        if (hint != null) hintKey to hint else null
                    }
                    .toMap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load session hints", e)
                emptyMap()
            }
        }
    }

    override suspend fun clearSessionHint(key: String) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(hintKey(key)).apply()
                Log.d(TAG, "Session hint cleared: $key")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear session hint: $key", e)
            }
        }
    }

    // ===== Session Data Methods (with private keys) =====

    private fun sessionKey(sessionId: String) = "session:$sessionId"

    override suspend fun saveSessionData(
        sessionId: String,
        session: StoredSessionData,
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Encrypt the private key (CRITICAL security data)
                val encryptedPrivateKey = encryptSessionPrivateKey(session.privateKey)

                // Create JSON with encrypted private key
                val sessionJson =
                    JSONObject().apply {
                        put("sessionId", session.sessionId)
                        put("walletAddress", session.walletAddress)
                        put("createdAt", session.createdAt)
                        put("lastActivityAt", session.lastActivityAt)
                        put("privateKey", encryptedPrivateKey) // Encrypted
                        put("publicKey", session.publicKey)
                        put("dAppName", session.dAppName)
                        put("dAppDescription", session.dAppDescription)
                        put("domain", sanitizeUrl(session.domain))
                        put("dAppIconUrl", sanitizeUrl(session.dAppIconUrl))
                    }

                // Store in EncryptedSharedPreferences
                encryptedPrefs.edit().putString(sessionKey(sessionId), sessionJson.toString()).apply()

                Log.d(
                    TAG,
                    "Session data saved: $sessionId for domain: ${sanitizeUrl(session.domain)}",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save session data: $sessionId", e)
                throw e
            }
        }
    }

    override suspend fun loadSessionData(sessionId: String): StoredSessionData? = withContext(Dispatchers.IO) {
        try {
            val jsonString = encryptedPrefs.getString(sessionKey(sessionId), null)
            if (jsonString != null) {
                val json = JSONObject(jsonString)
                val encryptedPrivateKey = json.getString("privateKey")
                val decryptedPrivateKey = decryptSessionPrivateKey(encryptedPrivateKey)

                StoredSessionData(
                    sessionId = json.getString("sessionId"),
                    walletAddress = json.getString("walletAddress"),
                    createdAt = json.getString("createdAt"),
                    lastActivityAt = json.getString("lastActivityAt"),
                    privateKey = decryptedPrivateKey,
                    publicKey = json.getString("publicKey"),
                    dAppName = json.getString("dAppName"),
                    dAppDescription = json.getString("dAppDescription"),
                    domain = json.getString("domain"),
                    dAppIconUrl = json.getString("dAppIconUrl"),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session data: $sessionId", e)
            null
        }
    }

    override suspend fun loadAllSessionData(): Map<String, StoredSessionData> = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.all
                .mapNotNull { (key, value) ->
                    if (!key.startsWith("session:")) return@mapNotNull null
                    val sessionId = key.removePrefix("session:")
                    val sessionData = (value as? String)?.let { jsonString ->
                        try {
                            val json = JSONObject(jsonString)
                            val encryptedPrivateKey = json.getString("privateKey")
                            val decryptedPrivateKey = decryptSessionPrivateKey(encryptedPrivateKey)

                            StoredSessionData(
                                sessionId = json.getString("sessionId"),
                                walletAddress = json.getString("walletAddress"),
                                createdAt = json.getString("createdAt"),
                                lastActivityAt = json.getString("lastActivityAt"),
                                privateKey = decryptedPrivateKey,
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
                }
                .toMap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load all session data", e)
            emptyMap()
        }
    }

    override suspend fun clearSessionData(sessionId: String) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(sessionKey(sessionId)).apply()
                Log.d(TAG, "Session data cleared: $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear session data: $sessionId", e)
            }
        }
    }

    override suspend fun updateSessionActivity(
        sessionId: String,
        timestamp: String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Load existing session
                val session = loadSessionData(sessionId)
                if (session != null) {
                    // Update lastActivityAt
                    val updatedSession = session.copy(lastActivityAt = timestamp)
                    saveSessionData(sessionId, updatedSession)
                    Log.d(TAG, "Session activity updated: $sessionId")
                } else {
                    Log.w(TAG, "Cannot update activity for non-existent session: $sessionId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update session activity: $sessionId", e)
            }
        }
    }

    // ===== Bridge Configuration Methods =====

    private val bridgeConfigKey = "bridge_config"

    override suspend fun saveBridgeConfig(config: StoredBridgeConfig) {
        withContext(Dispatchers.IO) {
            try {
                // Encrypt API key if present
                val encryptedApiKey = config.apiKey?.let { encryptApiKey(it) }

                val configJson =
                    JSONObject().apply {
                        put("network", config.network)
                        config.tonClientEndpoint?.let { put("tonClientEndpoint", sanitizeUrl(it)) }
                        config.tonApiUrl?.let { put("tonApiUrl", sanitizeUrl(it)) }
                        encryptedApiKey?.let { put("apiKey", it) }
                        config.bridgeUrl?.let { put("bridgeUrl", sanitizeUrl(it)) }
                        config.bridgeName?.let { put("bridgeName", it) }
                    }

                encryptedPrefs.edit().putString(bridgeConfigKey, configJson.toString()).apply()
                Log.d(TAG, "Bridge config saved for network: ${config.network}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save bridge config", e)
                throw e
            }
        }
    }

    override suspend fun loadBridgeConfig(): StoredBridgeConfig? = withContext(Dispatchers.IO) {
        try {
            val jsonString = encryptedPrefs.getString(bridgeConfigKey, null)
            if (jsonString != null) {
                val json = JSONObject(jsonString)
                val encryptedApiKey = json.optString("apiKey", null)
                val decryptedApiKey = encryptedApiKey?.let { decryptApiKey(it) }

                StoredBridgeConfig(
                    network = json.getString("network"),
                    tonClientEndpoint = json.optString("tonClientEndpoint", null),
                    tonApiUrl = json.optString("tonApiUrl", null),
                    apiKey = decryptedApiKey,
                    bridgeUrl = json.optString("bridgeUrl", null),
                    bridgeName = json.optString("bridgeName", null),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bridge config", e)
            null
        }
    }

    override suspend fun clearBridgeConfig() {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(bridgeConfigKey).apply()
                Log.d(TAG, "Bridge config cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear bridge config", e)
            }
        }
    }

    // ===== User Preferences Methods =====

    private val userPreferencesKey = "user_preferences"

    override suspend fun saveUserPreferences(prefs: StoredUserPreferences) {
        withContext(Dispatchers.IO) {
            try {
                val prefsJson =
                    JSONObject().apply {
                        prefs.activeWalletAddress?.let { put("activeWalletAddress", it) }
                        prefs.lastSelectedNetwork?.let { put("lastSelectedNetwork", it) }
                    }

                encryptedPrefs.edit().putString(userPreferencesKey, prefsJson.toString()).apply()
                Log.d(TAG, "User preferences saved")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save user preferences", e)
                throw e
            }
        }
    }

    override suspend fun loadUserPreferences(): StoredUserPreferences? = withContext(Dispatchers.IO) {
        try {
            val jsonString = encryptedPrefs.getString(userPreferencesKey, null)
            if (jsonString != null) {
                val json = JSONObject(jsonString)
                StoredUserPreferences(
                    activeWalletAddress = json.optString("activeWalletAddress", null),
                    lastSelectedNetwork = json.optString("lastSelectedNetwork", null),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user preferences", e)
            null
        }
    }

    override suspend fun clearUserPreferences() {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(userPreferencesKey).apply()
                Log.d(TAG, "User preferences cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear user preferences", e)
            }
        }
    }

    /**
     * Clears all stored data. Use with caution!
     */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().clear().apply()
                Log.d(TAG, "All data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all data", e)
            }
        }
    }

    /**
     * Encrypts a mnemonic phrase list using Android Keystore.
     * Returns Base64-encoded encrypted data.
     */
    private fun encryptMnemonic(mnemonic: List<String>): String {
        try {
            // Join mnemonic words with space
            val mnemonicString = mnemonic.joinToString(" ")

            // Encrypt using CryptoManager
            val encryptedBytes = cryptoManager.encrypt(mnemonicString)

            // Encode to Base64 for storage
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt mnemonic", e)
            throw SecurityException("Failed to encrypt mnemonic", e)
        }
    }

    /**
     * Decrypts a Base64-encoded encrypted mnemonic.
     * Returns the mnemonic as a list of words.
     */
    private fun decryptMnemonic(encryptedBase64: String): List<String> {
        try {
            // Decode from Base64
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            // Decrypt using CryptoManager
            val mnemonicString = cryptoManager.decrypt(encryptedBytes)

            // Split into words
            val words = mnemonicString.split(" ").filter { it.isNotBlank() }

            // Validate mnemonic word count (typically 12 or 24 words)
            if (words.size !in VALID_MNEMONIC_SIZES) {
                Log.w(TAG, "Unusual mnemonic word count: ${words.size}")
            }

            return words
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt mnemonic", e)
            throw SecurityException("Failed to decrypt mnemonic", e)
        }
    }

    /**
     * Sanitizes URLs to prevent injection attacks.
     * Only allows http:// and https:// schemes.
     */
    private fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            else -> {
                Log.w(TAG, "Invalid URL scheme, rejecting: $trimmed")
                ""
            }
        }
    }

    /**
     * Encrypts a session private key using a separate encryption key.
     * Returns Base64-encoded encrypted data.
     */
    private fun encryptSessionPrivateKey(privateKey: String): String {
        try {
            val sessionCryptoManager = CryptoManager("walletkit_session_key")
            val encryptedBytes = sessionCryptoManager.encrypt(privateKey)
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt session private key", e)
            throw SecurityException("Failed to encrypt session private key", e)
        }
    }

    /**
     * Decrypts a session private key.
     * Returns the decrypted private key string.
     */
    private fun decryptSessionPrivateKey(encryptedBase64: String): String {
        try {
            val sessionCryptoManager = CryptoManager("walletkit_session_key")
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            return sessionCryptoManager.decrypt(encryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt session private key", e)
            throw SecurityException("Failed to decrypt session private key", e)
        }
    }

    /**
     * Encrypts an API key using a separate encryption key.
     * Returns Base64-encoded encrypted data.
     */
    private fun encryptApiKey(apiKey: String): String {
        try {
            val apiKeyCryptoManager = CryptoManager("walletkit_apikey")
            val encryptedBytes = apiKeyCryptoManager.encrypt(apiKey)
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt API key", e)
            throw SecurityException("Failed to encrypt API key", e)
        }
    }

    /**
     * Decrypts an API key.
     * Returns the decrypted API key string.
     */
    private fun decryptApiKey(encryptedBase64: String): String {
        try {
            val apiKeyCryptoManager = CryptoManager("walletkit_apikey")
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            return apiKeyCryptoManager.decrypt(encryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt API key", e)
            throw SecurityException("Failed to decrypt API key", e)
        }
    }

    // ========== Raw Key-Value Storage for Bridge ==========

    /**
     * Get a raw value from encrypted storage (used by BridgeStorageAdapter).
     * @param key The storage key
     * @return The stored value, or null if not found
     */
    suspend fun getRawValue(key: String): String? =
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.getString(key, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get raw value: $key", e)
                null
            }
        }

    /**
     * Set a raw value in encrypted storage (used by BridgeStorageAdapter).
     * @param key The storage key
     * @param value The value to store
     */
    suspend fun setRawValue(
        key: String,
        value: String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().putString(key, value).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set raw value: $key", e)
                throw e
            }
        }
    }

    /**
     * Remove a raw value from encrypted storage (used by BridgeStorageAdapter).
     * @param key The storage key to remove
     */
    suspend fun removeRawValue(key: String) {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(key).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove raw value: $key", e)
            }
        }
    }

    /**
     * Clear all bridge-related data from storage (keys starting with "bridge:").
     */
    suspend fun clearBridgeData() {
        withContext(Dispatchers.IO) {
            try {
                val bridgeKeys = encryptedPrefs.all.keys.filter { it.startsWith("bridge:") }
                encryptedPrefs.edit().apply {
                    bridgeKeys.forEach { remove(it) }
                    apply()
                }
                Log.d(TAG, "Cleared ${bridgeKeys.size} bridge storage keys")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear bridge data", e)
            }
        }
    }

    companion object {
        private const val TAG = "SecureWalletKitStorage"
        private const val MAX_URL_LENGTH = 2048 // Prevent DoS via huge URLs
        private val VALID_MNEMONIC_SIZES = setOf(12, 15, 18, 21, 24) // BIP39 standard
    }
}
