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
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.WalletSigner
import io.ton.walletkit.model.WalletSignerInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests for custom signer support.
 * Tests the hardware wallet integration pattern using WalletSigner interface.
 */
class CustomSignerTest {

    /**
     * Mock hardware wallet signer for testing.
     */
    private class MockHardwareWalletSigner(
        private val pubKey: String = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
    ) : WalletSigner {
        var signCallCount = 0
        var lastSignedData: ByteArray? = null

        override fun publicKey(): TONHex = TONHex(pubKey)

        override suspend fun sign(data: ByteArray): TONHex {
            signCallCount++
            lastSignedData = data
            // Return a mock signature (64 bytes as hex)
            return TONHex(WalletKitUtils.byteArrayToHex(ByteArray(64) { 0xaa.toByte() }))
        }
    }

    @Test
    fun `createSignerFromCustom returns valid WalletSignerInfo`() = runTest {
        val mockKit = mockk<ITONWalletKit>()
        val customSigner = MockHardwareWalletSigner()
        val expectedSignerInfo = WalletSignerInfo(
            signerId = "signer_custom_123",
            publicKey = customSigner.publicKey(),
        )

        coEvery { mockKit.createSignerFromCustom(customSigner) } returns expectedSignerInfo

        val result = mockKit.createSignerFromCustom(customSigner)

        assertNotNull(result)
        assertEquals(customSigner.publicKey(), result.publicKey)
        assertNotNull(result.signerId)
        assertTrue(result.signerId.isNotEmpty())
        coVerify(exactly = 1) { mockKit.createSignerFromCustom(customSigner) }
    }

    @Test
    fun `custom signer sign method is called for signing operations`() = runTest {
        val customSigner = MockHardwareWalletSigner()
        val testData = "test_transaction_data".toByteArray()

        val signature = customSigner.sign(testData)

        assertEquals(1, customSigner.signCallCount)
        assertArrayEquals(testData, customSigner.lastSignedData)
        assertNotNull(signature)
        assertTrue(signature.value.startsWith("0x"))
        // 64 bytes = 128 hex chars + "0x" prefix = 130 total
        assertEquals(130, signature.value.length)
    }

    @Test
    fun `custom signer can handle multiple signing operations`() = runTest {
        val customSigner = MockHardwareWalletSigner()

        val data1 = "transaction_1".toByteArray()
        val data2 = "transaction_2".toByteArray()
        val data3 = "transaction_3".toByteArray()

        customSigner.sign(data1)
        customSigner.sign(data2)
        customSigner.sign(data3)

        assertEquals(3, customSigner.signCallCount)
        assertArrayEquals(data3, customSigner.lastSignedData)
    }

    @Test
    fun `custom signer with different public keys creates different signers`() = runTest {
        val mockKit = mockk<ITONWalletKit>()

        val signer1 = MockHardwareWalletSigner(pubKey = "0x1111111111111111111111111111111111111111111111111111111111111111")
        val signer2 = MockHardwareWalletSigner(pubKey = "0x2222222222222222222222222222222222222222222222222222222222222222")

        val signerInfo1 = WalletSignerInfo(signerId = "signer_1", publicKey = signer1.publicKey())
        val signerInfo2 = WalletSignerInfo(signerId = "signer_2", publicKey = signer2.publicKey())

        coEvery { mockKit.createSignerFromCustom(signer1) } returns signerInfo1
        coEvery { mockKit.createSignerFromCustom(signer2) } returns signerInfo2

        val result1 = mockKit.createSignerFromCustom(signer1)
        val result2 = mockKit.createSignerFromCustom(signer2)

        assertNotEquals(result1.signerId, result2.signerId)
        assertNotEquals(result1.publicKey, result2.publicKey)
        assertEquals(signer1.publicKey(), result1.publicKey)
        assertEquals(signer2.publicKey(), result2.publicKey)
    }
}
