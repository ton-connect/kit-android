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
 * Exception thrown when a storage operation fails in WalletKit.
 *
 * This exception is thrown by [TONWalletKitStorage] implementations when:
 * - Reading from storage fails (file not found, permission denied, decryption error)
 * - Writing to storage fails (disk full, permission denied, encryption error)
 * - Removing data fails
 * - Clearing storage fails
 *
 * ## Usage in Custom Storage
 *
 * When implementing [TONWalletKitStorage], wrap platform-specific exceptions:
 *
 * ```kotlin
 * class MyCustomStorage : TONWalletKitStorage {
 *     override suspend fun get(key: String): String? {
 *         return try {
 *             sharedPrefs.getString(key, null)
 *         } catch (e: SecurityException) {
 *             throw WalletKitStorageException(
 *                 operation = StorageOperation.GET,
 *                 key = key,
 *                 message = "Security error reading storage",
 *                 cause = e
 *             )
 *         }
 *     }
 *
 *     override suspend fun save(key: String, value: String) {
 *         try {
 *             sharedPrefs.edit().putString(key, value).apply()
 *         } catch (e: IOException) {
 *             throw WalletKitStorageException(
 *                 operation = StorageOperation.SAVE,
 *                 key = key,
 *                 message = "Failed to write to storage",
 *                 cause = e
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## Handling Storage Exceptions
 *
 * ```kotlin
 * try {
 *     walletKit.connect(request)
 * } catch (e: WalletKitStorageException) {
 *     when (e.operation) {
 *         StorageOperation.GET -> // Handle read failure
 *         StorageOperation.SAVE -> // Handle write failure
 *         StorageOperation.REMOVE -> // Handle delete failure
 *         StorageOperation.CLEAR -> // Handle clear failure
 *     }
 *     Log.e("WalletKit", "Storage error for key '${e.key}': ${e.message}", e.cause)
 * }
 * ```
 *
 * @property operation The storage operation that failed
 * @property key The storage key involved (null for [StorageOperation.CLEAR])
 * @property message A descriptive error message
 * @property cause The underlying exception that caused this failure (optional)
 *
 * @see TONWalletKitStorage
 * @see StorageOperation
 */
class WalletKitStorageException(
    val operation: StorageOperation,
    val key: String? = null,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Convenience constructor for simple error messages.
     */
    constructor(
        operation: StorageOperation,
        key: String?,
        cause: Throwable,
    ) : this(
        operation = operation,
        key = key,
        message = "Storage ${operation.name.lowercase()} failed${key?.let { " for key '$it'" } ?: ""}: ${cause.message}",
        cause = cause,
    )

    override fun toString(): String {
        return "WalletKitStorageException(operation=$operation, key=$key, message=$message)"
    }
}

/**
 * Types of storage operations that can fail.
 */
enum class StorageOperation {
    /** Reading a value from storage */
    GET,

    /** Writing a value to storage */
    SAVE,

    /** Removing a value from storage */
    REMOVE,

    /** Clearing all storage data */
    CLEAR,
}
