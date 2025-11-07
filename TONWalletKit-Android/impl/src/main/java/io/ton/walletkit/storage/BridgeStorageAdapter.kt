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

/**
 * Storage adapter interface for the WalletKit bridge to persist data between app restarts.
 * This enables the JavaScript bundle to use Android secure storage instead of ephemeral
 * WebView LocalStorage or memory storage.
 *
 * The bridge calls these methods via JavascriptInterface to persist:
 * - Wallet metadata (addresses, public keys, versions)
 * - Session data (session IDs, dApp info, private keys)
 * - User preferences (active wallet, network settings)
 *
 * Implementations should use secure storage (e.g., EncryptedSharedPreferences) for
 * sensitive data like session private keys.
 *
 * @suppress This is an internal interface. Partners should not implement or use this directly.
 */
internal interface BridgeStorageAdapter {
    /**
     * Get a value from storage by key.
     * @param key The storage key
     * @return The stored value as a JSON string, or null if not found
     */
    suspend fun get(key: String): String?

    /**
     * Set a value in storage.
     * @param key The storage key
     * @param value The value to store as a JSON string
     */
    suspend fun set(key: String, value: String)

    /**
     * Remove a value from storage.
     * @param key The storage key to remove
     */
    suspend fun remove(key: String)

    /**
     * Clear all storage data.
     */
    suspend fun clear()
}
