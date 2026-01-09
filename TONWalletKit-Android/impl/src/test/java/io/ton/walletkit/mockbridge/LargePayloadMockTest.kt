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
package io.ton.walletkit.mockbridge

import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.mockbridge.infra.DefaultMockScenario
import io.ton.walletkit.mockbridge.infra.MockBridgeTestBase
import io.ton.walletkit.mockbridge.infra.MockScenario
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests large payload handling using mocked engine.
 *
 * Scenario: Engine returns responses with very large data sets
 * (e.g., 100+ NFTs, transaction history, etc.).
 *
 * Tests that the SDK:
 * - Handles large payloads correctly
 * - Parses large responses correctly
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class LargePayloadMockTest : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = LargePayloadScenario()

    /**
     * Scenario that returns large NFT collections.
     */
    private class LargePayloadScenario : DefaultMockScenario() {
        override fun handleGetNfts(walletAddress: String, limit: Int, offset: Int): TONNFTsResponse {
            val nfts = mutableListOf<TONNFT>()
            for (i in 0 until limit) {
                val actualIndex = offset + i
                nfts.add(
                    TONNFT(
                        address = io.ton.walletkit.model.TONUserFriendlyAddress("EQD${actualIndex.toString().padStart(46, '0')}"),
                        index = actualIndex.toString(),
                        info = io.ton.walletkit.api.generated.TONTokenInfo(
                            name = "NFT Item #$actualIndex",
                            description = "This is a test NFT item number $actualIndex with some metadata that makes the payload larger.",
                            image = io.ton.walletkit.api.generated.TONTokenImage(
                                url = "https://example.com/nft/$actualIndex.png",
                            ),
                        ),
                        isInited = true,
                        isOnSale = actualIndex % 10 == 0,
                        ownerAddress = io.ton.walletkit.model.TONUserFriendlyAddress("EQOwner${actualIndex.toString().padStart(40, '0')}"),
                    ),
                )
            }
            return TONNFTsResponse(nfts = nfts, addressBook = null)
        }
    }

    @Test
    fun `large payload - SDK handles hundreds of NFTs`() = runTest {
        // Create a wallet to get NFTs from
        val mnemonic = sdk.createTonMnemonic()
        assertEquals(24, mnemonic.size)

        val signer = sdk.createSignerFromMnemonic(mnemonic)
        val adapter = sdk.createV5R1Adapter(signer)
        val wallet = sdk.addWallet(adapter.adapterId)

        // Mock returns 150 NFTs (large payload)
        val response = wallet.nfts(
            io.ton.walletkit.api.generated.TONNFTsRequest(
                pagination = io.ton.walletkit.api.generated.TONPagination(limit = 150),
            ),
        )

        // Should handle large response successfully
        assertEquals("Should receive 150 NFTs", 150, response.nfts.size)

        // Verify structure of first NFT
        val firstNft = response.nfts[0]
        assertTrue("NFT should have address", firstNft.address.value.isNotEmpty())
        assertNotNull("NFT should have info", firstNft.info)
        assertEquals("NFT Item #0", firstNft.info?.name)
    }

    @Test
    fun `large payload - multiple calls with large data`() = runTest {
        // Create wallet
        val mnemonic = sdk.createTonMnemonic()
        val signer = sdk.createSignerFromMnemonic(mnemonic)
        val adapter = sdk.createV5R1Adapter(signer)
        val wallet = sdk.addWallet(adapter.adapterId)

        // Make 3 calls, each returning 100 NFTs
        var totalNfts = 0
        repeat(3) { iteration ->
            val response = wallet.nfts(
                io.ton.walletkit.api.generated.TONNFTsRequest(
                    pagination = io.ton.walletkit.api.generated.TONPagination(
                        limit = 100,
                        offset = iteration * 100,
                    ),
                ),
            )
            totalNfts += response.nfts.size

            // Verify offset is being used correctly
            if (response.nfts.isNotEmpty()) {
                val expectedFirstIndex = iteration * 100
                assertEquals("NFT Item #$expectedFirstIndex", response.nfts[0].info?.name)
            }
        }

        // Verify all large responses were handled
        assertEquals("Should have received 300 NFTs total", 300, totalNfts)
    }
}
