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
package io.ton.walletkit

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for wallet management methods:
 * - getWallet(address)
 * - getWallets()
 * - removeWallet(address)
 * - clearWallets()
 */
class WalletManagementTest {

    private fun createMockWallet(address: String): ITONWallet {
        return mockk<ITONWallet>(relaxed = true) {
            every { this@mockk.address } returns address
            every { publicKey } returns "test_public_key_$address"
        }
    }

    @Test
    fun `getWallet returns wallet when it exists`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val address = "EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t"
        val expectedWallet = createMockWallet(address)

        coEvery { mockKit.getWallet(address) } returns expectedWallet

        val wallet = mockKit.getWallet(address)

        assertNotNull(wallet)
        assertEquals(address, wallet?.address)
        coVerify(exactly = 1) { mockKit.getWallet(address) }
    }

    @Test
    fun `getWallet returns null when wallet does not exist`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val nonExistentAddress = "EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_FAKE"

        coEvery { mockKit.getWallet(nonExistentAddress) } returns null

        val wallet = mockKit.getWallet(nonExistentAddress)

        assertNull(wallet)
        coVerify(exactly = 1) { mockKit.getWallet(nonExistentAddress) }
    }

    @Test
    fun `removeWallet returns true when wallet exists`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val address = "EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t"

        coEvery { mockKit.removeWallet(address) } returns true

        val result = mockKit.removeWallet(address)

        assertEquals(true, result)
        coVerify(exactly = 1) { mockKit.removeWallet(address) }
    }

    @Test
    fun `removeWallet returns false when wallet does not exist`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val nonExistentAddress = "EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_FAKE"

        coEvery { mockKit.removeWallet(nonExistentAddress) } returns false

        val result = mockKit.removeWallet(nonExistentAddress)

        assertEquals(false, result)
        coVerify(exactly = 1) { mockKit.removeWallet(nonExistentAddress) }
    }

    @Test
    fun `clearWallets removes all wallets`() = runTest {
        val mockKit = mockk<ITONWalletKit>()

        // Setup: Add multiple wallets first
        coEvery { mockKit.getWallets() } returnsMany listOf(
            listOf(
                createMockWallet("EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t"),
                createMockWallet("EQD5FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_1u"),
                createMockWallet("EQD6FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_2v"),
            ),
            emptyList(),
        )
        coEvery { mockKit.clearWallets() } just Runs

        // Verify wallets exist before clearing
        val walletsBeforeClear = mockKit.getWallets()
        assertEquals(3, walletsBeforeClear.size)

        // Clear all wallets
        mockKit.clearWallets()
        coVerify(exactly = 1) { mockKit.clearWallets() }

        val walletsAfterClear = mockKit.getWallets()
        assertEquals(0, walletsAfterClear.size)
    }

    @Test
    fun `clearWallets on empty state does not throw`() = runTest {
        val mockKit = mockk<ITONWalletKit>()

        coEvery { mockKit.getWallets() } returns emptyList()
        coEvery { mockKit.clearWallets() } just Runs

        // Should not throw exception
        mockKit.clearWallets()
        coVerify(exactly = 1) { mockKit.clearWallets() }
    }

    @Test
    fun `getWallets returns empty list when no wallets exist`() = runTest {
        val mockKit = mockk<ITONWalletKit>()

        coEvery { mockKit.getWallets() } returns emptyList()

        val wallets = mockKit.getWallets()

        assertTrue(wallets.isEmpty())
        coVerify(exactly = 1) { mockKit.getWallets() }
    }

    @Test
    fun `getWallets returns multiple wallets`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val expectedWallets = listOf(
            createMockWallet("EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t"),
            createMockWallet("EQD5FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_1u"),
        )

        coEvery { mockKit.getWallets() } returns expectedWallets

        val wallets = mockKit.getWallets()

        assertEquals(2, wallets.size)
        assertEquals(expectedWallets[0].address, wallets[0].address)
        assertEquals(expectedWallets[1].address, wallets[1].address)
    }

    @Test
    fun `removing wallet does not affect other wallets`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val wallet1 = createMockWallet("EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t")
        val wallet2 = createMockWallet("EQD5FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_1u")
        val address1 = "EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t"
        val address2 = "EQD5FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_1u"

        // Setup: Return both wallets initially
        coEvery { mockKit.getWallets() } returns listOf(wallet1, wallet2)
        coEvery { mockKit.removeWallet(address1) } returns true
        coEvery { mockKit.getWallet(address2) } returns wallet2

        // Remove first wallet
        val removed = mockKit.removeWallet(address1)
        assertEquals(true, removed)

        // Verify wallet2 still exists
        val existingWallet = mockKit.getWallet(address2)
        assertNotNull(existingWallet)
        assertEquals(address2, existingWallet?.address)

        // Verify removeWallet was called with correct address
        coVerify(exactly = 1) { mockKit.removeWallet(address1) }
    }

    @Test
    fun `getWallets isolation between tests`() = runTest {
        val mockKit = mockk<ITONWalletKit>()

        coEvery { mockKit.getWallets() } returns emptyList()

        val wallets = mockKit.getWallets()

        assertTrue(wallets.isEmpty())
        coVerify(exactly = 1) { mockKit.getWallets() }
    }
}
