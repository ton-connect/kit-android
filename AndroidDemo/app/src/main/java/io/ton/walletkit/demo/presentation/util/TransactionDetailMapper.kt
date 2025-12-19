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
package io.ton.walletkit.demo.presentation.util

import io.ton.walletkit.api.generated.TONTransaction
import io.ton.walletkit.demo.presentation.model.TransactionDetailUi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mapper for converting domain TONTransaction objects to UI models.
 *
 * Note: The new TONTransaction structure is significantly different from the old Transaction model.
 * Fields like sender, recipient, amount, type are now embedded in inMessage/outMessages.
 * This mapper provides basic functionality with the available fields.
 */
object TransactionDetailMapper {

    /**
     * Attempts to extract text content from a decoded JsonObject.
     * Looks for common fields like "text", "comment", or "message".
     */
    private fun extractDecodedText(decoded: JsonObject?): String? {
        if (decoded == null) return null

        // Try common field names for text content
        val textFields = listOf("text", "comment", "message")
        for (field in textFields) {
            val value = decoded[field]
            if (value is JsonPrimitive && value.isString) {
                return value.content
            }
        }
        return null
    }

    /**
     * Parse a domain TONTransaction into a TransactionDetailUi for display.
     *
     * @param tx The transaction to parse
     * @param walletAddress The wallet address (used for inferring sender/recipient)
     * @param unknownAddressLabel Label to use for unknown addresses
     * @param defaultFeeLabel Label to use when fee is unavailable
     * @param successStatusLabel Status label for successful transactions
     *
     * @return TransactionDetailUi ready for display
     */
    fun toDetailUi(
        tx: TONTransaction,
        walletAddress: String,
        unknownAddressLabel: String,
        defaultFeeLabel: String,
        successStatusLabel: String,
    ): TransactionDetailUi {
        // Extract hash as string
        val hashStr = tx.hash?.toString() ?: tx.logicalTime

        // Determine direction based on account (wallet is the main account)
        // If in message exists, it's likely incoming; if out messages exist, likely outgoing
        val hasInMessage = tx.inMessage != null
        val hasOutMessages = tx.outMessages.isNotEmpty()
        val isOutgoing = hasOutMessages && !hasInMessage

        // Try to extract amount and addresses from messages
        val inMessageValue = tx.inMessage?.value
        val outMessageValue = tx.outMessages.firstOrNull()?.value

        // Primary amount comes from the most relevant message
        val amount = if (isOutgoing) {
            outMessageValue ?: "0"
        } else {
            inMessageValue ?: "0"
        }

        // Get sender and recipient from messages
        val fromAddress = if (isOutgoing) {
            walletAddress
        } else {
            tx.inMessage?.source?.value ?: unknownAddressLabel
        }

        val toAddress = if (isOutgoing) {
            tx.outMessages.firstOrNull()?.destination?.value ?: unknownAddressLabel
        } else {
            walletAddress
        }

        // Extract comment from message content decoded field if available
        // The decoded field is a JsonObject that may contain a "text" field
        val comment = extractDecodedText(tx.inMessage?.messageContent?.decoded)
            ?: extractDecodedText(tx.outMessages.firstOrNull()?.messageContent?.decoded)

        return TransactionDetailUi(
            hash = hashStr,
            timestamp = tx.now.toLong(),
            amount = TonFormatter.formatNanoTon(amount),
            fee = tx.totalFees?.let { TonFormatter.formatNanoTon(it) } ?: defaultFeeLabel,
            fromAddress = fromAddress,
            toAddress = toAddress,
            comment = comment,
            status = successStatusLabel, // Transactions from bridge are already filtered/successful
            lt = tx.logicalTime,
            blockSeqno = tx.mcBlockSeqno,
            isOutgoing = isOutgoing,
        )
    }
}
