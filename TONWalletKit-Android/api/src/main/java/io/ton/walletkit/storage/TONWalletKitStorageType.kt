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
 * @see TONWalletKitStorage
 * @see TONWalletKitConfiguration
 */
sealed class TONWalletKitStorageType {
    /**
     * In-memory storage only. Data is not persisted between app launches.
     */
    data object Memory : TONWalletKitStorageType()

    /**
     * Encrypted persistent storage using EncryptedSharedPreferences.
     * This is the default and recommended option for production apps.
     */
    data object Encrypted : TONWalletKitStorageType()

    /**
     * Custom storage implementation.
     *
     * Use this to integrate with existing wallet storage systems.
     *
     * @property storage Your implementation of [TONWalletKitStorage]
     */
    data class Custom(val storage: TONWalletKitStorage) : TONWalletKitStorageType()
}
