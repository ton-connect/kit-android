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
package io.ton.walletkit.engine.infrastructure

import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Bridge serialization utilities for converting Kotlin data classes to JSONObject
 * instances used by the JavaScript bridge RPC layer.
 *
 * This centralizes the conversion pattern and ensures type-safe bridge communication.
 *
 * @suppress Internal utility for bridge operations.
 */

/**
 * Serialize any @Serializable value to JSONObject for bridge communication.
 *
 * This replaces manual JSONObject().apply { put(...) } patterns with type-safe
 * data class serialization.
 *
 * @param value The @Serializable value to encode
 * @return JSONObject ready for bridge RPC calls
 */
internal inline fun <reified T> Json.toJSONObject(value: T): JSONObject {
    return JSONObject(encodeToString(value))
}
