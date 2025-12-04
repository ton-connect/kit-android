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

import io.ton.walletkit.mockbridge.infra.DefaultMockScenario
import io.ton.walletkit.mockbridge.infra.MockBridgeTestBase
import io.ton.walletkit.mockbridge.infra.MockScenario
import io.ton.walletkit.model.TONNFTCollection
import io.ton.walletkit.model.TONNFTItem
import io.ton.walletkit.model.TONNFTItems
import io.ton.walletkit.model.TONTokenInfo
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
        override fun handleGetNfts(walletAddress: String, limit: Int, offset: Int): TONNFTItems {
            val nfts = mutableListOf<TONNFTItem>()
            for (i in 0 until limit) {
                val actualIndex = offset + i
                nfts.add(
                    TONNFTItem(
                        address = "EQD${actualIndex.toString().padStart(46, '0')}",
                        auctionContractAddress = null,
                        codeHash = "0x${actualIndex.toString(16).padStart(64, '0')}",
                        dataHash = "0x${(actualIndex + 1).toString(16).padStart(64, '0')}",
                        collection = TONNFTCollection(
                            address = "EQCollection${"0".repeat(42)}",
                            codeHash = "0x${"1234567890abcdef".repeat(4)}",
                            dataHash = "0x${"fedcba0987654321".repeat(4)}",
                            lastTransactionLt = "1000000",
                            nextItemIndex = limit.toString(),
                            ownerAddress = "EQCollectionOwner${"0".repeat(36)}",
                        ),
                        collectionAddress = "EQCollection${"0".repeat(42)}",
                        metadata = TONTokenInfo(
                            name = "NFT Item #$actualIndex",
                            description = "This is a test NFT item number $actualIndex with some metadata that makes the payload larger.",
                            image = "https://example.com/nft/$actualIndex.png",
                            nftIndex = actualIndex.toString(),
                            type = "nft",
                            valid = true,
                        ),
                        index = actualIndex.toString(),
                        initFlag = true,
                        lastTransactionLt = (System.currentTimeMillis() + actualIndex).toString(),
                        onSale = actualIndex % 10 == 0,
                        ownerAddress = "EQOwner${actualIndex.toString().padStart(40, '0')}",
                        realOwner = "EQOwner${actualIndex.toString().padStart(40, '0')}",
                        saleContractAddress = if (actualIndex % 10 == 0) "EQSale${actualIndex.toString().padStart(41, '0')}" else null,
                    ),
                )
            }
            return TONNFTItems(items = nfts)
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
        val nfts = wallet.getNFTItems(limit = 150)

        // Should handle large response successfully
        assertEquals("Should receive 150 NFTs", 150, nfts.size)

        // Verify structure of first NFT
        val firstNft = nfts[0]
        assertTrue("NFT should have address", firstNft.address.isNotEmpty())
        assertNotNull("NFT should have metadata", firstNft.metadata)
        assertEquals("NFT Item #0", firstNft.metadata?.name)
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
            val nfts = wallet.getNFTItems(limit = 100, offset = iteration * 100)
            totalNfts += nfts.size

            // Verify offset is being used correctly
            if (nfts.isNotEmpty()) {
                val expectedFirstIndex = iteration * 100
                assertEquals("NFT Item #$expectedFirstIndex", nfts[0].metadata?.name)
            }
        }

        // Verify all large responses were handled
        assertEquals("Should have received 300 NFTs total", 300, totalNfts)
    }
}
