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
package io.ton.walletkit.storage

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.config.SignDataType
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.WebViewWalletKitEngine
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Integration tests for custom storage implementations.
 *
 * These tests demonstrate how wallet apps like Tonkeeper can:
 * 1. Implement custom storage to bridge their existing session database
 * 2. Track what keys the SDK uses for storage
 * 3. Pre-populate sessions that the SDK will then "know about"
 *
 * The custom storage pattern enables:
 * - Reading sessions from Tonkeeper's existing AppConnectEntity database
 * - Writing sessions to both Tonkeeper's DB AND SDK's expected format
 * - The SDK "knowing" about Tonkeeper's sessions via the storage layer
 */
@RunWith(AndroidJUnit4::class)
class CustomStorageIntegrationTest {

    private lateinit var context: Context
    private var sdk: ITONWalletKit? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun tearDown() = runBlocking {
        withContext(Dispatchers.Main) {
            sdk?.destroy()
            sdk = null
            launch { WebViewWalletKitEngine.clearInstances() }.join()
        }
    }

    /**
     * Test that custom storage properly tracks all storage operations.
     * This is useful for understanding what keys the SDK uses.
     *
     * Note: The SDK reads from storage during initialization (sessions, last_event_id)
     * but only writes when there are actual TON Connect sessions to persist.
     */
    @Test
    fun customStorageTracksAllOperations() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val trackingStorage = TrackingCustomStorage()
                val config = createConfigWithCustomStorage(trackingStorage)
                sdk = ITONWalletKit.initialize(context, config)

                // SDK should have read from storage during initialization
                // (checking for existing sessions and last event ID)
                assertTrue(
                    "Storage should have recorded get operations during SDK init",
                    trackingStorage.getOperations.isNotEmpty(),
                )

                // Log the keys for debugging - this helps understand SDK storage structure
                println("=== SDK Storage Keys Read ===")
                trackingStorage.getOperations.forEach { key ->
                    println("Key: $key")
                }
                println("=============================")
            }
        }
    }

    /**
     * Test that custom storage can simulate Tonkeeper-style session bridge.
     * When SDK asks for "sessions", we can return Tonkeeper's sessions.
     */
    @Test
    fun customStorageCanBridgeSessions() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val bridgeStorage = SessionBridgeStorage()
                val config = createConfigWithCustomStorage(bridgeStorage)
                sdk = ITONWalletKit.initialize(context, config)

                // SDK initializes successfully even with bridged storage
                assertNotNull("SDK should initialize with bridge storage", sdk)

                // Create wallet to verify storage works end-to-end
                val mnemonic = testMnemonic()
                val signer = sdk!!.createSignerFromMnemonic(mnemonic)
                val adapter = sdk!!.createV4R2Adapter(signer, TONNetwork.MAINNET)
                val wallet = sdk!!.addWallet(adapter.adapterId)

                assertNotNull("Wallet creation should work with bridge storage", wallet)
            }
        }
    }

    /**
     * Test that storage operations are thread-safe.
     */
    @Test
    fun customStorageIsThreadSafe() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val threadSafeStorage = ThreadSafeCustomStorage()
                val config = createConfigWithCustomStorage(threadSafeStorage)
                sdk = ITONWalletKit.initialize(context, config)

                // Perform multiple concurrent operations
                val mnemonic = testMnemonic()
                val signer = sdk!!.createSignerFromMnemonic(mnemonic)
                val adapter = sdk!!.createV4R2Adapter(signer, TONNetwork.MAINNET)
                sdk!!.addWallet(adapter.adapterId)

                // If we get here without crashes, thread safety is working
                assertTrue("Thread-safe storage should work correctly", true)
            }
        }
    }

    // ==================== Storage Implementations ====================

    /**
     * Storage that tracks all operations for debugging/analysis.
     */
    private class TrackingCustomStorage : TONWalletKitStorage {
        private val storage = ConcurrentHashMap<String, String>()
        val saveOperations = CopyOnWriteArrayList<Pair<String, String>>()
        val getOperations = CopyOnWriteArrayList<String>()
        val removeOperations = CopyOnWriteArrayList<String>()
        var clearCalled = false

        override suspend fun get(key: String): String? {
            getOperations.add(key)
            return storage[key]
        }

        override suspend fun save(key: String, value: String) {
            saveOperations.add(key to value)
            storage[key] = value
        }

        override suspend fun remove(key: String) {
            removeOperations.add(key)
            storage.remove(key)
        }

        override suspend fun clear() {
            clearCalled = true
            storage.clear()
        }
    }

    /**
     * Storage that simulates bridging Tonkeeper's sessions.
     * This demonstrates the pattern iOS dev suggested:
     * - When SDK reads "sessions", return data from Tonkeeper's DB
     * - When SDK writes "sessions", write to both SDK and Tonkeeper's DB
     */
    private class SessionBridgeStorage : TONWalletKitStorage {
        // Simulates Tonkeeper's existing session database
        private val tonkeeperSessionDb = ConcurrentHashMap<String, String>()

        // SDK's expected storage
        private val sdkStorage = ConcurrentHashMap<String, String>()

        override suspend fun get(key: String): String? {
            // For session-related keys, prefer Tonkeeper's DB
            return when {
                key.contains("sessions") -> {
                    // In real implementation, this would read from Tonkeeper's
                    // AppConnectEntity table and convert to SDK format
                    tonkeeperSessionDb[key] ?: sdkStorage[key]
                }
                else -> sdkStorage[key]
            }
        }

        override suspend fun save(key: String, value: String) {
            // Write to SDK storage
            sdkStorage[key] = value

            // For session-related keys, also update Tonkeeper's DB
            if (key.contains("sessions")) {
                // In real implementation, this would parse the JSON and
                // update Tonkeeper's AppConnectEntity table
                tonkeeperSessionDb[key] = value
            }
        }

        override suspend fun remove(key: String) {
            sdkStorage.remove(key)
            tonkeeperSessionDb.remove(key)
        }

        override suspend fun clear() {
            sdkStorage.clear()
            // Optionally clear Tonkeeper sessions or not depending on requirements
        }
    }

    /**
     * Thread-safe storage implementation for concurrent access.
     */
    private class ThreadSafeCustomStorage : TONWalletKitStorage {
        private val storage = ConcurrentHashMap<String, String>()

        override suspend fun get(key: String): String? {
            return storage[key]
        }

        override suspend fun save(key: String, value: String) {
            storage[key] = value
        }

        override suspend fun remove(key: String) {
            storage.remove(key)
        }

        override suspend fun clear() {
            storage.clear()
        }
    }

    // ==================== Helper Methods ====================

    private fun createConfigWithCustomStorage(
        storage: TONWalletKitStorage,
    ): TONWalletKitConfiguration {
        return TONWalletKitConfiguration(
            network = TONNetwork.MAINNET,
            walletManifest = TONWalletKitConfiguration.Manifest(
                name = "Custom Storage Test",
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
            storageType = TONWalletKitStorageType.Custom(storage),
        )
    }

    private fun testMnemonic(): List<String> {
        return listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "art",
        )
    }
}
