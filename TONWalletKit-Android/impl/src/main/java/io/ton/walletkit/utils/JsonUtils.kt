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
package io.ton.walletkit.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JsonUtils - Safe JSON parsing and extraction utilities
 *
 * Provides null-safe methods for extracting values from JSON objects,
 * handling nested paths, and transforming arrays.
 */
object JsonUtils {

    /**
     * Safely gets a string value from JSON with a default fallback.
     */
    fun getString(json: JSONObject, key: String, default: String = ""): String {
        return try {
            if (json.has(key) && !json.isNull(key)) {
                json.getString(key)
            } else {
                default
            }
        } catch (e: JSONException) {
            default
        }
    }

    /**
     * Gets a required string value, throwing if missing or null.
     */
    fun getRequiredString(json: JSONObject, key: String): String {
        if (!json.has(key) || json.isNull(key)) {
            throw IllegalArgumentException("Required key '$key' is missing or null")
        }
        return json.getString(key)
    }

    /**
     * Safely gets an integer value from JSON with a default fallback.
     */
    fun getInt(json: JSONObject, key: String, default: Int = 0): Int {
        return try {
            if (json.has(key) && !json.isNull(key)) {
                json.getInt(key)
            } else {
                default
            }
        } catch (e: JSONException) {
            default
        }
    }

    /**
     * Safely gets a long value from JSON with a default fallback.
     */
    fun getLong(json: JSONObject, key: String, default: Long = 0L): Long {
        return try {
            if (json.has(key) && !json.isNull(key)) {
                json.getLong(key)
            } else {
                default
            }
        } catch (e: JSONException) {
            default
        }
    }

    /**
     * Safely gets a boolean value from JSON with a default fallback.
     */
    fun getBoolean(json: JSONObject, key: String, default: Boolean = false): Boolean {
        return try {
            if (json.has(key) && !json.isNull(key)) {
                json.getBoolean(key)
            } else {
                default
            }
        } catch (e: JSONException) {
            default
        }
    }

    /**
     * Safely gets a JSONObject value from JSON, returning null if missing.
     */
    fun getObject(json: JSONObject, key: String): JSONObject? {
        return try {
            if (json.has(key) && !json.isNull(key)) {
                json.getJSONObject(key)
            } else {
                null
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Safely gets a JSONArray value from JSON, returning null if missing.
     */
    fun getArray(json: JSONObject, key: String): JSONArray? {
        return try {
            if (json.has(key) && !json.isNull(key)) {
                json.getJSONArray(key)
            } else {
                null
            }
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Gets a nested string value using dot notation path (e.g., "user.profile.name").
     */
    fun getNestedString(json: JSONObject, path: String, default: String = ""): String {
        val parts = path.split(".")
        var current: JSONObject? = json

        for (i in 0 until parts.size - 1) {
            current = getObject(current ?: return default, parts[i])
        }

        return getString(current ?: return default, parts.last(), default)
    }

    /**
     * Gets a nested object using dot notation path.
     */
    fun getNestedObject(json: JSONObject, path: String): JSONObject? {
        val parts = path.split(".")
        var current: JSONObject? = json

        for (part in parts) {
            current = getObject(current ?: return null, part)
        }

        return current
    }

    /**
     * Maps a JSONArray to a list using a transformation function.
     */
    fun <T> mapArray(array: JSONArray?, transform: (JSONObject, Int) -> T): List<T> {
        if (array == null) return emptyList()

        val result = mutableListOf<T>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                result.add(transform(obj, i))
            } catch (e: JSONException) {
                // Skip invalid items
            }
        }
        return result
    }

    /**
     * Safely parses a JSON string to JSONObject, returning null on error.
     */
    fun parseObject(jsonString: String?): JSONObject? {
        if (jsonString.isNullOrBlank()) return null

        return try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Safely parses a JSON string to JSONArray, returning null on error.
     */
    fun parseArray(jsonString: String?): JSONArray? {
        if (jsonString.isNullOrBlank()) return null

        return try {
            JSONArray(jsonString)
        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Checks if a key exists and has a non-null value.
     */
    fun hasValue(json: JSONObject, key: String): Boolean {
        return json.has(key) && !json.isNull(key)
    }

    /**
     * Gets all keys from a JSONObject as a set.
     */
    fun getKeys(json: JSONObject): Set<String> {
        return json.keys().asSequence().toSet()
    }

    /**
     * Merges two JSON objects, with values from the second overriding the first.
     */
    fun merge(base: JSONObject, override: JSONObject): JSONObject {
        val result = JSONObject(base.toString())

        override.keys().forEach { key ->
            result.put(key, override.get(key))
        }

        return result
    }
}
