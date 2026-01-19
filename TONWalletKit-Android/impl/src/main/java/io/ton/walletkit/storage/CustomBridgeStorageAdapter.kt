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

import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.internal.util.Logger

/**
 * Adapter that wraps a public [TONWalletKitStorage] implementation to conform to
 * the internal [BridgeStorageAdapter] interface.
 *
 * This enables custom storage implementations provided by partners (like Tonkeeper)
 * to be used with the SDK's internal storage system.
 *
 * Keys are passed directly to the custom storage without modification,
 * matching the behavior of the iOS SDK.
 *
 * Exceptions from the custom storage are wrapped in [WalletKitStorageException]
 * if they aren't already.
 *
 * @suppress Internal implementation class.
 */
internal class CustomBridgeStorageAdapter(
    private val customStorage: TONWalletKitStorage,
) : BridgeStorageAdapter {

    override suspend fun get(key: String): String? {
        return try {
            Logger.d(TAG, "Custom storage get: key=$key")
            customStorage.get(key)
        } catch (e: WalletKitStorageException) {
            Logger.e(TAG, "Custom storage get failed for key=$key", e)
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Custom storage get failed for key=$key", e)
            throw WalletKitStorageException(StorageOperation.GET, key, e)
        }
    }

    override suspend fun set(key: String, value: String) {
        try {
            Logger.d(TAG, "Custom storage save: key=$key, valueLength=${value.length}")
            customStorage.save(key, value)
        } catch (e: WalletKitStorageException) {
            Logger.e(TAG, "Custom storage save failed for key=$key", e)
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Custom storage save failed for key=$key", e)
            throw WalletKitStorageException(StorageOperation.SAVE, key, e)
        }
    }

    override suspend fun remove(key: String) {
        try {
            Logger.d(TAG, "Custom storage remove: key=$key")
            customStorage.remove(key)
        } catch (e: WalletKitStorageException) {
            Logger.e(TAG, "Custom storage remove failed for key=$key", e)
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Custom storage remove failed for key=$key", e)
            throw WalletKitStorageException(StorageOperation.REMOVE, key, e)
        }
    }

    override suspend fun clear() {
        try {
            Logger.d(TAG, "Custom storage clear")
            customStorage.clear()
        } catch (e: WalletKitStorageException) {
            Logger.e(TAG, "Custom storage clear failed", e)
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Custom storage clear failed", e)
            throw WalletKitStorageException(StorageOperation.CLEAR, null, e)
        }
    }

    private companion object {
        private const val TAG = LogConstants.TAG_CUSTOM_STORAGE
    }
}
