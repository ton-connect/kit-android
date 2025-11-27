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
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Engine method efficiency tests - verifies that each SDK method call results in
 * exactly the expected number of bridge method calls.
 *
 * The principle: 1 SDK call = expected Engine/Bridge calls (no extra, no missing)
 *
 * SDK Method → Bridge Method Mapping:
 * - addEventsHandler() → init (first) + setEventsListeners (first)
 * - removeEventsHandler() → removeEventListeners (when all removed)
 * - createSignerFromMnemonic() → createSigner
 * - createSignerFromSecretKey() → createSigner
 * - createV5R1Adapter() → createAdapter
 * - createV4R2Adapter() → createAdapter
 * - addWallet() → addWallet
 * - getWallets() → getWallets
 * - getWallet() → getWallet
 * - removeWallet() → getWallet + removeWallet
 * - clearWallets() → getWallets + N×removeWallet
 * - createTonMnemonic() → createTonMnemonic
 * - mnemonicToKeyPair() → mnemonicToKeyPair
 * - sign() → sign
 * - handleNewTransaction() → handleNewTransaction
 * - disconnectSession() → disconnectSession
 */
@RunWith(AndroidJUnit4::class)
class EngineMethodEfficiencyTests : MockBridgeTestBase() {
    override fun getMockScenarioHtml(): String = "method-efficiency"
    override fun autoInitWalletKit(): Boolean = true
    override fun autoAddEventsHandler(): Boolean = false

    private suspend fun getMethodCallCount(method: String): Int {
        val result = engine.callBridgeMethod("getMethodCallCount", JSONObject().put("method", method))
        return result.optInt("count", 0)
    }

    private suspend fun resetMethodTracking() {
        engine.callBridgeMethod("resetMethodTracking", JSONObject())
    }

    // ===========================================
    // addEventsHandler / removeEventsHandler
    // ===========================================

    @Test
    fun addEventsHandler_callsInitOnce_andSetEventsListenersOnce() = runBlocking {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler)
        delay(200)

