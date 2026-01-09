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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.TESTNET
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.WebViewWalletKitEngine
import io.ton.walletkit.event.TONWalletKitEvent
import io.ton.walletkit.listener.TONBridgeEventsHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * High-level SDK tests that demonstrate the canonical TON Wallet Kit API usage.
 *
 * These tests show how applications should use the new instance-based SDK:
 * 1. Initialize with ITONWalletKit.initialize()
 * 2. Add event handlers with addEventsHandler()
 * 3. Create wallets using the 3-step pattern: createSigner → createAdapter → addWallet
 * 4. Perform operations on wallet instances
 * 5. Clean up with destroy()
 *
 * Note: These tests initialize the SDK but do not perform full wallet operations
 * as that would require network access and longer timeouts suitable for integration tests.
 */
@RunWith(AndroidJUnit4::class)
class TONWalletKitSDKTest {

    private lateinit var context: Context
    private lateinit var sdk: ITONWalletKit
    private val events = mutableListOf<TONWalletKitEvent>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        events.clear()
    }

    @After
    fun tearDown() = runBlocking {
        withContext(Dispatchers.Main) {
            if (::sdk.isInitialized) {
                sdk.destroy()
            }
            // Clear cached WebView engines so each test gets a fresh instance
            launch { WebViewWalletKitEngine.clearInstances() }.join()
        }
    }

    @Test
    fun sdkInitializeWithMainnetConfiguration() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) { // 60 seconds for WebView initialization
                val config = TONWalletKitConfiguration(
                    network = TONNetwork.MAINNET,
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

                sdk = ITONWalletKit.initialize(context, config)
                assertNotNull("SDK should initialize successfully", sdk)
            }
        }
    }

    @Test
    fun sdkInitializeWithTestnetConfiguration() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = TONWalletKitConfiguration(
                    network = TONNetwork.TESTNET,
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
                            types = listOf(SignDataType.TEXT),
                        ),
                    ),
                    storage = TONWalletKitConfiguration.Storage(persistent = false),
                )

                sdk = ITONWalletKit.initialize(context, config)
                assertNotNull("SDK should initialize successfully", sdk)
            }
        }
    }

    @Test
    fun sdkSupportsEventHandlers() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createTestConfig()
                sdk = ITONWalletKit.initialize(context, config)

                val eventsHandler = object : TONBridgeEventsHandler {
                    override fun handle(event: TONWalletKitEvent) {
                        events.add(event)
                    }
                }

                // Add event handler
                sdk.addEventsHandler(eventsHandler)

                // Should not throw
                assertTrue("Event handler should be added successfully", true)

                // Remove event handler
                sdk.removeEventsHandler(eventsHandler)
            }
        }
    }

    @Test
    fun sdkCanCreateWalletWithMnemonic() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createTestConfig()
                sdk = ITONWalletKit.initialize(context, config)

                // Test mnemonic (standard BIP39)
                val mnemonic = listOf(
                    "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                    "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                    "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                    "abandon", "abandon", "abandon", "abandon", "abandon", "art",
                )

                // Create V4R2 wallet using 3-step pattern
                val signer = sdk.createSignerFromMnemonic(mnemonic)
                val adapter = sdk.createV4R2Adapter(signer, TONNetwork.MAINNET)
                val wallet = sdk.addWallet(adapter.adapterId)

                assertNotNull("Wallet should be created", wallet)
                assertNotNull("Wallet should have address", wallet.address)
                assertNotNull("Signer should have public key", signer.publicKey)
            }
        }
    }

    @Test
    fun sdkCanCreateV5R1Wallet() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createTestConfig()
                sdk = ITONWalletKit.initialize(context, config)

                // Test mnemonic
                val mnemonic = listOf(
                    "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                    "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                    "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                    "abandon", "abandon", "abandon", "abandon", "abandon", "art",
                )

                // Create V5R1 wallet using 3-step pattern
                val signer = sdk.createSignerFromMnemonic(mnemonic)
                val adapter = sdk.createV5R1Adapter(signer, TONNetwork.MAINNET)
                val wallet = sdk.addWallet(adapter.adapterId)

                assertNotNull("V5R1 wallet should be created", wallet)
                assertNotNull("Wallet should have address", wallet.address)
                assertNotNull("Signer should have public key", signer.publicKey)
            }
        }
    }

    @Test
    fun sdkCanBeDestroyed() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createTestConfig()
                sdk = ITONWalletKit.initialize(context, config)

                // Should not throw
                sdk.destroy()
                assertTrue("SDK should be destroyed successfully", true)
            }
        }
    }

    private fun createTestConfig(): TONWalletKitConfiguration {
        return TONWalletKitConfiguration(
            network = TONNetwork.MAINNET,
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
}
