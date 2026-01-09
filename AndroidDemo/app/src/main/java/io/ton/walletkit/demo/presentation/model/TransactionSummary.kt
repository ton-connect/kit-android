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
package io.ton.walletkit.demo.presentation.model

import io.ton.walletkit.api.generated.TONTransaction

/**
 * Simplified transaction summary for UI display.
 */
data class TransactionSummary(
    val id: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val amount: String,
    val hash: String,
)

/**
 * Determines whether a transaction is outgoing by comparing the account address
 * with the wallet address.
 */
fun TONTransaction.isOutgoing(walletAddress: String): Boolean {
    // If the transaction account matches our wallet, check if we're the sender
    // For outgoing transactions, we initiate the transaction
    val accountAddress = account.value

    // If the transaction is from our account and has outgoing messages, it's outgoing
    return accountAddress.equals(walletAddress, ignoreCase = true) && outMessages.isNotEmpty()
}

/**
 * Gets the primary amount from a transaction (in nanoTON).
 */
fun TONTransaction.getPrimaryAmount(): String {
    // For outgoing: sum of outgoing message values
    // For incoming: value from inMessage
    val outAmount = outMessages.sumOf {
        it.value?.toBigDecimalOrNull()?.toLong() ?: 0L
    }
    val inAmount = inMessage?.value?.toLongOrNull() ?: 0L

    return if (outAmount > 0) outAmount.toString() else inAmount.toString()
}

/**
 * Gets the unique identifier for a transaction.
 */
fun TONTransaction.getUniqueId(): String = hash?.toString() ?: logicalTime

/**
 * Gets the display hash for a transaction (truncated if from JsonObject).
 */
fun TONTransaction.getDisplayHash(): String {
    val hashStr = hash?.toString() ?: logicalTime
    // Clean up JsonObject representation if needed
    return hashStr.removePrefix("{").removeSuffix("}").take(32)
}

/**
 * Converts a TONTransaction to a TransactionSummary for UI display.
 */
fun TONTransaction.toSummary(walletAddress: String): TransactionSummary {
    val isOut = isOutgoing(walletAddress)
    return TransactionSummary(
        id = getUniqueId(),
        timestamp = now.toLong() * 1000, // Convert Unix timestamp to milliseconds
        isOutgoing = isOut,
        amount = getPrimaryAmount(),
        hash = getDisplayHash(),
    )
}
