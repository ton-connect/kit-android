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

import io.ton.walletkit.api.generated.TONNFTsResponse
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.engine.model.WalletAccount
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.mockbridge.infra.DefaultMockScenario
import io.ton.walletkit.mockbridge.infra.MockBridgeTestBase
import io.ton.walletkit.mockbridge.infra.MockScenario
import io.ton.walletkit.model.WalletAdapterInfo
import io.ton.walletkit.model.WalletSignerInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Engine method efficiency tests - verifies that each SDK method call results in
 * exactly the expected number of engine method calls.
 *
 * The principle: 1 SDK call = 1 Engine call (no extra, no missing)
 *
 * With the mocked engine approach, we track method calls directly in the scenario
 * instead of querying a bridge method count.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class EngineMethodEfficiencyTests : MockBridgeTestBase() {

    override fun getMockScenario(): MockScenario = MethodTrackingScenario()
    override fun autoAddEventsHandler(): Boolean = false

    /**
     * Scenario that tracks all method calls for efficiency verification.
     */
    class MethodTrackingScenario : DefaultMockScenario() {
        val createTonMnemonicCount = AtomicInteger(0)
        val createSignerFromMnemonicCount = AtomicInteger(0)
        val createV5R1AdapterCount = AtomicInteger(0)
        val createV4R2AdapterCount = AtomicInteger(0)
        val addWalletCount = AtomicInteger(0)
        val getWalletsCount = AtomicInteger(0)
        val getNftsCount = AtomicInteger(0)

        fun resetCounts() {
            createTonMnemonicCount.set(0)
            createSignerFromMnemonicCount.set(0)
            createV5R1AdapterCount.set(0)
            createV4R2AdapterCount.set(0)
            addWalletCount.set(0)
            getWalletsCount.set(0)
            getNftsCount.set(0)
        }

        override fun handleCreateTonMnemonic(wordCount: Int): List<String> {
            createTonMnemonicCount.incrementAndGet()
            return super.handleCreateTonMnemonic(wordCount)
        }

        override fun handleCreateSignerFromMnemonic(mnemonic: List<String>, mnemonicType: String): WalletSignerInfo {
            createSignerFromMnemonicCount.incrementAndGet()
            return super.handleCreateSignerFromMnemonic(mnemonic, mnemonicType)
        }

        override fun handleCreateV5R1Adapter(
            signerId: String,
            network: TONNetwork?,
            workchain: Int,
            walletId: Long,
            publicKey: String?,
            isCustom: Boolean,
        ): WalletAdapterInfo {
            createV5R1AdapterCount.incrementAndGet()
            return super.handleCreateV5R1Adapter(signerId, network, workchain, walletId, publicKey, isCustom)
        }

        override fun handleCreateV4R2Adapter(
            signerId: String,
            network: TONNetwork?,
            workchain: Int,
            walletId: Long,
            publicKey: String?,
            isCustom: Boolean,
        ): WalletAdapterInfo {
            createV4R2AdapterCount.incrementAndGet()
            return super.handleCreateV4R2Adapter(signerId, network, workchain, walletId, publicKey, isCustom)
        }

        override fun handleAddWallet(adapterId: String): WalletAccount {
            addWalletCount.incrementAndGet()
            return super.handleAddWallet(adapterId)
        }

        override fun handleGetWallets(): List<WalletAccount> {
            getWalletsCount.incrementAndGet()
            return super.handleGetWallets()
        }

        override fun handleGetNfts(walletAddress: String, limit: Int, offset: Int): TONNFTsResponse {
            getNftsCount.incrementAndGet()
            return super.handleGetNfts(walletAddress, limit, offset)
        }
    }

    private val trackingScenario: MethodTrackingScenario
        get() = scenario as MethodTrackingScenario

    // ===========================================
    // createTonMnemonic
    // ===========================================

    @Test
    fun `createTonMnemonic - calls engine method once`() = runTest {
        trackingScenario.resetCounts()

        sdk.createTonMnemonic()

        assertEquals("createTonMnemonic should be called once", 1, trackingScenario.createTonMnemonicCount.get())
    }

    @Test
    fun `createTonMnemonic - three calls result in three engine calls`() = runTest {
        trackingScenario.resetCounts()

        sdk.createTonMnemonic()
        sdk.createTonMnemonic()
        sdk.createTonMnemonic()

        assertEquals("createTonMnemonic should be called 3 times", 3, trackingScenario.createTonMnemonicCount.get())
    }

    // ===========================================
    // createSignerFromMnemonic
    // ===========================================

    @Test
    fun `createSignerFromMnemonic - calls engine method once`() = runTest {
        trackingScenario.resetCounts()

        val mnemonic = sdk.createTonMnemonic()
        trackingScenario.resetCounts() // Reset after mnemonic generation

        sdk.createSignerFromMnemonic(mnemonic)

        assertEquals("createSignerFromMnemonic should be called once", 1, trackingScenario.createSignerFromMnemonicCount.get())
    }

    // ===========================================
    // createV5R1Adapter
    // ===========================================

    @Test
    fun `createV5R1Adapter - calls engine method once`() = runTest {
        trackingScenario.resetCounts()

        val mnemonic = sdk.createTonMnemonic()
        val signer = sdk.createSignerFromMnemonic(mnemonic)
        trackingScenario.resetCounts() // Reset before test

        sdk.createV5R1Adapter(signer)

        assertEquals("createV5R1Adapter should be called once", 1, trackingScenario.createV5R1AdapterCount.get())
    }

    // ===========================================
    // createV4R2Adapter
    // ===========================================

    @Test
    fun `createV4R2Adapter - calls engine method once`() = runTest {
        trackingScenario.resetCounts()

        val mnemonic = sdk.createTonMnemonic()
        val signer = sdk.createSignerFromMnemonic(mnemonic)
        trackingScenario.resetCounts() // Reset before test

        sdk.createV4R2Adapter(signer)

        assertEquals("createV4R2Adapter should be called once", 1, trackingScenario.createV4R2AdapterCount.get())
    }

    // ===========================================
    // addWallet
    // ===========================================

    @Test
    fun `addWallet - calls engine method once`() = runTest {
        trackingScenario.resetCounts()

        val mnemonic = sdk.createTonMnemonic()
        val signer = sdk.createSignerFromMnemonic(mnemonic)
        val adapter = sdk.createV5R1Adapter(signer)
        trackingScenario.resetCounts() // Reset before test

        sdk.addWallet(adapter.adapterId)

        assertEquals("addWallet should be called once", 1, trackingScenario.addWalletCount.get())
    }

    // ===========================================
    // getWallets
    // ===========================================

    @Test
    fun `getWallets - calls engine method once`() = runTest {
        trackingScenario.resetCounts()

        sdk.getWallets()

        assertEquals("getWallets should be called once", 1, trackingScenario.getWalletsCount.get())
    }

    @Test
    fun `getWallets - three calls result in three engine calls`() = runTest {
        trackingScenario.resetCounts()

        sdk.getWallets()
        sdk.getWallets()
        sdk.getWallets()

        assertEquals("getWallets should be called 3 times", 3, trackingScenario.getWalletsCount.get())
    }

    // ===========================================
    // Full wallet creation flow
    // ===========================================

    @Test
    fun `full wallet creation - each step calls engine once`() = runTest {
        trackingScenario.resetCounts()

        // Step 1: Create mnemonic
        val mnemonic = sdk.createTonMnemonic()
        assertEquals("createTonMnemonic should be called once", 1, trackingScenario.createTonMnemonicCount.get())

        // Step 2: Create signer
        val signer = sdk.createSignerFromMnemonic(mnemonic)
        assertEquals("createSignerFromMnemonic should be called once", 1, trackingScenario.createSignerFromMnemonicCount.get())

        // Step 3: Create adapter
        val adapter = sdk.createV5R1Adapter(signer)
        assertEquals("createV5R1Adapter should be called once", 1, trackingScenario.createV5R1AdapterCount.get())

        // Step 4: Add wallet
        sdk.addWallet(adapter.adapterId)
        assertEquals("addWallet should be called once", 1, trackingScenario.addWalletCount.get())

        // Total: 4 distinct engine calls, each called exactly once
    }

    // ===========================================
    // Event handler efficiency
    // ===========================================

    @Test
    fun `multiple event handlers - no extra engine calls`() = runTest {
        trackingScenario.resetCounts()

        val handler1 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }
        val handler2 = object : TONBridgeEventsHandler {
            override fun handle(event: TONWalletKitEvent) {}
        }

        sdk.addEventsHandler(handler1)
        sdk.addEventsHandler(handler2)

        // Now make an SDK call - should only result in one engine call
        sdk.getWallets()

        assertEquals("getWallets should be called once regardless of handlers", 1, trackingScenario.getWalletsCount.get())
    }
}
