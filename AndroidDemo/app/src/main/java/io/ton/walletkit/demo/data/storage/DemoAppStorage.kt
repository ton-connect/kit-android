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
package io.ton.walletkit.demo.data.storage

import io.ton.walletkit.demo.domain.model.WalletInterfaceType

/**
 * Storage interface for the WalletKit demo app.
 * This is separate from the SDK's BridgeStorageAdapter and is used for
 * demo app-specific data like wallet mnemonics, metadata, and user preferences.
 *
 * NOT part of the SDK - this is demo app internal storage.
 */
interface DemoAppStorage {
    /**
     * Save a wallet record (mnemonic + metadata).
     */
    suspend fun saveWallet(address: String, record: WalletRecord)

    /**
     * Load a wallet record by address.
     */
    suspend fun loadWallet(address: String): WalletRecord?

    /**
     * Load all stored wallet records.
     */
    suspend fun loadAllWallets(): Map<String, WalletRecord>

    /**
     * Clear wallet data for a specific address.
     */
    suspend fun clear(address: String)

    /**
     * Save user preferences (active wallet, etc).
     */
    suspend fun saveUserPreferences(preferences: UserPreferences)

    /**
     * Load user preferences.
     */
    suspend fun loadUserPreferences(): UserPreferences?

    /**
     * Clear all demo app data.
     */
    suspend fun clearAll()

    // ========== Password Management ==========

    /**
     * Check if a password has been set.
     */
    fun isPasswordSet(): Boolean

    /**
     * Set/update the password (stores hash).
     */
    fun setPassword(password: String)

    /**
     * Verify if the provided password matches.
     */
    fun verifyPassword(password: String): Boolean

    /**
     * Check if the wallet is currently unlocked (in-memory only).
     */
    fun isUnlocked(): Boolean

    /**
     * Set the unlocked state (in-memory only for security).
     */
    fun setUnlocked(unlocked: Boolean)

    /**
     * Reset password data (called during wallet reset).
     */
    fun resetPassword()
}

/**
 * Wallet record stored in demo app storage.
 */
data class WalletRecord(
    val mnemonic: List<String>,
    val name: String,
    val network: String,
    val version: String,
    val createdAt: Long = System.currentTimeMillis(), // Unix timestamp in milliseconds
    val interfaceType: String = WalletInterfaceType.MNEMONIC.value, // "mnemonic" or "signer"
)

/**
 * User preferences for the demo app.
 */
data class UserPreferences(
    val activeWalletAddress: String? = null,
)
