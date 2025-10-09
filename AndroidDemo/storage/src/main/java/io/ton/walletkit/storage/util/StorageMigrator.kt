package io.ton.walletkit.storage.util

import android.content.Context
import android.util.Log
import io.ton.walletkit.storage.impl.DebugSharedPrefsStorage
import io.ton.walletkit.storage.impl.SecureWalletKitStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for migrating data from DebugSharedPrefsStorage to SecureWalletKitStorage.
 *
 * Usage:
 * ```
 * val migrator = StorageMigrator(context)
 * val migrated = migrator.migrateFromDebugToSecure()
 * if (migrated > 0) {
 *     Log.d("Migration", "Migrated $migrated wallets to secure storage")
 * }
 * ```
 */
class StorageMigrator(private val context: Context) {
    private val tag = "StorageMigrator"

    /**
     * Migrates all wallets and session hints from DebugSharedPrefsStorage to SecureWalletKitStorage.
     *
     * @param deleteOldData If true, deletes data from DebugSharedPrefsStorage after successful migration
     * @return Number of wallets migrated
     */
    suspend fun migrateFromDebugToSecure(deleteOldData: Boolean = true): Int = withContext(Dispatchers.IO) {
        try {
            val debugStorage = DebugSharedPrefsStorage(context)
            val secureStorage = SecureWalletKitStorage(context)

            // Migrate wallets
            val wallets = debugStorage.loadAllWallets()
            Log.d(tag, "Found ${wallets.size} wallets to migrate")

            var migratedCount = 0
            wallets.forEach { (accountId, record) ->
                try {
                    // Check if already exists in secure storage
                    val existing = secureStorage.loadWallet(accountId)
                    if (existing != null) {
                        Log.d(tag, "Wallet $accountId already exists in secure storage, skipping")
                        return@forEach
                    }

                    // Save to secure storage
                    secureStorage.saveWallet(accountId, record)
                    migratedCount++
                    Log.d(tag, "Migrated wallet: $accountId")

                    // Delete from debug storage if requested
                    if (deleteOldData) {
                        debugStorage.clear(accountId)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to migrate wallet $accountId", e)
                }
            }

            // Migrate session hints
            val hints = debugStorage.loadSessionHints()
            Log.d(tag, "Found ${hints.size} session hints to migrate")

            hints.forEach { (key, hint) ->
                try {
                    secureStorage.saveSessionHint(key, hint)
                    Log.d(tag, "Migrated session hint: $key")

                    // Delete from debug storage if requested
                    if (deleteOldData) {
                        debugStorage.clearSessionHint(key)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to migrate session hint $key", e)
                }
            }

            Log.d(tag, "Migration complete: $migratedCount wallets, ${hints.size} hints")
            migratedCount
        } catch (e: Exception) {
            Log.e(tag, "Migration failed", e)
            0
        }
    }

    /**
     * Checks if there is data in DebugSharedPrefsStorage that needs migration.
     */
    suspend fun hasPendingMigration(): Boolean = withContext(Dispatchers.IO) {
        try {
            val debugStorage = DebugSharedPrefsStorage(context)
            val wallets = debugStorage.loadAllWallets()
            wallets.isNotEmpty()
        } catch (e: Exception) {
            Log.e(tag, "Failed to check for pending migration", e)
            false
        }
    }
}
