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

import io.ton.walletkit.demo.presentation.model.TransactionDetailUi
import io.ton.walletkit.model.Transaction
import io.ton.walletkit.model.TransactionType

/**
 * Mapper for converting domain Transaction objects to UI models.
 */
object TransactionDetailMapper {

    /**
     * Parse a domain Transaction into a TransactionDetailUi for display.
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
        tx: Transaction,
        walletAddress: String,
        unknownAddressLabel: String,
        defaultFeeLabel: String,
        successStatusLabel: String,
    ): TransactionDetailUi {
        val isOutgoing = tx.type == TransactionType.OUTGOING

        return TransactionDetailUi(
            hash = tx.hash,
            timestamp = tx.timestamp,
            amount = TonFormatter.formatNanoTon(tx.amount),
            fee = tx.fee?.let { TonFormatter.formatNanoTon(it) } ?: defaultFeeLabel,
            fromAddress = tx.sender ?: (if (isOutgoing) walletAddress else unknownAddressLabel),
            toAddress = tx.recipient ?: (if (!isOutgoing) walletAddress else unknownAddressLabel),
            comment = tx.comment,
            status = successStatusLabel, // Transactions from bridge are already filtered/successful
            lt = tx.lt ?: "0",
            blockSeqno = tx.blockSeqno ?: 0,
            isOutgoing = isOutgoing,
        )
    }
}
