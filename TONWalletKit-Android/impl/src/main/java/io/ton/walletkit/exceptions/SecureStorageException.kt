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
package io.ton.walletkit.exceptions

/**
 * Exception thrown when secure storage operations fail.
 *
 * This sealed class represents failures in secure storage operations on Android, such as
 * EncryptedSharedPreferences, KeyStore, or other secure storage mechanisms. It is the
 * Android equivalent of iOS SDK's `KeychainError`.
 *
 * Secure storage is used to store sensitive data such as:
 * - Private keys and mnemonics
 * - Session tokens
 * - Wallet configuration
 * - User preferences
 *
 * This class provides specific error types for different storage operation failures,
 * making it easier to handle and diagnose storage-related issues.
 *
 * @see io.ton.walletkit.storage.WalletKitStorage
 */
internal sealed class SecureStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Failed to save data to secure storage.
     *
     * Example:
     * ```kotlin
     * try {
     *     encryptedPrefs.edit().putString(key, value).apply()
     * } catch (e: Exception) {
     *     throw SecureStorageException.SaveFailed(cause = e)
     * }
     * ```
     *
     * Common causes:
     * - KeyStore initialization failure
     * - Encryption error
     * - Storage quota exceeded
     * - Permission denied
     *
     * @param cause The underlying exception that caused the save failure
     */
    class SaveFailed(
        cause: Throwable? = null,
    ) : SecureStorageException("Failed to save to secure storage", cause)

    /**
     * Failed to retrieve data from secure storage.
     *
     * Example:
     * ```kotlin
     * try {
     *     val value = encryptedPrefs.getString(key, null)
     * } catch (e: Exception) {
     *     throw SecureStorageException.GetFailed(cause = e)
     * }
     * ```
     *
     * Common causes:
     * - Decryption error
     * - KeyStore key unavailable
     * - Corrupted storage
     * - Permission denied
     *
     * @param cause The underlying exception that caused the get failure
     */
    class GetFailed(
        cause: Throwable? = null,
    ) : SecureStorageException("Failed to get from secure storage", cause)

    /**
     * Failed to delete data from secure storage.
     *
     * Example:
     * ```kotlin
     * try {
     *     encryptedPrefs.edit().remove(key).apply()
     * } catch (e: Exception) {
     *     throw SecureStorageException.DeleteFailed(cause = e)
     * }
     * ```
     *
     * Common causes:
     * - Storage locked
     * - Permission denied
     * - I/O error
     *
     * @param cause The underlying exception that caused the delete failure
     */
    class DeleteFailed(
        cause: Throwable? = null,
    ) : SecureStorageException("Failed to delete from secure storage", cause)

    /**
     * Failed to clear all data from secure storage.
     *
     * Example:
     * ```kotlin
     * try {
     *     encryptedPrefs.edit().clear().apply()
     * } catch (e: Exception) {
     *     throw SecureStorageException.ClearFailed(cause = e)
     * }
     * ```
     *
     * Common causes:
     * - Storage locked
     * - Permission denied
     * - I/O error
     *
     * @param cause The underlying exception that caused the clear failure
     */
    class ClearFailed(
        cause: Throwable? = null,
    ) : SecureStorageException("Failed to clear secure storage", cause)

    /**
     * Retrieved data from secure storage is invalid or corrupted.
     *
     * Example:
     * ```kotlin
     * val encrypted = encryptedPrefs.getString(key, null)
     * if (encrypted == null || encrypted.isBlank()) {
     *     throw SecureStorageException.InvalidData()
     * }
     * ```
     *
     * Common causes:
     * - Corrupted encrypted data
     * - Invalid base64 encoding
     * - Incomplete data write
     * - Version mismatch
     */
    class InvalidData : SecureStorageException("Invalid data retrieved from secure storage")
}
