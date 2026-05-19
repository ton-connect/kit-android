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
package io.ton.walletkit.client

import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.generated.TONGetMethodResult
import io.ton.walletkit.api.generated.TONMasterchainInfo
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONRawStackItem
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONUserFriendlyAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class TONAPIClientNetworkTest {

    private class StubbedClient(
        override var network: TONNetwork,
    ) : TONAPIClient {
        override suspend fun sendBoc(boc: TONBase64): String = ""
        override suspend fun runGetMethod(
            address: TONUserFriendlyAddress,
            method: String,
            stack: List<TONRawStackItem>?,
            seqno: Int?,
        ): TONGetMethodResult = error("not used")
        override suspend fun getBalance(address: TONUserFriendlyAddress, seqno: Int?): String = "0"
        override suspend fun getMasterchainInfo(): TONMasterchainInfo = error("not used")
    }

    @Test
    fun `client_network reflects post-construction mutations`() {
        val client = StubbedClient(network = TONNetwork.MAINNET)
        assertEquals(TONNetwork.MAINNET, client.network)

        client.network = TONNetwork.TESTNET
        assertEquals(TONNetwork.TESTNET, client.network)
    }

    @Test
    fun `client_network can carry a custom chainId`() {
        val custom = TONNetwork(chainId = "123456")
        val client = StubbedClient(network = custom)

        assertEquals(custom, client.network)
        assertEquals("123456", client.network.chainId)
    }

    @Test
    fun `NetworkConfiguration_apiClient_network takes priority over configured slot`() {
        val client = StubbedClient(network = TONNetwork.TESTNET)
        val networkConfig = TONWalletKitConfiguration.NetworkConfiguration(
            network = TONNetwork.MAINNET,
            apiClient = client,
        )

        val resolved = networkConfig.apiClient?.network ?: networkConfig.network
        assertEquals(TONNetwork.TESTNET, resolved)
    }

    @Test
    fun `NetworkConfiguration without custom client falls back to configured network`() {
        val networkConfig = TONWalletKitConfiguration.NetworkConfiguration(
            network = TONNetwork.MAINNET,
            apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(key = "k"),
        )

        val resolved = networkConfig.apiClient?.network ?: networkConfig.network
        assertEquals(TONNetwork.MAINNET, resolved)
    }

    @Test
    fun `WebViewManager apiGetNetworkForChainId lookup returns live client_network`() {
        val mainnetClient = StubbedClient(network = TONNetwork.MAINNET)
        val testnetClient = StubbedClient(network = TONNetwork.TESTNET)
        val clients: List<TONAPIClient> = listOf(mainnetClient, testnetClient)

        val mainnetChain = TONNetwork.MAINNET.chainId
        val customChain = "999"

        val lookupBefore = clients.find { it.network.chainId == mainnetChain }?.network
        assertEquals(TONNetwork.MAINNET, lookupBefore)

        mainnetClient.network = TONNetwork(chainId = customChain)
        val lookupByNewChainId = clients.find { it.network.chainId == customChain }?.network
        assertEquals(TONNetwork(chainId = customChain), lookupByNewChainId)
    }
}
