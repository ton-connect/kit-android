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
package io.ton.walletkit.internal.util

import io.ton.walletkit.WalletKitBridgeException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility functions for JSON processing in the bridge layer.
 *
 * @suppress Internal utilities for bridge operations.
 */
internal object JsonUtils {

    /**
     * Convert JavaScript Uint8Array (serialized as either JSONArray or JSONObject with indexed keys)
     * to ByteArray.
     *
     * JavaScript Uint8Arrays can be serialized as:
     * - JSONArray: [167, 80, 160, ...]
     * - JSONObject with numeric keys: {"0": 167, "1": 80, "2": 160, ...}
     *
     * This function handles both formats.
     *
     * @param json The JSON data to convert (JSONArray or JSONObject)
     * @param fieldName Name of the field being converted (for error messages)
     * @return ByteArray containing the data
     * @throws WalletKitBridgeException if the format is invalid
     */
    fun jsonToByteArray(json: Any?, fieldName: String): ByteArray {
        return when (json) {
            is JSONArray -> {
                // Standard array format: [167, 80, 160, ...]
                ByteArray(json.length()) { i -> json.optInt(i).toByte() }
            }
            is JSONObject -> {
                // Object with indexed keys: {"0": 167, "1": 80, ...}
                val length = json.length()
                ByteArray(length) { i ->
                    json.optInt(i.toString(), 0).toByte()
                }
            }
            else -> throw WalletKitBridgeException("Invalid $fieldName format in response: expected array or object")
        }
    }
}
