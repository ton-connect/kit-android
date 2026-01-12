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

import androidx.recyclerview.widget.DiffUtil
import io.ton.walletkit.api.generated.TONTransaction

private const val CHANGE_TIMESTAMP = "timestamp"
private const val CHANGE_FEES = "fees"
private const val CHANGE_BLOCK_SEQNO = "blockSeqno"

/**
 * DiffUtil callback for efficiently calculating differences between transaction lists.
 * Uses transaction hash (as string) or logical time as the unique identifier.
 */
class TransactionDiffCallback(
    private val oldList: List<TONTransaction>,
    private val newList: List<TONTransaction>,
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    /**
     * Get unique identifier for a transaction.
     * Uses hash (as string) if available, otherwise falls back to logicalTime.
     */
    private fun TONTransaction.uniqueId(): String = hash?.toString() ?: logicalTime

    /**
     * Check if items represent the same transaction (same hash).
     */
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.uniqueId() == newItem.uniqueId()
    }

    /**
     * Check if the contents of the transaction are the same.
     * This is called only when areItemsTheSame returns true.
     */
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        // Compare all fields that might change
        return oldItem.uniqueId() == newItem.uniqueId() &&
            oldItem.now == newItem.now &&
            oldItem.logicalTime == newItem.logicalTime &&
            oldItem.mcBlockSeqno == newItem.mcBlockSeqno &&
            oldItem.totalFees == newItem.totalFees &&
            oldItem.account.value == newItem.account.value
    }

    /**
     * Get the payload of changes between old and new items.
     * This can be used for partial updates in the UI.
     */
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        // Create a list of changed fields
        val changes = mutableListOf<String>()

        if (oldItem.now != newItem.now) changes.add(CHANGE_TIMESTAMP)
        if (oldItem.totalFees != newItem.totalFees) changes.add(CHANGE_FEES)
        if (oldItem.mcBlockSeqno != newItem.mcBlockSeqno) changes.add(CHANGE_BLOCK_SEQNO)

        return if (changes.isNotEmpty()) changes else null
    }
}

/**
 * Helper object for calculating transaction list diffs.
 */
object TransactionDiffUtil {
    /**
     * Get unique identifier for a transaction.
     */
    private fun TONTransaction.uniqueId(): String = hash?.toString() ?: logicalTime

    /**
     * Calculate the difference between two transaction lists.
     * Returns a DiffUtil.DiffResult that can be used to update UI efficiently.
     */
    fun calculateDiff(
        oldList: List<TONTransaction>,
        newList: List<TONTransaction>,
        detectMoves: Boolean = true,
    ): DiffUtil.DiffResult {
        val callback = TransactionDiffCallback(oldList, newList)
        return DiffUtil.calculateDiff(callback, detectMoves)
    }

    /**
     * Check if two transaction lists are effectively the same.
     * This is a quick check based on size and hash values.
     */
    fun areListsEqual(
        oldList: List<TONTransaction>,
        newList: List<TONTransaction>,
    ): Boolean {
        if (oldList.size != newList.size) return false

        // Quick hash-based comparison
        val oldHashes = oldList.map { it.uniqueId() }.toSet()
        val newHashes = newList.map { it.uniqueId() }.toSet()

        return oldHashes == newHashes
    }

    /**
     * Get new transactions (transactions in newList but not in oldList).
     */
    fun getNewTransactions(
        oldList: List<TONTransaction>,
        newList: List<TONTransaction>,
    ): List<TONTransaction> {
        val oldHashes = oldList.map { it.uniqueId() }.toSet()
        return newList.filter { it.uniqueId() !in oldHashes }
    }

    /**
     * Get removed transactions (transactions in oldList but not in newList).
     */
    fun getRemovedTransactions(
        oldList: List<TONTransaction>,
        newList: List<TONTransaction>,
    ): List<TONTransaction> {
        val newHashes = newList.map { it.uniqueId() }.toSet()
        return oldList.filter { it.uniqueId() !in newHashes }
    }

    /**
     * Merge two transaction lists, preferring newer data.
     * Deduplicates by hash and sorts by timestamp descending.
     */
    fun mergeLists(
        oldList: List<TONTransaction>,
        newList: List<TONTransaction>,
        maxSize: Int = 100,
    ): List<TONTransaction> {
        val transactionMap = mutableMapOf<String, TONTransaction>()

        // Add old transactions first
        oldList.forEach { tx ->
            transactionMap[tx.uniqueId()] = tx
        }

        // Add/update with new transactions (overwrites existing)
        newList.forEach { tx ->
            transactionMap[tx.uniqueId()] = tx
        }

        // Sort by timestamp descending and limit
        return transactionMap.values
            .sortedByDescending { it.now }
            .take(maxSize)
    }
}