        assertEquals("init", 1, getMethodCallCount("init"))
        assertEquals("setEventsListeners", 1, getMethodCallCount("setEventsListeners"))
    }

    @Test
    fun removeEventsHandler_callsRemoveEventListenersOnce_whenAllRemoved() = runBlocking {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler)
        delay(150)
        resetMethodTracking()

        sdk.removeEventsHandler(handler)
        delay(150)

        assertEquals("removeEventListeners", 1, getMethodCallCount("removeEventListeners"))
    }

    // ===========================================
    // getWallets
    // ===========================================

    @Test
    fun getWallets_callsGetWalletsOnce() = runBlocking {
        resetMethodTracking()

        sdk.getWallets()
        delay(100)

        assertEquals("getWallets", 1, getMethodCallCount("getWallets"))
    }

    // ===========================================
    // getWallet
    // ===========================================

    @Test
    fun getWallet_callsGetWalletOnce() = runBlocking {
        resetMethodTracking()

        sdk.getWallet(TONUserFriendlyAddress("EQTest123"))
        delay(100)

        assertEquals("getWallet", 1, getMethodCallCount("getWallet"))
    }

    // ===========================================
    // createTonMnemonic
    // ===========================================

    @Test
    fun createTonMnemonic_callsCreateTonMnemonicOnce() = runBlocking {
        resetMethodTracking()

        try {
            sdk.createTonMnemonic()
        } catch (e: Exception) {
            // Mock may return error, but call should still be tracked
        }
        delay(100)

        assertEquals("createTonMnemonic", 1, getMethodCallCount("createTonMnemonic"))
    }

    // ===========================================
    // mnemonicToKeyPair
    // ===========================================

    @Test
    fun mnemonicToKeyPair_callsMnemonicToKeyPairOnce() = runBlocking {
        resetMethodTracking()

        val testMnemonic = listOf(
            "word1", "word2", "word3", "word4", "word5", "word6",
            "word7", "word8", "word9", "word10", "word11", "word12",
        )

        try {
            sdk.mnemonicToKeyPair(testMnemonic)
        } catch (e: Exception) {
            // Mock may return error
        }
        delay(100)

        assertEquals("mnemonicToKeyPair", 1, getMethodCallCount("mnemonicToKeyPair"))
    }

    // ===========================================
    // sign
    // ===========================================

    @Test
    fun sign_callsSignOnce() = runBlocking {
        resetMethodTracking()

        try {
            sdk.sign(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
        } catch (e: Exception) {
            // Mock may return error
        }
        delay(100)

        assertEquals("sign", 1, getMethodCallCount("sign"))
    }

    // ===========================================
    // disconnectSession
    // ===========================================

    @Test
    fun disconnectSession_callsDisconnectSessionOnce() = runBlocking {
        resetMethodTracking()

        try {
            sdk.disconnectSession("session-123")
        } catch (e: Exception) {
            // Mock may return error
        }
        delay(100)

        assertEquals("disconnectSession", 1, getMethodCallCount("disconnectSession"))
    }

    // ===========================================
    // createSignerFromMnemonic
    // ===========================================

    @Test
    fun createSignerFromMnemonic_callsCreateSignerOnce() = runBlocking {
        resetMethodTracking()

        val testMnemonic = listOf(
            "word1", "word2", "word3", "word4", "word5", "word6",
            "word7", "word8", "word9", "word10", "word11", "word12",
            "word13", "word14", "word15", "word16", "word17", "word18",
            "word19", "word20", "word21", "word22", "word23", "word24",
        )

        try {
            sdk.createSignerFromMnemonic(testMnemonic)
        } catch (e: Exception) {
            // Mock may return error
        }
        delay(100)

        assertEquals("createSigner", 1, getMethodCallCount("createSigner"))
    }

    // ===========================================
    // createSignerFromSecretKey
    // ===========================================

    @Test
    fun createSignerFromSecretKey_callsCreateSignerOnce() = runBlocking {
        resetMethodTracking()

        try {
            sdk.createSignerFromSecretKey(ByteArray(32) { it.toByte() })
        } catch (e: Exception) {
            // Mock may return error
        }
        delay(100)

        assertEquals("createSigner", 1, getMethodCallCount("createSigner"))
    }

    // ===========================================
    // Multiple calls verification
    // ===========================================

    @Test
    fun threeGetWalletsCalls_callsGetWalletsThreeTimes() = runBlocking {
        resetMethodTracking()

        sdk.getWallets()
        sdk.getWallets()
        sdk.getWallets()
        delay(150)

        assertEquals("getWallets should be called 3 times", 3, getMethodCallCount("getWallets"))
    }

    @Test
    fun multipleHandlers_callsSetEventsListenersOnlyOnce() = runBlocking {
        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler3 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler1)
        delay(100)
        sdk.addEventsHandler(handler2)
        delay(100)
        sdk.addEventsHandler(handler3)
        delay(100)

        assertEquals("setEventsListeners should be called only once", 1, getMethodCallCount("setEventsListeners"))
    }

    @Test
    fun partialHandlerRemoval_doesNotCallRemoveEventsListeners() = runBlocking {
        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)
        delay(150)
        resetMethodTracking()

        // Remove one, but one remains
        sdk.removeEventsHandler(handler1)
        delay(100)

        assertEquals("removeEventsListeners should NOT be called", 0, getMethodCallCount("removeEventsListeners"))
    }

    @Test
    fun multipleOperations_callsInitOnlyOnce() = runBlocking {
        val handler = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler) // triggers init
        delay(150)

        // All these should NOT trigger additional init
        sdk.getWallets()
        sdk.getWallet(TONUserFriendlyAddress("EQTest"))
        try {
            sdk.createTonMnemonic()
        } catch (e: Exception) {}
        try {
            sdk.disconnectSession("test")
        } catch (e: Exception) {}
        delay(150)

        assertEquals("init should be called only once", 1, getMethodCallCount("init"))
    }
}
