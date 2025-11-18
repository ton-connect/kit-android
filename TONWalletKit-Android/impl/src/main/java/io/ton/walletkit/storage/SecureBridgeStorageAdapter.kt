/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit.storage

import android.content.Context
import io.ton.walletkit.exceptions.SecureStorageException
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.constants.StorageConstants
import io.ton.walletkit.internal.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Secure implementation of BridgeStorageAdapter using EncryptedSharedPreferences.
 *
 * This adapter provides secure persistent storage for the WalletKit JavaScript bridge,
 * replacing ephemeral WebView LocalStorage with Android's encrypted storage.
 *
 * Security:
 * - Uses EncryptedSharedPreferences with AES256-GCM encryption
 * - Hardware-backed master key when available
 * - All keys prefixed with "bridge:" for isolation
 *
 * Thread Safety:
 * - All operations are thread-safe via coroutine dispatchers
 * - Safe for concurrent access from multiple threads
 *
 * @suppress This is an internal implementation class.
 */
internal class SecureBridgeStorageAdapter(
    context: Context,
) : BridgeStorageAdapter {
    private val appContext = context.applicationContext
    private val secureStorage = SecureWalletKitStorage(appContext, StorageConstants.BRIDGE_STORAGE_NAME)

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        try {
            secureStorage.getRawValue(bridgeKey(key))
        } catch (e: Exception) {
            Logger.e(TAG, ERROR_FAILED_GET_RAW_VALUE + key, e)
            throw SecureStorageException.GetFailed(cause = e)
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
                Logger.e(TAG, ERROR_FAILED_SET_RAW_VALUE + key, e)
                throw SecureStorageException.SaveFailed(cause = e)
            }
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            try {
                secureStorage.removeRawValue(bridgeKey(key))
            } catch (e: Exception) {
                Logger.e(TAG, ERROR_FAILED_REMOVE_RAW_VALUE + key, e)
                throw SecureStorageException.DeleteFailed(cause = e)
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            try {
                secureStorage.clearBridgeData()
            } catch (e: Exception) {
                Logger.e(TAG, ERROR_FAILED_CLEAR_BRIDGE_DATA, e)
                throw SecureStorageException.ClearFailed(cause = e)
            }
        }
    }

    private fun bridgeKey(key: String) = StorageConstants.KEY_PREFIX_BRIDGE + key

    companion object {
        private const val TAG = LogConstants.TAG_BRIDGE_STORAGE

        // Storage Errors
        const val ERROR_FAILED_GET_RAW_VALUE = "Failed to get raw value: "
        const val ERROR_FAILED_SET_RAW_VALUE = "Failed to set raw value: "
        const val ERROR_FAILED_REMOVE_RAW_VALUE = "Failed to remove raw value: "
        const val ERROR_FAILED_CLEAR_BRIDGE_DATA = "Failed to clear bridge data"
    }
}
