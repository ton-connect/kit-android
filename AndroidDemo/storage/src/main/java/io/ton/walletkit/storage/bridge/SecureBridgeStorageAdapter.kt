package io.ton.walletkit.storage.bridge

import android.content.Context
import android.util.Log
import io.ton.walletkit.storage.impl.SecureWalletKitStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Secure implementation of BridgeStorageAdapter using EncryptedSharedPreferences.
 * This adapter provides the bridge with access to Android secure storage, enabling
 * the JavaScript bundle to persist wallet and session data between app restarts.
 *
 * All data is encrypted at rest using Android Keystore-backed encryption.
 *
 * Storage Keys:
 * - "bridge:*" - Raw key-value pairs from JavaScript bundle
 * - Automatically encrypted by EncryptedSharedPreferences
 *
 * @param context Application context
 */
class SecureBridgeStorageAdapter(
    context: Context,
) : BridgeStorageAdapter {
    private val appContext = context.applicationContext
    private val secureStorage = SecureWalletKitStorage(appContext, "walletkit_bridge_storage")

    override suspend fun get(key: String): String? =
        withContext(Dispatchers.IO) {
            try {
                secureStorage.getRawValue(bridgeKey(key))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get value for key: $key", e)
                null
            }
        }

    override suspend fun set(
        key: String,
        value: String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                secureStorage.setRawValue(bridgeKey(key), value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set value for key: $key", e)
                throw e
            }
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            try {
                secureStorage.removeRawValue(bridgeKey(key))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove key: $key", e)
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            try {
                secureStorage.clearBridgeData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear bridge storage", e)
            }
        }
    }

    private fun bridgeKey(key: String) = "bridge:$key"

    private companion object {
        private const val TAG = "SecureBridgeStorageAdapter"
    }
}
