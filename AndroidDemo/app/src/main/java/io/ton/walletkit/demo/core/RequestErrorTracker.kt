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
package io.ton.walletkit.demo.core

import android.util.Log

/**
 * Singleton to track RequestError events for E2E testing.
 *
 * This allows tests to verify that the SDK properly received and processed
 * error events (connect errors, transaction validation errors, etc.)
 */
object RequestErrorTracker {
    private const val TAG = "RequestErrorTracker"

    /**
     * Data class representing a captured request error.
     */
    data class CapturedError(
        val method: String,
        val errorCode: Int,
        val errorMessage: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    @Volatile
    private var lastError: CapturedError? = null

    /**
     * Record a new request error.
     * Called by ViewModel when RequestError event is received.
     */
    fun recordError(method: String, errorCode: Int, errorMessage: String) {
        val error = CapturedError(method, errorCode, errorMessage)
        lastError = error
        Log.d(TAG, "Recorded error: $error")
        Log.d(TAG, "   Method: $method | Code: $errorCode | Message: $errorMessage")
    }

    /**
     * Clear the last recorded error.
     * Call this before running a test to ensure clean state.
     */
    fun clear() {
        Log.d(TAG, "Clearing last error")
        lastError = null
    }

    /**
     * Wait for an error to be recorded (with timeout).
     * Useful in tests to wait for async error events.
     *
     * @param timeoutMs Maximum time to wait in milliseconds
     * @param method Optional method to filter by (e.g., "connect", "sendTransaction")
     * @return The captured error, or null if timeout
     */
    fun waitForError(timeoutMs: Long = 5000, method: String? = null): CapturedError? {
        Log.d(TAG, "⏳ Waiting for error event (method=$method, timeout=${timeoutMs}ms). Current error: $lastError")
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val error = lastError
            if (error != null && (method == null || error.method == method)) {
                Log.d(TAG, "Error received within timeout: $error")
                return error
            }
            Thread.sleep(100)
        }
        Log.w(TAG, "⏰ Timeout waiting for error (method=$method). lastError: $lastError")
        return null
    }
}
