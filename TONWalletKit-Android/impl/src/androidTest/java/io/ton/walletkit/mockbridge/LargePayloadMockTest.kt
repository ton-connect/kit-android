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

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Tests large payload handling using REAL SDK with MOCK JavaScript.
 *
 * Scenario: JavaScript returns responses with very large data sets
 * (e.g., 100+ NFTs, transaction history, etc.).
 *
 * Tests that the SDK:
 * - Handles large JSON payloads without memory issues
 * - Parses large responses correctly
 * - Doesn't timeout on large data transfers
 */
@RunWith(AndroidJUnit4::class)
class LargePayloadMockTest : MockBridgeTestBase() {

    override fun getMockScenarioHtml(): String = "large-payload"

    @Test(timeout = 15000)
    fun largePayloadSdkHandlesHundredsOfNfts() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(12.seconds) {
                // First create a wallet to get NFTs from
                val mnemonic = sdk.createTonMnemonic()
                assertEquals(24, mnemonic.size)

                val signer = sdk.createSignerFromMnemonic(mnemonic)
                val adapter = sdk.createV5R1Adapter(signer)
                val wallet = sdk.addWallet(adapter.adapterId)

                // Mock returns 150 NFTs (large payload ~100KB+)
                val nfts = wallet.getNFTItems(limit = 150)

                // Should handle large response successfully
                assertEquals("Should receive 150 NFTs", 150, nfts.size)

                // Verify structure of first NFT
                val firstNft = nfts[0]
                assertTrue("NFT should have address", firstNft.address.isNotEmpty())
                assertNotNull("NFT should have metadata", firstNft.metadata)
            }
        }
    }

    @Test(timeout = 20000)
    fun largePayloadMultipleCallsWithLargeData() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(18.seconds) {
                // Create wallet
                val mnemonic = sdk.createTonMnemonic()
                val signer = sdk.createSignerFromMnemonic(mnemonic)
                val adapter = sdk.createV5R1Adapter(signer)
                val wallet = sdk.addWallet(adapter.adapterId)

                // Make 3 calls, each returning 100 NFTs
                var totalNfts = 0
                repeat(3) {
                    val nfts = wallet.getNFTItems(limit = 100, offset = it * 100)
                    totalNfts += nfts.size
                    delay(200)
                }

                // Verify all large responses were handled
                assertEquals("Should have received 300 NFTs total", 300, totalNfts)
            }
        }
    }
}
