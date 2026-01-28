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
 * Storage interface for TON Wallet Kit.
 *
 * Implement this interface to provide custom storage for the SDK.
 *
 * The SDK stores data as JSON strings under keys like "sessions", "wallets", "preferences".
 *
 * @see TONWalletKitStorageType.Custom
 * @see WalletKitStorageException
 */
interface TONWalletKitStorage {
    /**
     * Get a value from storage.
     *
     * @param key The storage key
     * @return The stored value, or null if not found
     * @throws WalletKitStorageException if storage access fails
     */
    suspend fun get(key: String): String?

    /**
     * Save a value to storage.
     *
     * @param key The storage key
     * @param value The value to store
     * @throws WalletKitStorageException if storage write fails
     */
    suspend fun save(key: String, value: String)

    /**
     * Remove a value from storage.
     *
     * @param key The storage key to remove
     * @throws WalletKitStorageException if storage delete fails
     */
    suspend fun remove(key: String)

    /**
     * Clear all SDK-related storage data.
     *
     * @throws WalletKitStorageException if storage clear fails
     */
    suspend fun clear()
}
