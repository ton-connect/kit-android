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
package io.ton.walletkit.demo.data.cache

import android.util.Log
import io.ton.walletkit.api.generated.TONTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache for wallet transactions with deduplication and merging capabilities.
 * Uses transaction hash as unique identifier.
 */
class TransactionCache {
    private val mutex = Mutex()

    // Map of wallet address -> list of transactions (ordered by timestamp desc)
    private val cache = mutableMapOf<String, List<TONTransaction>>()

    // Track last update time for each wallet
    private val lastUpdateTime = mutableMapOf<String, Long>()

    /**
     * Get cached transactions for a wallet address.
     * Returns null if no cache exists for this wallet.
     */
    suspend fun get(walletAddress: String): List<TONTransaction>? = mutex.withLock {
        cache[walletAddress]
    }

    /**
     * Update cache with new transactions, merging with existing ones.
     * New transactions are deduplicated by hash and merged with existing cache.
     * The list is sorted by timestamp (descending) and limited to maxSize.
     *
     * @param walletAddress The wallet address
     * @param newTransactions The new transactions to add/update
     * @param maxSize Maximum number of transactions to keep in cache (default 100)
     * @return The merged and sorted transaction list
     */
    suspend fun update(
        walletAddress: String,
        newTransactions: List<TONTransaction>,
        maxSize: Int = 100,
    ): List<TONTransaction> = mutex.withLock {
        val existing = cache[walletAddress] ?: emptyList()

        // Create a map of hash -> transaction for deduplication
        // New transactions take priority over existing ones (in case of updates)
        val transactionMap = mutableMapOf<String, TONTransaction>()

        // Add existing transactions first
        existing.forEach { tx ->
            val hashStr = tx.hash?.toString() ?: tx.logicalTime
            transactionMap[hashStr] = tx
        }

        // Add/update with new transactions (overwrites existing)
        newTransactions.forEach { tx ->
            val hashStr = tx.hash?.toString() ?: tx.logicalTime
            transactionMap[hashStr] = tx
        }

        // Convert back to list, sort by timestamp (descending), and limit size
        val merged = transactionMap.values
            .sortedByDescending { it.now }
            .take(maxSize)

        // Update cache
        cache[walletAddress] = merged
        lastUpdateTime[walletAddress] = System.currentTimeMillis()

        Log.d(
            TAG,
            "TransactionCache updated for $walletAddress: " +
                "${existing.size} existing + ${newTransactions.size} new = ${merged.size} total " +
                "(${newTransactions.size - (merged.size - existing.size)} duplicates)",
        )

        merged
    }

    /**
     * Replace the entire cache for a wallet (useful for full refresh).
     */
    suspend fun replace(
        walletAddress: String,
        transactions: List<TONTransaction>,
        maxSize: Int = 100,
    ): List<TONTransaction> = mutex.withLock {
        val sorted = transactions
            .distinctBy { it.hash?.toString() ?: it.logicalTime } // Deduplicate
            .sortedByDescending { it.now }
            .take(maxSize)

        cache[walletAddress] = sorted
        lastUpdateTime[walletAddress] = System.currentTimeMillis()

        Log.d(TAG, "TransactionCache replaced for $walletAddress: ${sorted.size} transactions")

        sorted
    }

    /**
     * Clear cache for a specific wallet.
     */
    suspend fun clear(walletAddress: String) = mutex.withLock {
        cache.remove(walletAddress)
        lastUpdateTime.remove(walletAddress)
        Log.d(TAG, "TransactionCache cleared for $walletAddress")
    }

    /**
     * Clear all caches.
     */
    suspend fun clearAll() = mutex.withLock {
        cache.clear()
        lastUpdateTime.clear()
        Log.d(TAG, "TransactionCache cleared all")
    }

    /**
     * Get the last update time for a wallet (in milliseconds).
     */
    suspend fun getLastUpdateTime(walletAddress: String): Long? = mutex.withLock {
        lastUpdateTime[walletAddress]
    }

    /**
     * Check if cache is stale (older than maxAge milliseconds).
     */
    suspend fun isStale(walletAddress: String, maxAge: Long = 60_000L): Boolean = mutex.withLock {
        val lastUpdate = lastUpdateTime[walletAddress] ?: return@withLock true
        System.currentTimeMillis() - lastUpdate > maxAge
    }

    /**
     * Get cache statistics for debugging.
     */
    suspend fun getStats(): CacheStats = mutex.withLock {
        CacheStats(
            walletCount = cache.size,
            totalTransactions = cache.values.sumOf { it.size },
            oldestUpdate = lastUpdateTime.values.minOrNull() ?: 0L,
            newestUpdate = lastUpdateTime.values.maxOrNull() ?: 0L,
        )
    }

    companion object {
        private const val TAG = "TransactionCache"
    }
}

/**
 * Statistics about the transaction cache.
 */
data class CacheStats(
    val walletCount: Int,
    val totalTransactions: Int,
    val oldestUpdate: Long,
    val newestUpdate: Long,
)
