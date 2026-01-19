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
 * Public storage interface for TON Wallet Kit.
 *
 * Implement this interface to provide custom storage for the SDK, enabling
 * integration with existing wallet storage systems like Tonkeeper's AppConnectEntity.
 *
 * The SDK stores the following data via this interface:
 * - Session data (session IDs, dApp info, encryption keys) under key "sessions"
 * - Wallet metadata (addresses, public keys) under key "wallets"
 * - User preferences under key "preferences"
 *
 * All values are stored as JSON strings.
 *
 * ## Example: Bridging with existing storage
 * ```kotlin
 * class TonkeeperStorage(
 *     private val dappsRepository: DAppsRepository
 * ) : TONWalletKitStorage {
 *
 *     override suspend fun get(key: String): String? {
 *         return try {
 *             when (key) {
 *                 "sessions" -> convertTonkeeperSessionsToSdkFormat()
 *                 else -> fallbackStorage.get(key)
 *             }
 *         } catch (e: Exception) {
 *             throw WalletKitStorageException(StorageOperation.GET, key, e)
 *         }
 *     }
 *
 *     override suspend fun save(key: String, value: String) {
 *         try {
 *             when (key) {
 *                 "sessions" -> {
 *                     saveToDappsRepository(value)
 *                     fallbackStorage.save(key, value)
 *                 }
 *                 else -> fallbackStorage.save(key, value)
 *             }
 *         } catch (e: Exception) {
 *             throw WalletKitStorageException(StorageOperation.SAVE, key, e)
 *         }
 *     }
 * }
 * ```
 *
 * ## Security Considerations
 * - Session data contains sensitive encryption keys - use secure storage
 * - Consider using EncryptedSharedPreferences or equivalent for the fallback
 * - The SDK's built-in encrypted storage is recommended unless you need custom integration
 *
 * @see TONWalletKitStorageType.Custom
 * @see WalletKitStorageException
 */
interface TONWalletKitStorage {
    /**
     * Get a value from storage by key.
     *
     * @param key The storage key (e.g., "sessions", "wallets", "preferences")
     * @return The stored value as a JSON string, or null if not found
     * @throws WalletKitStorageException if storage access fails
     */
    suspend fun get(key: String): String?

    /**
     * Save a value to storage.
     *
     * @param key The storage key (e.g., "sessions", "wallets", "preferences")
     * @param value The value to store as a JSON string
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
     * This should clear all keys stored by the SDK, but preserve any
     * app-specific data that may be in the same storage.
     *
     * @throws WalletKitStorageException if storage clear fails
     */
    suspend fun clear()
}
