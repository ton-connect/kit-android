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
package io.ton.walletkit

/**
 * Exception thrown when an error occurs during WalletKit bridge operations.
 *
 * This exception is thrown when the JavaScript bridge encounters issues such as:
 * - Bridge initialization failures
 * - Invalid responses from the JavaScript layer
 * - Communication errors between Kotlin and JavaScript
 * - JavaScript runtime exceptions that propagate to the native layer
 * - Timeout errors when waiting for bridge responses
 *
 * Example scenarios:
 * ```
 * // Bridge not initialized
 * throw WalletKitBridgeException("Bridge not initialized")
 *
 * // Invalid JSON response
 * throw WalletKitBridgeException("Failed to parse bridge response: $jsonString")
 *
 * // JavaScript error
 * throw WalletKitBridgeException("JavaScript error: ${error.message}")
 * ```
 *
 * @property message A descriptive error message explaining what went wrong
 * @see io.ton.walletkit.presentation.WalletKitEngine
 */
class WalletKitBridgeException(message: String) : Exception(message)
