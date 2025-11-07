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
package io.ton.walletkit.model

/**
 * Represents a blockchain transaction.
 *
 * @property hash Unique transaction hash
 * @property timestamp Transaction timestamp in milliseconds
 * @property amount Transaction amount in nanoTON
 * @property fee Transaction fee in nanoTON (nullable if not yet confirmed)
 * @property comment Optional comment/message attached to the transaction
 * @property sender Sender address (nullable for outgoing transactions)
 * @property recipient Recipient address (nullable for incoming transactions)
 * @property type Type of transaction (incoming, outgoing, or unknown)
 * @property lt Logical time - transaction ordering in the blockchain
 * @property blockSeqno Block sequence number where transaction was included
 */
data class Transaction(
    val hash: String,
    val timestamp: Long,
    val amount: String,
    val fee: String? = null,
    val comment: String? = null,
    val sender: String? = null,
    val recipient: String? = null,
    val type: TransactionType = TransactionType.UNKNOWN,
    val lt: String? = null,
    val blockSeqno: Int? = null,
)
