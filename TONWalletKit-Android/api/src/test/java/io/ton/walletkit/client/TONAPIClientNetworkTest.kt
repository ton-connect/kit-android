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
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifies how custom [TONAPIClient] implementations attach to networks via
 * [TONWalletKitConfiguration.NetworkConfiguration]. Network identity is owned by the
 * configuration, not by the client — matches iOS, where `TONAPIClient` does not expose
 * `network`.
 */
class TONAPIClientNetworkTest {

    private class StubbedClient : TONAPIClient {
        override suspend fun sendBoc(boc: TONBase64): String = ""
        override suspend fun runGetMethod(
            address: TONUserFriendlyAddress,
            method: String,
            stack: List<TONRawStackItem>?,
            seqno: Int?,
        ): TONGetMethodResult = error("not used")
        override suspend fun getMasterchainInfo(): TONMasterchainInfo = error("not used")
    }

    @Test
    fun `NetworkConfiguration pairs a custom client with its network`() {
        val client = StubbedClient()
        val nc = TONWalletKitConfiguration.NetworkConfiguration(
            network = TONNetwork.MAINNET,
            apiClient = client,
        )

        assertEquals(TONNetwork.MAINNET, nc.network)
        assertNotNull(nc.apiClient)
    }

    @Test
    fun `NetworkConfiguration without custom client retains its network`() {
        val nc = TONWalletKitConfiguration.NetworkConfiguration(
            network = TONNetwork.MAINNET,
            apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(key = "k"),
        )

        assertEquals(TONNetwork.MAINNET, nc.network)
    }

    @Test
    fun `multiple networks each carry their own client pairing`() {
        val mainnetClient = StubbedClient()
        val testnetClient = StubbedClient()
        val configs = listOf(
            TONWalletKitConfiguration.NetworkConfiguration(
                network = TONNetwork.MAINNET,
                apiClient = mainnetClient,
            ),
            TONWalletKitConfiguration.NetworkConfiguration(
                network = TONNetwork.TESTNET,
                apiClient = testnetClient,
            ),
        )

        val byMainnet = configs.firstOrNull { it.network == TONNetwork.MAINNET }?.apiClient
        val byTestnet = configs.firstOrNull { it.network == TONNetwork.TESTNET }?.apiClient

        assertEquals(mainnetClient, byMainnet)
        assertEquals(testnetClient, byTestnet)
    }

    @Test
    fun `network pairs can carry a custom chainId`() {
        val custom = TONNetwork(chainId = "123456")
        val nc = TONWalletKitConfiguration.NetworkConfiguration(
            network = custom,
            apiClient = StubbedClient(),
        )

        assertEquals(custom, nc.network)
        assertEquals("123456", nc.network.chainId)
    }
}
