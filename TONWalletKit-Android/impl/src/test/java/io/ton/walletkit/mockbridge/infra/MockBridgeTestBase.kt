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

import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.WalletKitEngine
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base class for Mock Bridge tests using mocked WalletKitEngine.
 *
 * This tests the SDK behavior by mocking the engine layer that normally
 * communicates with JavaScript. Instead of loading HTML/JS files, tests
 * define a MockScenario that provides responses for RPC calls.
 *
 * Subclasses must implement getMockScenario() to define the test behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
abstract class MockBridgeTestBase {

    protected lateinit var sdk: ITONWalletKit
    internal lateinit var mockEngine: WalletKitEngine
    protected lateinit var scenario: MockScenario

    // Track SDK events
    protected val events = mutableListOf<TONWalletKitEvent>()

    /**
     * Subclasses must return the mock scenario that defines RPC responses.
     */
    abstract fun getMockScenario(): MockScenario

    @Before
    fun setupMockBridge() = runBlocking {
        scenario = getMockScenario()

        val testInstance = TestWalletKitFactory.createWithMockScenario(
            scenario = scenario,
            configuration = createTestConfig(),
            eventsHandler = if (autoAddEventsHandler()) {
                object : TONBridgeEventsHandler {
                    override fun handle(event: TONWalletKitEvent) {
                        events.add(event)
                    }
                }
            } else {
                null
            },
            autoInit = autoInitWalletKit(),
        )

        sdk = testInstance.sdk
        mockEngine = testInstance.mockEngine
    }

    /**
     * Create a test configuration for SDK initialization
     */
    protected fun createTestConfig(
        network: TONNetwork = TONNetwork(chainId = "-3"),
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
        if (::sdk.isInitialized && !skipTeardownDestroy) {
            sdk.destroy()
        }
        events.clear()
    }

    protected open fun autoAddEventsHandler(): Boolean = true

    /**
     * Override to return false if the test should not auto-initialize the SDK.
     * When false, the test must call sdk.init() manually.
     */
    protected open fun autoInitWalletKit(): Boolean = true

    /**
     * Set to true in tests that manually call sdk.destroy() to prevent
     * double-destroy. Reset after each test.
     */
    protected var skipTeardownDestroy: Boolean = false
}
