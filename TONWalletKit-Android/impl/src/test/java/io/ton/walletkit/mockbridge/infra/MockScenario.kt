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
package io.ton.walletkit.mockbridge.infra

import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSignerInfo
import org.json.JSONObject

/**
 * Interface for mock scenarios that define how the mocked engine responds to RPC calls.
 *
 * Each test can implement this interface to define custom behavior for specific test cases.
 * Default implementations are provided for common operations.
 */
interface MockScenario {

    /**
     * Handle init RPC call.
     * Override to simulate delayed initialization or init that never completes.
     * By default, init completes immediately.
     */
    suspend fun handleInit(configuration: TONWalletKitConfiguration) {
        // Default: init completes immediately
    }

    /**
     * Handle createTonMnemonic RPC call.
     * @param wordCount Number of words to generate (12 or 24)
     * @return List of mnemonic words
     */
    fun handleCreateTonMnemonic(wordCount: Int): List<String> = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident",
        "account", "accuse", "achieve", "acid", "acoustic", "acquire",
        "across", "act", "action", "actor", "actress", "actual",
    ).take(wordCount)

    /**
     * Handle createSignerFromMnemonic RPC call.
     */
    fun handleCreateSignerFromMnemonic(mnemonic: List<String>, mnemonicType: String): WalletSignerInfo {
        return WalletSignerInfo(
            signerId = "signer-${mnemonic.hashCode().toString(16)}",
            publicKey = TONHex("0x${"0".repeat(64)}"),
        )
    }

    /**
     * Handle createV5R1Adapter RPC call.
     */
    fun handleCreateV5R1Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo {
        return WalletAdapterInfo(
            adapterId = "adapter-v5r1-${signerId.hashCode().toString(16)}",
            address = TONUserFriendlyAddress("EQDTest${signerId.hashCode().toString(16).padStart(40, '0')}"),
            network = network ?: TONNetwork.TESTNET,
        )
    }

    /**
     * Handle createV4R2Adapter RPC call.
     */
    fun handleCreateV4R2Adapter(
        signerId: String,
        network: TONNetwork?,
        workchain: Int,
        walletId: Long,
        publicKey: String?,
        isCustom: Boolean,
    ): WalletAdapterInfo {
        return WalletAdapterInfo(
            adapterId = "adapter-v4r2-${signerId.hashCode().toString(16)}",
            address = TONUserFriendlyAddress("EQDTest${signerId.hashCode().toString(16).padStart(40, '0')}"),
            network = network ?: TONNetwork.TESTNET,
        )
    }

    /**
     * Handle addWallet RPC call.
     */
    fun handleAddWallet(adapterId: String): WalletAccount {
        val address = "EQDTest${adapterId.hashCode().toString(16).padStart(40, '0')}"
        // Use a mock walletId hash (in real bridge, this comes from SHA256 of chainId:address)
        return WalletAccount(
            walletId = "mock-wallet-id-${adapterId.hashCode().toString(16)}",
            address = TONUserFriendlyAddress(address),
            publicKey = "0x${"0".repeat(64)}",
            version = WalletVersions.V5R1,
        )
    }

    /**
     * Handle getWallets RPC call.
     */
    fun handleGetWallets(): List<WalletAccount> = emptyList()

    /**
     * Handle getNfts RPC call.
     * Override this in specific scenarios to test large payloads, errors, etc.
     */
    fun handleGetNfts(walletAddress: String, limit: Int, offset: Int): TONNFTsResponse {
        return TONNFTsResponse(nfts = emptyList(), addressBook = null)
    }

    /**
     * Handle generic RPC call via callBridgeMethod.
     * This is a fallback for methods not explicitly handled above.
     */
    fun handleRpcCall(method: String, params: JSONObject?): JSONObject {
        return JSONObject().put("success", true)
    }
}

/**
 * Default mock scenario that provides basic responses for all RPC calls.
 * Useful as a base class for custom scenarios.
 */
open class DefaultMockScenario : MockScenario
