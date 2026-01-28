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
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory storage adapter for ephemeral sessions.
 *
 * Data is stored in a ConcurrentHashMap and lost when the app is terminated.
 * Useful for testing or when persistent storage is not desired.
 *
 * Thread-safe via ConcurrentHashMap.
 *
 * @suppress Internal implementation class.
 */
internal class MemoryBridgeStorageAdapter : BridgeStorageAdapter {
    private val storage = ConcurrentHashMap<String, String>()

    override suspend fun get(key: String): String? {
        Logger.d(TAG, "Memory storage get: key=$key")
        return storage[key]
    }

    override suspend fun set(key: String, value: String) {
        Logger.d(TAG, "Memory storage set: key=$key, valueLength=${value.length}")
        storage[key] = value
    }

    override suspend fun remove(key: String) {
        Logger.d(TAG, "Memory storage remove: key=$key")
        storage.remove(key)
    }

    override suspend fun clear() {
        Logger.d(TAG, "Memory storage clear: ${storage.size} items")
        storage.clear()
    }

    private companion object {
        private const val TAG = LogConstants.TAG_MEMORY_STORAGE
    }
}
