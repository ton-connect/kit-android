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

import android.content.Context
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.core.TONWalletKit
import io.ton.walletkit.engine.WebViewWalletKitEngine
import io.ton.walletkit.listener.TONBridgeEventsHandler

/**
 * Test-specific factory for creating TONWalletKit instances with mock JavaScript bridges.
 *
 * This allows tests to inject custom HTML/JS files that simulate abnormal backend behavior
 * (rapid responses, errors, race conditions, etc.) without modifying the public API.
 *
 * **Internal test utility - not part of public SDK API.**
 */
internal object TestWalletKitFactory {
    /**
     * Test result containing both the public SDK interface and internal engine.
     * The engine is exposed for tests that need to call engine-specific methods.
     */
    internal data class TestSDKInstance(
        val sdk: ITONWalletKit,
        val engine: WebViewWalletKitEngine,
    )

    /**
     * Create a TONWalletKit instance for testing with a custom mock JavaScript bridge.
     *
     * Unlike the public `ITONWalletKit.initialize()`, this factory method allows specifying
     * a custom asset path to load test-specific HTML/JS files instead of the production bridge.
     *
     * @param context Android context
     * @param mockAssetPath Path to the test HTML file (e.g., "mock-bridge/rapid-rpc.html")
     * @param configuration SDK configuration (will be initialized after creation)
     * @param eventsHandler Optional events handler to track SDK events
     * @return TestSDKInstance with both SDK and engine references
     *
     * @sample
     * ```kotlin
     * val (sdk, engine) = TestWalletKitFactory.createWithMockBridge(
     *     context = context,
     *     mockAssetPath = "mock-bridge/rapid-rpc.html",
     *     configuration = createTestConfig()
     * )
     * sdk.addEventsHandler(myHandler)
     * val mnemonic = sdk.generateMnemonic()
     * // Or use engine for internal methods
     * engine.init(createTestConfig())
     * ```
     */
    internal suspend fun createWithMockBridge(
        context: Context,
        mockAssetPath: String,
        configuration: TONWalletKitConfiguration,
        eventsHandler: TONBridgeEventsHandler? = null,
        autoInit: Boolean = true,
    ): TestSDKInstance {
        // Create test engine with custom asset path
        val engine = WebViewWalletKitEngine.createForTesting(
            context = context,
            assetPath = mockAssetPath,
            eventsHandler = eventsHandler,
        )

        // Initialize the engine with configuration
        if (autoInit) {
            engine.init(configuration)
        }

        // Wrap in TONWalletKit (accessing internal constructor via reflection)
        val constructor = TONWalletKit::class.java.getDeclaredConstructor(
            io.ton.walletkit.engine.WalletKitEngine::class.java,
        )
        constructor.isAccessible = true
        val sdk = constructor.newInstance(engine) as ITONWalletKit

        return TestSDKInstance(sdk, engine)
    }
}
