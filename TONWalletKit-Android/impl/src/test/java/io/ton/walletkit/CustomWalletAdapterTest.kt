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

import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONPreparedSignData
import io.ton.walletkit.api.generated.TONProofMessage
import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONUserFriendlyAddress
import io.ton.walletkit.model.TONWalletAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the user-implementable [TONWalletAdapter] contract — in particular the
 * `signedSignMessage` method added for TON-1240 parity with iOS `TONWalletAdapterProtocol`.
 *
 * Mirrors iOS `TONWalletAdapterTests` / `TONWalletAdapterJSAdapterTests` at the interface layer.
 */
class CustomWalletAdapterTest {

    private class MockWalletAdapter(
        private val adapterId: String = "adapter-1",
        private val pubKey: String = "0xpub",
        private val net: TONNetwork = TONNetwork(chainId = "-239"),
        private val addr: String = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N",
    ) : TONWalletAdapter {
        var stateInitCalls = 0
        var signedSendTransactionCalls = 0
        var signedSignMessageCalls = 0
        var lastFakeSignature: Boolean? = null
        var lastTransactionRequest: TONTransactionRequest? = null

        override fun identifier(): String = adapterId
        override fun publicKey(): TONHex = TONHex(pubKey)
        override fun network(): TONNetwork = net
        override fun address(testnet: Boolean): TONUserFriendlyAddress = TONUserFriendlyAddress(addr)

        override suspend fun stateInit(): TONBase64 {
            stateInitCalls++
            return TONBase64("c3RhdGVfaW5pdF9ib2M=")
        }

        override suspend fun signedSendTransaction(
            input: TONTransactionRequest,
            fakeSignature: Boolean?,
        ): TONBase64 {
            signedSendTransactionCalls++
            lastFakeSignature = fakeSignature
            lastTransactionRequest = input
            return TONBase64("c2lnbmVkX3R4")
        }

        override suspend fun signedSignMessage(
            input: TONTransactionRequest,
            fakeSignature: Boolean?,
        ): TONBase64 {
            signedSignMessageCalls++
            lastFakeSignature = fakeSignature
            lastTransactionRequest = input
            return TONBase64("c2lnbmVkX21zZw==")
        }

        override suspend fun signedSignData(
            input: TONPreparedSignData,
            fakeSignature: Boolean?,
        ): TONHex = TONHex("0xdead")

        override suspend fun signedTonProof(
            input: TONProofMessage,
            fakeSignature: Boolean?,
        ): TONHex = TONHex("0xbeef")

        override fun supportedFeatures(): List<TONWalletKitConfiguration.Feature>? = null
    }

    @Test
    fun `signedSignMessage is callable and returns base64`() = runTest {
        val adapter = MockWalletAdapter()
        val tx = TONTransactionRequest(messages = emptyList())

        val signed = adapter.signedSignMessage(tx, fakeSignature = false)

        assertEquals(1, adapter.signedSignMessageCalls)
        assertEquals(false, adapter.lastFakeSignature)
        assertEquals("c2lnbmVkX21zZw==", signed.value)
    }

    @Test
    fun `signedSignMessage records fakeSignature flag when true`() = runTest {
        val adapter = MockWalletAdapter()

        adapter.signedSignMessage(TONTransactionRequest(messages = emptyList()), fakeSignature = true)

        assertEquals(true, adapter.lastFakeSignature)
    }

    @Test
    fun `signedSignMessage allows null fakeSignature`() = runTest {
        val adapter = MockWalletAdapter()

        adapter.signedSignMessage(TONTransactionRequest(messages = emptyList()), fakeSignature = null)

        assertNull(adapter.lastFakeSignature)
    }

    @Test
    fun `signedSignMessage receives the supplied transaction request`() = runTest {
        val adapter = MockWalletAdapter()
        val tx = TONTransactionRequest(messages = emptyList())

        adapter.signedSignMessage(tx, fakeSignature = false)

        assertNotNull(adapter.lastTransactionRequest)
        assertEquals(tx, adapter.lastTransactionRequest)
    }

    @Test
    fun `signedSignMessage and signedSendTransaction are independent`() = runTest {
        val adapter = MockWalletAdapter()

        adapter.signedSendTransaction(TONTransactionRequest(messages = emptyList()), fakeSignature = false)
        adapter.signedSignMessage(TONTransactionRequest(messages = emptyList()), fakeSignature = false)

        assertEquals(1, adapter.signedSendTransactionCalls)
        assertEquals(1, adapter.signedSignMessageCalls)
    }

    @Test
    fun `adapter exposes identifier publicKey network address synchronously`() {
        val adapter = MockWalletAdapter()

        assertEquals("adapter-1", adapter.identifier())
        assertEquals("0xpub", adapter.publicKey().value)
        assertEquals("-239", adapter.network().chainId)
        assertNotNull(adapter.address(testnet = false))
    }

    @Test
    fun `supportedFeatures default returns null when not overridden`() {
        val adapter = MockWalletAdapter()
        assertNull(adapter.supportedFeatures())
    }

    @Test
    fun `signedSignMessage returns non-empty TONBase64 wrapper`() = runTest {
        val adapter = MockWalletAdapter()
        val result = adapter.signedSignMessage(TONTransactionRequest(messages = emptyList()), false)
        assertTrue(result.value.isNotEmpty())
    }
}
