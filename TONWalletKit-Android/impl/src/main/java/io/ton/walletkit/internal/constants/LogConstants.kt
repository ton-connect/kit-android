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
 * Constants for logging tags used throughout the SDK.
 *
 * These constants provide consistent logging tags for different components,
 * making it easier to filter and search logs during development and debugging.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object LogConstants {
    /**
     * Log tag for SecureWalletKitStorage class.
     */
    const val TAG_SECURE_STORAGE = "SecureWalletKitStorage"

    /**
     * Log tag for SecureBridgeStorageAdapter class.
     */
    const val TAG_BRIDGE_STORAGE = "SecureBridgeStorageAdapter"

    /**
     * Log tag for WebViewWalletKitEngine class.
     */
    const val TAG_WEBVIEW_ENGINE = "WebViewWalletKitEngine"

    /**
     * Log tag for CustomBridgeStorageAdapter class.
     */
    const val TAG_CUSTOM_STORAGE = "CustomBridgeStorageAdapter"

    /**
     * Log tag for MemoryBridgeStorageAdapter class.
     */
    const val TAG_MEMORY_STORAGE = "MemoryBridgeStorageAdapter"

    // Log messages
    /**
     * Log message prefix for malformed payload from JavaScript.
     */
    const val MSG_MALFORMED_PAYLOAD = "Malformed payload from JS"

    /**
     * Error message prefix for malformed payloads.
     */
    const val ERROR_MALFORMED_PAYLOAD_PREFIX = "Malformed payload: "

    /**
     * Log message prefix for storage get failures.
     */
    const val MSG_STORAGE_GET_FAILED = "Storage get failed for key: "

    /**
     * Log message prefix for storage set failures.
     */
    const val MSG_STORAGE_SET_FAILED = "Storage set failed for key: "

    /**
     * Log message prefix for storage remove failures.
     */
    const val MSG_STORAGE_REMOVE_FAILED = "Storage remove failed for key: "

    /**
     * Log message for storage clear failures.
     */
    const val MSG_STORAGE_CLEAR_FAILED = "Storage clear failed"
}
