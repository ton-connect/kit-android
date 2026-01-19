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
 * Storage type configuration for TON Wallet Kit.
 *
 * Specifies where the SDK should persist its data (sessions, wallets, preferences).
 * This mirrors the iOS SDK's TONWalletKitStorageType for API parity.
 *
 * ## Default Behavior
 * If not specified in configuration, the SDK uses [Encrypted] storage which provides
 * AES-256-GCM encryption backed by Android Keystore.
 *
 * ## Available Options
 *
 * ### Memory Storage
 * Data is kept only in memory and lost when the app is terminated.
 * Useful for testing or ephemeral sessions.
 * ```kotlin
 * TONWalletKitStorageType.Memory
 * ```
 *
 * ### Encrypted Storage (Default)
 * Uses EncryptedSharedPreferences with hardware-backed encryption when available.
 * Recommended for production use.
 * ```kotlin
 * TONWalletKitStorageType.Encrypted
 * ```
 *
 * ### Custom Storage
 * Provide your own storage implementation to integrate with existing systems.
 * Perfect for wallets like Tonkeeper that have their own session storage.
 * ```kotlin
 * TONWalletKitStorageType.Custom(myStorageImplementation)
 * ```
 *
 * ## Example: Configuration with custom storage
 * ```kotlin
 * val customStorage = MyTonkeeperStorage(dappsRepository)
 *
 * val config = TONWalletKitConfiguration(
 *     network = TONNetwork.MAINNET,
 *     walletManifest = manifest,
 *     bridge = bridge,
 *     features = features,
 *     storageType = TONWalletKitStorageType.Custom(customStorage)
 * )
 *
 * val walletKit = ITONWalletKit.initialize(context, config)
 * ```
 *
 * @see TONWalletKitStorage
 * @see TONWalletKitConfiguration
 */
sealed class TONWalletKitStorageType {
    /**
     * In-memory storage only.
     *
     * Data is not persisted between app launches. Useful for:
     * - Testing scenarios
     * - Ephemeral sessions that shouldn't persist
     * - Privacy-focused configurations
     *
     * Note: Sessions will be lost when the app is terminated.
     */
    data object Memory : TONWalletKitStorageType()

    /**
     * Encrypted persistent storage using EncryptedSharedPreferences.
     *
     * This is the recommended default for production apps. Features:
     * - AES-256-GCM encryption for values
     * - AES-256-SIV encryption for keys
     * - Hardware-backed master key (StrongBox) when available
     * - Automatic key rotation and migration
     *
     * Data is stored in a dedicated encrypted SharedPreferences file
     * isolated from other app data.
     */
    data object Encrypted : TONWalletKitStorageType()

    /**
     * Custom storage implementation.
     *
     * Use this to integrate with existing wallet storage systems like:
     * - Tonkeeper's DAppsRepository and AppConnectEntity
     * - Custom encrypted databases
     * - Cross-platform storage solutions
     *
     * The custom storage receives keys prefixed with "bridge:" (e.g., "bridge:sessions").
     *
     * @property storage Your implementation of [TONWalletKitStorage]
     *
     * ## Example
     * ```kotlin
     * class TonkeeperStorage(
     *     private val dappsRepository: DAppsRepository
     * ) : TONWalletKitStorage {
     *     override suspend fun get(key: String): String? {
     *         // Bridge existing sessions to SDK format
     *     }
     *     override suspend fun set(key: String, value: String) {
     *         // Store in both Tonkeeper DB and SDK
     *     }
     *     override suspend fun remove(key: String) { /* ... */ }
     *     override suspend fun clear() { /* ... */ }
     * }
     *
     * val storageType = TONWalletKitStorageType.Custom(TonkeeperStorage(repo))
     * ```
     */
    data class Custom(val storage: TONWalletKitStorage) : TONWalletKitStorageType()
}
