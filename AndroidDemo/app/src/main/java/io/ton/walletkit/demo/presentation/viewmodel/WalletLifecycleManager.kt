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
package io.ton.walletkit.demo.presentation.viewmodel

import android.util.Log
import io.ton.walletkit.ITONWallet
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.ChainIds
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.data.cache.TransactionCache
import io.ton.walletkit.demo.data.storage.DemoAppStorage
import io.ton.walletkit.demo.data.storage.UserPreferences
import io.ton.walletkit.demo.data.storage.WalletRecord
import io.ton.walletkit.demo.domain.model.PendingWalletRecord
import io.ton.walletkit.demo.domain.model.WalletInterfaceType
import io.ton.walletkit.demo.domain.model.WalletMetadata
import io.ton.walletkit.demo.presentation.model.SessionSummary
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.util.TonFormatter

/**
 * Handles wallet lifecycle: bootstrapping from storage, metadata management,
 * and producing [WalletSummary] items for UI consumption.
 */
class WalletLifecycleManager(
    private val storage: DemoAppStorage,
    private val defaultWalletVersion: String,
    private val defaultWalletNameProvider: (Int) -> String,
    private val kitProvider: suspend () -> ITONWalletKit,
    initialNetwork: TONNetwork,
) {

    data class BootstrapResult(
        val savedActiveWallet: String?,
    )

    val tonWallets: MutableMap<String, ITONWallet> = mutableMapOf()
    val walletMetadata: MutableMap<String, WalletMetadata> = mutableMapOf()
    val pendingWallets: ArrayDeque<PendingWalletRecord> = ArrayDeque()
    val transactionCache: TransactionCache = TransactionCache()

    var currentNetwork: TONNetwork = initialNetwork
        private set

    var lastPersistedActiveWallet: String? = null
        private set

    suspend fun bootstrap(): Result<BootstrapResult> = runCatching {
        val userPrefs = storage.loadUserPreferences()
        lastPersistedActiveWallet = userPrefs?.activeWalletAddress

        val kit = kitProvider()
        val wallets = kit.getWallets()
        tonWallets.clear()
        wallets.forEach { wallet ->
            wallet.address?.value?.let { tonWallets[it] = wallet }
        }

        if (tonWallets.isEmpty()) {
            rehydrateWalletsFromStorage()
        }

        val walletsAfterMigration = kit.getWallets()
        tonWallets.clear()
        val metadataCorrections = mutableListOf<String>()
        for (wallet in walletsAfterMigration) {
            val address = wallet.address?.value ?: continue
            tonWallets[address] = wallet
            if (walletMetadata[address] == null) {
                val storedRecord = storage.loadWallet(address)
                if (storedRecord != null) {
                    walletMetadata[address] = WalletMetadata(
                        name = storedRecord.name,
                        network = parseNetworkString(storedRecord.network, currentNetwork),
                        version = storedRecord.version,
                    )
                } else {
                    walletMetadata[address] = WalletMetadata(
                        name = defaultWalletNameProvider(walletMetadata.size),
                        network = currentNetwork,
                        version = defaultWalletVersion,
                    )
                    metadataCorrections.add(address)
                }
            }
        }
        if (metadataCorrections.isNotEmpty()) {
            Log.w(
                LOG_TAG,
                "No stored metadata for restored wallets: ${metadataCorrections.joinToString()} (fallback metadata applied)",
            )
        }

        BootstrapResult(savedActiveWallet = lastPersistedActiveWallet)
    }

    suspend fun loadWalletSummaries(sessions: List<SessionSummary>): List<WalletSummary> {
        val kit = kitProvider()
        val wallets = kit.getWallets()
        tonWallets.clear()
        wallets.forEach { wallet ->
            wallet.address?.value?.let { tonWallets[it] = wallet }
        }

        val knownAddresses = wallets.map { it.address.value }.toSet()
        walletMetadata.keys.retainAll(knownAddresses)

        val result = mutableListOf<WalletSummary>()
        for (wallet in wallets) {
            val address = wallet.address.value
            val metadata = ensureMetadataForAddress(address)

            val balance = runCatching { wallet.balance() }
                .onFailure { Log.e(LOG_TAG, "loadWalletSummaries: balance failed for $address", it) }
                .getOrNull()
            val formattedBalance = balance?.let { TonFormatter.formatTon(it.value) }

            val cachedTransactions = transactionCache.get(address)
            val walletSessions = sessions.filter { it.walletAddress == address }

            val storedRecord = storage.loadWallet(address)
            val createdAt = storedRecord?.createdAt
            val interfaceType = storedRecord?.interfaceType?.let { io.ton.walletkit.demo.domain.model.WalletInterfaceType.fromValue(it) }
                ?: io.ton.walletkit.demo.domain.model.WalletInterfaceType.MNEMONIC

            result.add(
                WalletSummary(
                    walletId = wallet.id,
                    address = address,
                    name = metadata.name,
                    network = metadata.network,
                    version = metadata.version.ifBlank { defaultWalletVersion },
                    publicKey = null,
                    balanceNano = balance?.value,
                    balance = formattedBalance,
                    transactions = cachedTransactions,
                    lastUpdated = System.currentTimeMillis(),
                    connectedSessions = walletSessions,
                    createdAt = createdAt,
                    interfaceType = interfaceType,
                ),
            )
        }
        return result
    }

    suspend fun persistActiveWalletPreference(address: String?) {
        if (lastPersistedActiveWallet == address) return
        val updatedPrefs = UserPreferences(activeWalletAddress = address)
        storage.saveUserPreferences(updatedPrefs)
        lastPersistedActiveWallet = address
    }

    suspend fun switchNetworkIfNeeded(target: TONNetwork, onRefresh: suspend () -> Unit) {
        if (target == currentNetwork) return
        currentNetwork = target
        walletMetadata.clear()
        pendingWallets.clear()
        onRefresh()
    }

    suspend fun clearCachesForReset() {
        tonWallets.clear()
        walletMetadata.clear()
        transactionCache.clearAll()
        lastPersistedActiveWallet = null
    }

    private suspend fun rehydrateWalletsFromStorage(): Boolean {
        val stored = storage.loadAllWallets()
        if (stored.isEmpty()) {
            Log.d(LOG_TAG, "rehydrate: storage empty, nothing to restore")
            return false
        }

        val kit = kitProvider()
        var restoredCount = 0
        for ((storedAddress, record) in stored) {
            val interfaceType = record.interfaceType
            if (interfaceType != WalletInterfaceType.MNEMONIC.value) {
                Log.w(
                    LOG_TAG,
                    "rehydrate: skipping $storedAddress (interfaceType=$interfaceType not supported for auto-restore)",
                )
                continue
            }

            val networkEnum = parseNetworkString(record.network, currentNetwork)
            val version = record.version.ifBlank { defaultWalletVersion }
            val name = record.name.ifBlank { defaultWalletNameProvider(restoredCount) }

            val result = runCatching {
                when (version) {
                    WalletVersions.V4R2 -> {
                        val signer = kit.createSignerFromMnemonic(record.mnemonic)
                        val adapter = kit.createV4R2Adapter(signer, networkEnum)
                        kit.addWallet(adapter.adapterId)
                    }
                    WalletVersions.V5R1 -> {
                        val signer = kit.createSignerFromMnemonic(record.mnemonic)
                        val adapter = kit.createV5R1Adapter(signer, networkEnum)
                        kit.addWallet(adapter.adapterId)
                    }
                    else -> {
                        Log.w(LOG_TAG, "rehydrate: unsupported version $version for $storedAddress")
                        null
                    }
                }
            }
            if (result.getOrNull() == null) {
                continue
            }
            result.onSuccess { walletNullable ->
                val wallet = walletNullable ?: return@onSuccess
                val restoredAddress = wallet.address?.value
                if (restoredAddress.isNullOrBlank()) {
                    Log.w(LOG_TAG, "rehydrate: wallet added but address null for stored $storedAddress")
                    return@onSuccess
                }

                tonWallets[restoredAddress] = wallet
                walletMetadata[restoredAddress] = WalletMetadata(
                    name = name,
                    network = networkEnum,
                    version = version,
                )

                if (restoredAddress != storedAddress) {
                    Log.w(
                        LOG_TAG,
                        "rehydrate: restored address mismatch stored=$storedAddress restored=$restoredAddress",
                    )
                }

                restoredCount += 1
                Log.d(
                    LOG_TAG,
                    "rehydrate: restored wallet $restoredAddress (name='$name', network=$networkEnum, version=$version)",
                )
            }.onFailure {
                Log.e(LOG_TAG, "rehydrate: failed to restore $storedAddress", it)
            }
        }

        return restoredCount > 0
    }

    private suspend fun ensureMetadataForAddress(address: String): WalletMetadata {
        walletMetadata[address]?.let { return it }

        val pending = pendingWallets.removeLastOrNull()
        val storedRecord = storage.loadWallet(address)
        val metadata = pending?.metadata
            ?: storedRecord?.let {
                WalletMetadata(
                    name = it.name,
                    network = parseNetworkString(it.network, currentNetwork),
                    version = it.version,
                )
            }
            ?: WalletMetadata(
                name = defaultWalletNameProvider(walletMetadata.size),
                network = currentNetwork,
                version = defaultWalletVersion,
            )
        walletMetadata[address] = metadata

        if (pending?.mnemonic != null) {
            val record = WalletRecord(
                mnemonic = pending.mnemonic,
                name = metadata.name,
                network = metadata.network.chainId,
                version = metadata.version,
            )
            runCatching { storage.saveWallet(address, record) }
                .onSuccess { Log.d(LOG_TAG, "ensureMetadataForAddress: saved pending record for $address") }
                .onFailure { Log.e(LOG_TAG, "ensureMetadataForAddress: failed to save pending record for $address", it) }
        } else if (storedRecord != null) {
            val needsUpdate = storedRecord.name != metadata.name ||
                storedRecord.network != metadata.network.chainId ||
                storedRecord.version != metadata.version
            if (needsUpdate) {
                val record = WalletRecord(
                    mnemonic = storedRecord.mnemonic,
                    name = metadata.name,
                    network = metadata.network.chainId,
                    version = metadata.version,
                    interfaceType = storedRecord.interfaceType,
                    createdAt = storedRecord.createdAt,
                )
                runCatching { storage.saveWallet(address, record) }
                    .onSuccess { Log.d(LOG_TAG, "ensureMetadataForAddress: refreshed stored record for $address") }
                    .onFailure { Log.e(LOG_TAG, "ensureMetadataForAddress: refresh save failed for $address", it) }
            }
        }

        return metadata
    }

    private fun parseNetworkString(networkStr: String?, fallback: TONNetwork): TONNetwork = when (networkStr?.trim()) {
        ChainIds.MAINNET -> TONNetwork.MAINNET
        ChainIds.TESTNET -> TONNetwork.TESTNET
        null, "" -> fallback
        else -> fallback
    }

    companion object {
        private const val LOG_TAG = "WalletLifecycleMgr"
    }
}
