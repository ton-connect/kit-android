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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import io.ton.walletkit.model.TONNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Base class for Mock JavaScript Bridge tests using PUBLIC SDK API + mock JavaScript behavior.
 *
 * This tests the REAL SDK (ITONWalletKit) exactly as users would use it, but with
 * mock JavaScript files that simulate various backend behaviors (edge cases, errors,
 * race conditions, etc.).
 *
 * The mock JavaScript sends controlled messages to test how the SDK handles abnormal
 * JS behavior: multiple ready events, unknown responses, timeouts, etc.
 *
 * Subclasses specify which mock scenario HTML to load by implementing getMockScenarioHtml().
 */
@RunWith(AndroidJUnit4::class)
abstract class MockBridgeTestBase {

    protected lateinit var context: Context
    protected lateinit var sdk: ITONWalletKit
    internal lateinit var engine: io.ton.walletkit.engine.WebViewWalletKitEngine

    // Track SDK events
    protected val events = mutableListOf<TONWalletKitEvent>()

    /**
     * Subclasses must return the HTML file name (without .html extension)
     * Example: "normal-flow", "multi-ready", "unknown-call-id"
     */
    abstract fun getMockScenarioHtml(): String

    @Before
    fun setupMockBridge() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Initialize REAL SDK using test factory with mock HTML path
        // Must run on Main thread for WebView
        withContext(Dispatchers.Main) {
            val mockAssetPath = "mock-bridge/${getMockScenarioHtml()}.html"

            val testInstance = createTestInstance(context, mockAssetPath)

            // Extract both SDK (for public API) and engine (for internal methods)
            sdk = testInstance.sdk
            engine = testInstance.engine

            // Add event handler like real users would
            if (autoAddEventsHandler()) {
                sdk.addEventsHandler(object : TONBridgeEventsHandler {
                    override fun handle(event: TONWalletKitEvent) {
                        events.add(event)
                    }
                })
            }
        }
    }

    /**
     * Idle the main looper to process WebView events
     */
    protected fun idleMainLooper() { // For AndroidJUnit4, we just add a small delay to let WebView process events
        Thread.sleep(50)
    }

    /**
     * Wait for a condition with timeout
     */
    protected suspend fun waitFor(
        timeoutSeconds: Int = 5,
        condition: () -> Boolean,
    ) {
        withTimeout(timeoutSeconds.seconds) {
            while (!condition()) {
                delay(50)
                idleMainLooper()
            }
        }
    }

    /**
     * Create a test configuration for SDK initialization
     */
    protected fun createTestConfig(
        network: TONNetwork = TONNetwork.TESTNET,
    ): TONWalletKitConfiguration {
        return TONWalletKitConfiguration(
            network = network,
            walletManifest = TONWalletKitConfiguration.Manifest(
                name = "Test Wallet",
                appName = "Wallet",
                imageUrl = "https://example.com/icon.png",
                aboutUrl = "https://example.com",
                universalLink = "https://example.com/tc",
                bridgeUrl = "https://bridge.tonapi.io/bridge",
            ),
            bridge = TONWalletKitConfiguration.Bridge(
                bridgeUrl = "https://bridge.tonapi.io/bridge",
            ),
            apiClient = null,
            features = listOf(
                TONWalletKitConfiguration.SendTransactionFeature(maxMessages = 4),
                TONWalletKitConfiguration.SignDataFeature(
                    types = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL),
                ),
            ),
            storage = TONWalletKitConfiguration.Storage(persistent = false),
        )
    }

    @After
    fun teardownMockBridge() = runBlocking {
        withContext(Dispatchers.Main) {
            if (::sdk.isInitialized) {
                sdk.destroy()
            }
        }
    }

    protected open fun autoAddEventsHandler(): Boolean = true
    protected open fun autoInitWalletKit(): Boolean = true
    internal open suspend fun createTestInstance(
        context: Context,
        mockAssetPath: String,
    ): TestWalletKitFactory.TestSDKInstance {
        return TestWalletKitFactory.createWithMockBridge(
            context = context,
            mockAssetPath = mockAssetPath,
            configuration = createTestConfig(),
            autoInit = autoInitWalletKit(),
        )
    }
}
