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
import org.json.JSONObject

/**
 * TransactionResponseParser - Transaction response transformation
 *
 * Handles parsing and enriching transaction responses from WalletKit,
 * including address formatting, comment extraction, and hash conversion.
 * Replaces the 267 lines of transformation logic removed from transactions.ts.
 */
object TransactionResponseParser {

    /**
     * Parses and enriches a list of transactions from the response.
     */
    fun parseTransactions(response: JSONObject?, isTestnet: Boolean = false): List<JSONObject> {
        if (response == null) return emptyList()

        val transactionsArray = JsonUtils.getArray(response, "transactions")
            ?: return emptyList()

        return (0 until transactionsArray.length()).mapNotNull { i ->
            transactionsArray.optJSONObject(i)?.let { tx ->
                enrichTransaction(tx, isTestnet)
            }
        }
    }

    /**
     * Enriches a single transaction with formatted addresses and extracted comments.
     */
    fun enrichTransaction(tx: JSONObject, isTestnet: Boolean = false): JSONObject {
        // Create a copy to avoid modifying the original
        val enriched = JSONObject(tx.toString())

        // Convert hash to hex if present
        val hash = JsonUtils.getString(tx, "hash")
        if (hash.isNotBlank() && !JsonUtils.hasValue(tx, "hash_hex")) {
            val hashHex = AddressTransformer.base64ToHex(hash)
            if (hashHex != null) {
                enriched.put("hash_hex", hashHex)
            }
        }

        // Enrich in_msg if present
        val inMsg = JsonUtils.getObject(tx, "in_msg")
        if (inMsg != null) {
            enrichInMessage(inMsg, isTestnet)
            enriched.put("in_msg", inMsg)
        }

        // Enrich out_msgs array if present
        val outMsgs = JsonUtils.getArray(tx, "out_msgs")
        if (outMsgs != null) {
            val enrichedOutMsgs = JSONArray()
            for (i in 0 until outMsgs.length()) {
                try {
                    val msg = outMsgs.getJSONObject(i)
                    enrichMessage(msg, isTestnet)
                    enrichedOutMsgs.put(msg)
                } catch (e: Exception) {
                    // Skip invalid messages
                }
            }
            enriched.put("out_msgs", enrichedOutMsgs)
        }

        return enriched
    }

    /**
     * Enriches an incoming message with user-friendly addresses and comment.
     */
    private fun enrichInMessage(msg: JSONObject, isTestnet: Boolean) {
        enrichMessage(msg, isTestnet)

        // Extract comment from message body if present
        val messageContent = JsonUtils.getObject(msg, "message_content")
        if (messageContent != null) {
            val body = JsonUtils.getString(messageContent, "body")
            if (body.isNotBlank()) {
                val comment = extractTextComment(body)
                if (comment != null) {
                    msg.put("comment", comment)
                }
            }
        }
    }

    /**
     * Enriches a message with user-friendly addresses.
     */
    private fun enrichMessage(msg: JSONObject, isTestnet: Boolean) {
        // Convert source address
        val source = JsonUtils.getString(msg, "source")
        if (source.isNotBlank()) {
            val sourceFriendly = AddressTransformer.toUserFriendly(source, isTestnet)
            if (sourceFriendly != null) {
                msg.put("source_friendly", sourceFriendly)
            }
        }

        // Convert destination address
        val destination = JsonUtils.getString(msg, "destination")
        if (destination.isNotBlank()) {
            val destinationFriendly = AddressTransformer.toUserFriendly(destination, isTestnet)
            if (destinationFriendly != null) {
                msg.put("destination_friendly", destinationFriendly)
            }
        }

        // Extract comment from message content if present
        val messageContent = JsonUtils.getObject(msg, "message_content")
        if (messageContent != null) {
            val body = JsonUtils.getString(messageContent, "body")
            if (body.isNotBlank()) {
                val comment = extractTextComment(body)
                if (comment != null) {
                    msg.put("comment", comment)
                }
            }
        }
    }

    /**
     * Extracts a text comment from a message body cell.
     *
     * TON messages can contain text comments encoded in the cell body.
     * Text comments start with opcode 0x00000000 followed by UTF-8 text.
     */
    fun extractTextComment(body: String?): String? {
        if (body.isNullOrBlank()) return null

        try {
            // If body is base64, decode it
            val bytes = if (body.matches(Regex("^[0-9a-zA-Z+/=]+$"))) {
                android.util.Base64.decode(body, android.util.Base64.NO_WRAP)
            } else {
                body.toByteArray()
            }

            // Check for text comment opcode (0x00000000)
            if (bytes.size >= 4) {
                val opcode = ((bytes[0].toInt() and 0xFF) shl 24) or
                    ((bytes[1].toInt() and 0xFF) shl 16) or
                    ((bytes[2].toInt() and 0xFF) shl 8) or
                    (bytes[3].toInt() and 0xFF)

                if (opcode == 0) {
                    // Text comment - extract UTF-8 text after the opcode
                    val text = String(bytes, 4, bytes.size - 4, Charsets.UTF_8)
                    return text.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            // Failed to extract comment
        }

        return null
    }

    /**
     * Parses a transaction preview response.
     */
    fun parseTransactionPreview(response: JSONObject?): JSONObject? {
        if (response == null) return null

        // Preview might be nested in a "preview" field
        val preview = JsonUtils.getObject(response, "preview")
        return preview ?: response
    }
}
