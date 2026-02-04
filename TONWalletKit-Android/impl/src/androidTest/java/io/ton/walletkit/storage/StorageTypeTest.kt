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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap

/**
 * Tests for the different storage types supported by TONWalletKit.
 *
 * These tests verify that all three storage types work correctly:
 * 1. Memory - ephemeral in-memory storage
 * 2. Encrypted - persistent secure storage
 * 3. Custom - user-provided storage implementation
 */
@RunWith(AndroidJUnit4::class)
class StorageTypeTest {

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
            // Clear cached WebView engines so each test gets a fresh instance
            launch { WebViewWalletKitEngine.clearInstances() }.join()
        }
    }

    // ==================== Memory Storage Tests ====================

    @Test
    fun sdkInitializesWithMemoryStorage() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createConfigWithStorageType(TONWalletKitStorageType.Memory)

                sdk = ITONWalletKit.initialize(context, config)
                assertNotNull("SDK should initialize with memory storage", sdk)
            }
        }
    }

    @Test
    fun memoryStorageAllowsWalletCreation() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createConfigWithStorageType(TONWalletKitStorageType.Memory)
                sdk = ITONWalletKit.initialize(context, config)

                val mnemonic = testMnemonic()
                val signer = sdk!!.createSignerFromMnemonic(mnemonic)
                val adapter = sdk!!.createV4R2Adapter(signer, TONNetwork.MAINNET)
                val wallet = sdk!!.addWallet(adapter.adapterId)

                assertNotNull("Wallet should be created with memory storage", wallet)
                assertNotNull("Wallet should have address", wallet.address)
            }
        }
    }

    // ==================== Encrypted Storage Tests ====================

    @Test
    fun sdkInitializesWithEncryptedStorage() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createConfigWithStorageType(TONWalletKitStorageType.Encrypted)

                sdk = ITONWalletKit.initialize(context, config)
                assertNotNull("SDK should initialize with encrypted storage", sdk)
            }
        }
    }

    @Test
    fun encryptedStorageAllowsWalletCreation() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val config = createConfigWithStorageType(TONWalletKitStorageType.Encrypted)
                sdk = ITONWalletKit.initialize(context, config)

                val mnemonic = testMnemonic()
                val signer = sdk!!.createSignerFromMnemonic(mnemonic)
                val adapter = sdk!!.createV4R2Adapter(signer, TONNetwork.MAINNET)
                val wallet = sdk!!.addWallet(adapter.adapterId)

                assertNotNull("Wallet should be created with encrypted storage", wallet)
                assertNotNull("Wallet should have address", wallet.address)
            }
        }
    }

    // ==================== Custom Storage Tests ====================

    @Test
    fun sdkInitializesWithCustomStorage() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val customStorage = InMemoryCustomStorage()
                val config = createConfigWithStorageType(
                    TONWalletKitStorageType.Custom(customStorage),
                )

                sdk = ITONWalletKit.initialize(context, config)
                assertNotNull("SDK should initialize with custom storage", sdk)
            }
        }
    }

    @Test
    fun customStorageAllowsWalletCreation() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val customStorage = InMemoryCustomStorage()
                val config = createConfigWithStorageType(
                    TONWalletKitStorageType.Custom(customStorage),
                )
                sdk = ITONWalletKit.initialize(context, config)

                val mnemonic = testMnemonic()
                val signer = sdk!!.createSignerFromMnemonic(mnemonic)
                val adapter = sdk!!.createV4R2Adapter(signer, TONNetwork.MAINNET)
                val wallet = sdk!!.addWallet(adapter.adapterId)

                assertNotNull("Wallet should be created with custom storage", wallet)
                assertNotNull("Wallet should have address", wallet.address)
            }
        }
    }

    /**
     * Test that custom storage is actually used by the SDK.
     *
     * Note: The SDK reads from storage during initialization (sessions, last_event_id)
     * but only writes when there are actual TON Connect sessions to persist.
     * Wallet data is kept in-memory and not persisted to this storage.
     */
    @Test
    fun customStorageReceivesDataFromSDK() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val customStorage = InMemoryCustomStorage()
                val config = createConfigWithStorageType(
                    TONWalletKitStorageType.Custom(customStorage),
                )
                sdk = ITONWalletKit.initialize(context, config)

                // SDK should have queried storage during initialization
                // (reading sessions and last_event_id)
                assertTrue(
                    "Custom storage should have been queried by SDK",
                    customStorage.getOperationCount() > 0,
                )
            }
        }
    }

    @Test
    fun customStorageCanPreloadData() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                // Pre-populate storage with some data
                val customStorage = InMemoryCustomStorage()
                customStorage.save("test-key", "test-value")

                val config = createConfigWithStorageType(
                    TONWalletKitStorageType.Custom(customStorage),
                )
                sdk = ITONWalletKit.initialize(context, config)

                // Verify pre-loaded data is still accessible
                assertEquals(
                    "Pre-loaded data should persist",
                    "test-value",
                    customStorage.get("test-key"),
                )
            }
        }
    }

    @Test
    fun customStorageClearWorks() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val customStorage = InMemoryCustomStorage()
                customStorage.save("key1", "value1")
                customStorage.save("key2", "value2")

                val config = createConfigWithStorageType(
                    TONWalletKitStorageType.Custom(customStorage),
                )
                sdk = ITONWalletKit.initialize(context, config)

                // Clear should remove all data
                customStorage.clear()

                assertNull("Data should be cleared", customStorage.get("key1"))
                assertNull("Data should be cleared", customStorage.get("key2"))
                assertTrue("Storage should be empty", customStorage.getAllKeys().isEmpty())
            }
        }
    }

    @Test
    fun customStorageRemoveWorks() = runBlocking {
        withContext(Dispatchers.Main) {
            withTimeout(60_000) {
                val customStorage = InMemoryCustomStorage()
                customStorage.save("key1", "value1")
                customStorage.save("key2", "value2")

                val config = createConfigWithStorageType(
                    TONWalletKitStorageType.Custom(customStorage),
                )
                sdk = ITONWalletKit.initialize(context, config)

                // Remove single key
                customStorage.remove("key1")

                assertNull("Removed key should return null", customStorage.get("key1"))
                assertEquals("Other key should remain", "value2", customStorage.get("key2"))
            }
        }
    }

    // ==================== Helper Classes ====================

    /**
     * In-memory implementation of TONWalletKitStorage for testing.
     */
    private class InMemoryCustomStorage : TONWalletKitStorage {
        private val storage = ConcurrentHashMap<String, String>()
        private var getCount = 0
        private var saveCount = 0

        override suspend fun get(key: String): String? {
            getCount++
            return storage[key]
        }

        override suspend fun save(key: String, value: String) {
            saveCount++
            storage[key] = value
        }

        override suspend fun remove(key: String) {
            storage.remove(key)
        }

        override suspend fun clear() {
            storage.clear()
        }

        fun getAllKeys(): Set<String> = storage.keys.toSet()
        fun getOperationCount(): Int = getCount
        fun saveOperationCount(): Int = saveCount
    }

    // ==================== Helper Methods ====================

    private fun createConfigWithStorageType(
        storageType: TONWalletKitStorageType,
    ): TONWalletKitConfiguration {
        return TONWalletKitConfiguration(
            networkConfigurations = setOf(
                TONWalletKitConfiguration.NetworkConfiguration(
                    network = TONNetwork.MAINNET,
                    apiClientConfiguration = TONWalletKitConfiguration.APIClientConfiguration(key = ""),
                ),
            ),
            walletManifest = TONWalletKitConfiguration.Manifest(
                name = "Storage Test Wallet",
                appName = "Wallet",
                imageUrl = "https://example.com/icon.png",
                aboutUrl = "https://example.com",
                universalLink = "https://example.com/tc",
                bridgeUrl = "https://bridge.tonapi.io/bridge",
            ),
            bridge = TONWalletKitConfiguration.Bridge(
                bridgeUrl = "https://bridge.tonapi.io/bridge",
            ),
            features = listOf(
                TONWalletKitConfiguration.SendTransactionFeature(maxMessages = 4),
                TONWalletKitConfiguration.SignDataFeature(
                    types = listOf(SignDataType.TEXT, SignDataType.BINARY, SignDataType.CELL),
                ),
            ),
            storageType = storageType,
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
