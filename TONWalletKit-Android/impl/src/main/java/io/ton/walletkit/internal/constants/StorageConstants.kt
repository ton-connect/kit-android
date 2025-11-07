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
package io.ton.walletkit.internal.constants

/**
 * Constants for storage keys and prefixes used throughout the SDK.
 *
 * These constants ensure consistent key naming across storage implementations
 * and make refactoring safer by avoiding hardcoded string literals.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object StorageConstants {
    /**
     * Prefix for wallet-related storage keys.
     *
     * Format: "wallet:{accountId}"
     */
    const val KEY_PREFIX_WALLET = "wallet:"

    /**
     * Prefix for bridge-related storage keys (used by JavaScript bundle).
     *
     * Format: "bridge:{key}"
     */
    const val KEY_PREFIX_BRIDGE = "bridge:"

    /**
     * Prefix for session-related storage keys.
     *
     * Format: "session:{sessionId}"
     */
    const val KEY_PREFIX_SESSION = "session:"

    /**
     * Prefix for configuration storage keys.
     *
     * Format: "config:{configId}"
     */
    const val KEY_PREFIX_CONFIG = "config:"

    /**
     * Prefix for user preferences storage keys.
     *
     * Format: "preferences:{prefKey}"
     */
    const val KEY_PREFIX_PREFERENCES = "preferences:"

    /**
     * Prefix for pending events storage keys (automatic retry mechanism).
     *
     * Format: "pending_event:{eventId}"
     */
    const val KEY_PREFIX_PENDING_EVENT = "pending_event:"

    /**
     * Default name for secure storage SharedPreferences file.
     */
    const val DEFAULT_SECURE_STORAGE_NAME = "walletkit_secure_storage"

    /**
     * Name for bridge storage SharedPreferences file.
     */
    const val BRIDGE_STORAGE_NAME = "walletkit_bridge_storage"

    /**
     * Keystore alias for mnemonic encryption.
     */
    const val MNEMONIC_KEYSTORE_KEY = "walletkit_mnemonic_key"

    /**
     * Keystore alias for session private key encryption.
     */
    const val SESSION_KEYSTORE_KEY = "walletkit_session_key"

    /**
     * Keystore alias for API key encryption.
     */
    const val API_KEY_KEYSTORE_KEY = "walletkit_apikey"

    /**
     * Storage key for bridge configuration.
     */
    const val BRIDGE_CONFIG_KEY = "bridge_config"

    /**
     * Storage key for user preferences.
     */
    const val USER_PREFERENCES_KEY = "user_preferences"
}
