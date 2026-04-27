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
import io.ton.walletkit.api.TETRA
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.data.cache.TransactionCache
import io.ton.walletkit.demo.data.storage.DemoAppStorage
import io.ton.walletkit.demo.data.storage.UserPreferences
import io.ton.walletkit.demo.data.storage.WalletRecord
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

        val metadataCorrections = mutableListOf<String>()
        for (wallet in wallets) {
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
        val freshWallets = kit.getWallets()
        // If the bridge returns empty but we have cached wallets, the bridge is likely
        // reinitializing (e.g. after the in-app WebView browser was closed on a slow CI
        // emulator). Preserve the cache so activeWalletAddress is not incorrectly nulled out.
        val wallets = if (freshWallets.isEmpty() && tonWallets.isNotEmpty()) {
            Log.w(LOG_TAG, "loadWalletSummaries: kit returned empty but cache has ${tonWallets.size} wallets – reusing cache")
            tonWallets.values.toList()
        } else {
            tonWallets.clear()
            freshWallets.forEach { wallet ->
                wallet.address?.value?.let { tonWallets[it] = wallet }
            }
            freshWallets
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
            val interfaceType = storedRecord?.interfaceType?.let { WalletInterfaceType.fromValue(it) }
                ?: WalletInterfaceType.MNEMONIC

            result.add(
                WalletSummary(
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
        onRefresh()
    }

    suspend fun clearCachesForReset() {
        tonWallets.clear()
        walletMetadata.clear()
        transactionCache.clearAll()
        lastPersistedActiveWallet = null
    }

    private suspend fun ensureMetadataForAddress(address: String): WalletMetadata {
        walletMetadata[address]?.let { return it }

        val storedRecord = storage.loadWallet(address)
        val metadata = storedRecord?.let {
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

        if (storedRecord != null) {
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
        ChainIds.TETRA -> TONNetwork.TETRA
        null, "" -> fallback
        else -> fallback
    }

    companion object {
        private const val LOG_TAG = "WalletLifecycleMgr"
    }
}
