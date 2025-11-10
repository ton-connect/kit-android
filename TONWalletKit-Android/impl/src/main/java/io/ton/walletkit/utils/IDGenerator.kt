/**
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
package io.ton.walletkit.utils

import java.util.UUID

/**
 * IDGenerator - Unique identifier generation utilities
 *
 * Provides cryptographically secure ID generation using UUID.
 * Replaces Date.now() + Math.random() pattern from JavaScript bridge.
 */
object IDGenerator {
    /**
     * Generates a unique signer ID.
     *
     * @return Signer ID in format "signer_UUID"
     */
    fun generateSignerId(): String {
        return "signer_${UUID.randomUUID()}"
    }

    /**
     * Generates a unique adapter ID.
     *
     * @return Adapter ID in format "adapter_UUID"
     */
    fun generateAdapterId(): String {
        return "adapter_${UUID.randomUUID()}"
    }

    /**
     * Generates a unique ID with a custom prefix.
     *
     * @param prefix The prefix for the ID
     * @return ID in format "prefix_UUID"
     */
    fun generateId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID()}"
    }
}
