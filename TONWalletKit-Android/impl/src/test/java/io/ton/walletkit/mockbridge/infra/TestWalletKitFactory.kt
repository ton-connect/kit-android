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
package io.ton.walletkit.mockbridge.infra

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.core.TONWalletKit
import io.ton.walletkit.core.WalletKitEngineKind
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.event.DisconnectEvent
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import org.json.JSONObject

/**
 * Test-specific factory for creating TONWalletKit instances with mocked engine.
 *
 * This factory creates a mocked WalletKitEngine that simulates JavaScript bridge behavior
 * without requiring a WebView or actual JavaScript execution. Mock responses are provided
 * by a MockScenario that defines the behavior for each RPC method.
 *
 * **Internal test utility - not part of public SDK API.**
 */
internal object TestWalletKitFactory {

    /**
     * Test result containing both the public SDK interface and the mock scenario.
     */
    internal data class TestSDKInstance(
        val sdk: ITONWalletKit,
        val mockEngine: WalletKitEngine,
        val scenario: MockScenario,
    )

    /**
     * Create a TONWalletKit instance for testing with a mock scenario.
     *
     * Unlike the public `ITONWalletKit.initialize()`, this factory method creates
     * a mocked engine that doesn't require WebView or JavaScript execution.
     *
     * @param scenario Mock scenario that defines RPC method responses
     * @param configuration SDK configuration
     * @param eventsHandler Optional events handler to track SDK events
     * @param autoInit If true, calls mockEngine.init() during creation. If false, test must call sdk.init() manually.
     * @return TestSDKInstance with SDK and mock references
     */
    internal suspend fun createWithMockScenario(
        scenario: MockScenario,
        configuration: TONWalletKitConfiguration,
        eventsHandler: TONBridgeEventsHandler? = null,
        autoInit: Boolean = true,
    ): TestSDKInstance {
        val mockEngine = createMockEngine(scenario)

        // Initialize the mock engine only if autoInit is true
        if (autoInit) {
            mockEngine.init(configuration)
        }

        // Wrap in TONWalletKit (accessing internal constructor via reflection)
        val constructor = TONWalletKit::class.java.getDeclaredConstructor(
            WalletKitEngine::class.java,
        )
        constructor.isAccessible = true
        val sdk = constructor.newInstance(mockEngine) as ITONWalletKit

        // Add events handler if provided
        if (eventsHandler != null) {
            sdk.addEventsHandler(eventsHandler)
        }

        return TestSDKInstance(sdk, mockEngine, scenario)
    }

    /**
     * Creates a mocked WalletKitEngine that uses the provided scenario for responses.
     */
    private fun createMockEngine(scenario: MockScenario): WalletKitEngine {
        val mockEngine = mockk<WalletKitEngine>(relaxed = true)

        // Setup basic engine properties
        every { mockEngine.kind } returns WalletKitEngineKind.WEBVIEW
        every { mockEngine.getConfiguration() } returns null

        // Track handlers for event dispatch (thread-safe for concurrent registration)
        val handlers = java.util.concurrent.CopyOnWriteArrayList<TONBridgeEventsHandler>()
        coEvery { mockEngine.addEventsHandler(any()) } answers {
            handlers.add(firstArg())
        }
        coEvery { mockEngine.removeEventsHandler(any()) } answers {
            handlers.remove(firstArg())
        }

        // Setup init - delegate to scenario's handleInit
        coEvery { mockEngine.init(any()) } coAnswers {
            val config = firstArg<TONWalletKitConfiguration>()
            scenario.handleInit(config)
            every { mockEngine.getConfiguration() } returns config
        }

        // Setup createTonMnemonic
        coEvery { mockEngine.createTonMnemonic(any()) } answers {
            scenario.handleCreateTonMnemonic(firstArg())
        }

        // Setup createSignerFromMnemonic
        coEvery { mockEngine.createSignerFromMnemonic(any(), any()) } answers {
            scenario.handleCreateSignerFromMnemonic(firstArg(), secondArg())
        }

        // Setup createV5R1Adapter
        coEvery { mockEngine.createV5R1Adapter(any(), any(), any(), any(), any(), any()) } answers {
            scenario.handleCreateV5R1Adapter(
                signerId = firstArg(),
                network = secondArg(),
                workchain = thirdArg(),
                walletId = arg(3),
                publicKey = arg(4),
                isCustom = arg(5),
            )
        }

        // Setup createV4R2Adapter
        coEvery { mockEngine.createV4R2Adapter(any(), any(), any(), any(), any(), any()) } answers {
            scenario.handleCreateV4R2Adapter(
                signerId = firstArg(),
                network = secondArg(),
                workchain = thirdArg(),
                walletId = arg(3),
                publicKey = arg(4),
                isCustom = arg(5),
            )
        }

        // Setup addWallet
        coEvery { mockEngine.addWallet(any()) } answers {
            scenario.handleAddWallet(firstArg())
        }

        // Setup getWallets
        coEvery { mockEngine.getWallets() } answers {
            scenario.handleGetWallets()
        }

        // Setup getNfts
        coEvery { mockEngine.getNfts(any(), any(), any()) } answers {
            scenario.handleGetNfts(firstArg(), secondArg(), thirdArg())
        }

        // Setup disconnectSession - dispatch disconnect event to handlers
        coEvery { mockEngine.disconnectSession(any()) } answers {
            val sessionId = firstArg<String?>()
            val event = TONWalletKitEvent.Disconnect(
                event = DisconnectEvent(
                    sessionId = sessionId,
                    reason = "disconnected",
                ),
            )
            // Dispatch to all registered handlers
            handlers.toList().forEach { handler ->
                handler.handle(event)
            }
        }

        // Setup callBridgeMethod for generic RPC calls
        val methodSlot = slot<String>()
        val paramsSlot = slot<JSONObject?>()
        coEvery {
            mockEngine.callBridgeMethod(
                capture(methodSlot),
                captureNullable(paramsSlot),
            )
        } answers {
            scenario.handleRpcCall(methodSlot.captured, paramsSlot.captured)
        }

        return mockEngine
    }
}
