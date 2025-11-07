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
package io.ton.walletkit.engine.infrastructure

import io.ton.walletkit.internal.util.Logger
import io.ton.walletkit.internal.constants.LogConstants
import io.ton.walletkit.storage.BridgeStorageAdapter

/**
 * Mediates storage operations between the JavaScript bridge and the Android storage adapter.
 *
 * This component enforces the persistent storage flag and preserves legacy logging behaviour for
 * debugging. All operations must be invoked from a coroutine context; callers that need a blocking
 * API (e.g., JavaScript interfaces) should wrap the calls in `runBlocking`.
 *
 * @suppress Internal component. Use through [WebViewWalletKitEngine].
 */
internal class StorageManager(
    private val storageAdapter: BridgeStorageAdapter,
    private val isPersistentStorageEnabled: () -> Boolean,
) {
    suspend fun get(key: String): String? {
        if (!isPersistentStorageEnabled()) {
            return null
        }

        return try {
            Logger.d(TAG, "storageGet called with key=$key")
            storageAdapter.get(key)
        } catch (e: Exception) {
            Logger.e(TAG, LogConstants.MSG_STORAGE_GET_FAILED + key, e)
            null
        }
    }

    suspend fun set(key: String, value: String) {
        if (!isPersistentStorageEnabled()) {
            return
        }

        try {
            Logger.d(TAG, "storageSet called with key=$key, valueLength=${value.length}")
            storageAdapter.set(key, value)
        } catch (e: Exception) {
            Logger.e(TAG, LogConstants.MSG_STORAGE_SET_FAILED + key, e)
        }
    }

    suspend fun remove(key: String) {
        if (!isPersistentStorageEnabled()) {
            return
        }

        try {
            Logger.d(TAG, "storageRemove called with key=$key")
            storageAdapter.remove(key)
        } catch (e: Exception) {
            Logger.e(TAG, LogConstants.MSG_STORAGE_REMOVE_FAILED + key, e)
        }
    }

    suspend fun clear() {
        if (!isPersistentStorageEnabled()) {
            return
        }

        try {
            storageAdapter.clear()
        } catch (e: Exception) {
            Logger.e(TAG, LogConstants.MSG_STORAGE_CLEAR_FAILED, e)
        }
    }

    private companion object {
        private const val TAG = LogConstants.TAG_WEBVIEW_ENGINE
    }
}
