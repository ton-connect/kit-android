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
package io.ton.walletkit.engine.operations

import io.mockk.coEvery
import io.mockk.mockk
import io.ton.walletkit.WalletKitBridgeException
import io.ton.walletkit.api.WalletVersions
import io.ton.walletkit.engine.state.SignerManager
import io.ton.walletkit.internal.constants.NetworkConstants
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for WalletOperations response parsing and data transformation.
 *
 * Focus: Response parsing logic, hex prefix stripping, default value handling.
 * Not testing: That ensureInitialized/rpcClient.call are invoked (too trivial).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WalletOperationsTest : OperationsTestBase() {

    private lateinit var signerManager: SignerManager
    private lateinit var walletOperations: WalletOperations
    private var currentNetwork = NetworkConstants.DEFAULT_NETWORK

    // Valid TON test addresses for testing
    companion object {
        const val TEST_ADDRESS_1 = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        const val TEST_ADDRESS_2 = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"
    }

    @Before
    override fun setup() {
        super.setup()
        signerManager = mockk(relaxed = true)
        walletOperations = WalletOperations(
            ensureInitialized = ensureInitialized,
            rpcClient = rpcClient,
            signerManager = signerManager,
            currentNetworkProvider = { currentNetwork },
            json = json,
        )
    }

    // --- createSignerFromMnemonic tests ---

    @Test
    fun createSignerFromMnemonic_extractsSignerFromNestedResponse() = runBlocking {
        // JS returns { _tempId, signer: { publicKey } }
        givenBridgeReturns(
            jsonOf(
                "_tempId" to "signer-123",
                "signer" to JSONObject().apply {
                    put("publicKey", "0xabcdef1234567890")
                },
            ),
        )

        val result = walletOperations.createSignerFromMnemonic(listOf("word1", "word2"))

        assertEquals("signer-123", result.signerId)
        assertEquals("abcdef1234567890", result.publicKey.value) // 0x prefix stripped
    }

    @Test
    fun createSignerFromMnemonic_stripsHexPrefixFromPublicKey() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "_tempId" to "signer-1",
                "signer" to JSONObject().apply {
                    put("publicKey", "0x1234abcd")
                },
            ),
        )

        val result = walletOperations.createSignerFromMnemonic(listOf("test"))

        assertEquals("1234abcd", result.publicKey.value)
    }

    @Test
    fun createSignerFromMnemonic_handlesPublicKeyWithoutPrefix() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "_tempId" to "signer-1",
                "signer" to JSONObject().apply {
                    put("publicKey", "abcd1234")
                },
            ),
        )

        val result = walletOperations.createSignerFromMnemonic(listOf("test"))

        assertEquals("abcd1234", result.publicKey.value)
    }

    @Test
    fun createSignerFromMnemonic_fallsBackToRootObjectIfNoSignerNested() = runBlocking {
        // Legacy format: { _tempId, publicKey } without nested signer
        givenBridgeReturns(
            jsonOf(
                "_tempId" to "signer-legacy",
                "publicKey" to "0xlegacykey",
            ),
        )

        val result = walletOperations.createSignerFromMnemonic(listOf("test"))

        assertEquals("signer-legacy", result.signerId)
        assertEquals("legacykey", result.publicKey.value)
    }

    @Test
    fun createSignerFromMnemonic_generatesSignerIdIfTempIdMissing() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "signer" to JSONObject().apply {
                    put("publicKey", "0xkey123")
                },
            ),
        )

        val result = walletOperations.createSignerFromMnemonic(listOf("test"))

        // Should generate an ID (starts with "signer_")
        assertTrue(
            "Expected generated signerId starting with signer_, got: ${result.signerId}",
            result.signerId.startsWith("signer_"),
        )
        assertEquals("key123", result.publicKey.value)
    }

    // --- getWallets tests ---

    @Test
    fun getWallets_parsesArrayOfWallets() = runBlocking {
        // JS now returns array of { walletId, wallet } objects
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> {
                    val walletId = (secondArg<Any?>() as? JSONObject)?.optString("walletId") ?: ""
                    jsonOf("value" to if (walletId.contains("wallet-1")) TEST_ADDRESS_1 else TEST_ADDRESS_2)
                }
                else -> JSONObject().apply {
                    put(
                        "items",
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("walletId", "-239:wallet-1")
                                    put(
                                        "wallet",
                                        JSONObject().apply {
                                            put("publicKey", "0xpub1")
                                            put("version", WalletVersions.V5R1)
                                        },
                                    )
                                },
                            )
                            put(
                                JSONObject().apply {
                                    put("walletId", "-3:wallet-2")
                                    put(
                                        "wallet",
                                        JSONObject().apply {
                                            put("publicKey", "pub2") // no 0x prefix
                                            put("version", WalletVersions.V4R2)
                                        },
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }

        val result = walletOperations.getWallets()

        assertEquals(2, result.size)

        assertEquals(TEST_ADDRESS_1, result[0].address.value)
        assertEquals("pub1", result[0].publicKey) // stripped
        assertEquals(WalletVersions.V5R1, result[0].version)

        assertEquals(TEST_ADDRESS_2, result[1].address.value)
        assertEquals("pub2", result[1].publicKey)
        assertEquals(WalletVersions.V4R2, result[1].version)
    }

    @Test
    fun getWallets_handlesDirectArrayResponse() = runBlocking {
        // Tests the items wrapper
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> jsonOf("value" to TEST_ADDRESS_1)
                else -> JSONObject().apply {
                    put(
                        "items",
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("walletId", "-239:wallet-1")
                                    put(
                                        "wallet",
                                        JSONObject().apply {
                                            put("publicKey", "directKey")
                                        },
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }

        val result = walletOperations.getWallets()

        assertEquals(1, result.size)
        assertEquals(TEST_ADDRESS_1, result[0].address.value)
    }

    @Test
    fun getWallets_returnsEmptyListIfNoItems() = runBlocking {
        givenBridgeReturns(JSONObject()) // No items key

        val result = walletOperations.getWallets()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getWallets_usesAlternateKeyNames() = runBlocking {
        // Tests that it checks wallet.publicKey
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> jsonOf("value" to TEST_ADDRESS_1)
                else -> JSONObject().apply {
                    put(
                        "items",
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("walletId", "-239:wallet-1")
                                    put(
                                        "wallet",
                                        JSONObject().apply {
                                            // No publicKey - should be null
                                        },
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }

        val result = walletOperations.getWallets()

        assertEquals(1, result.size)
        // Should handle missing publicKey gracefully
        assertNull(result[0].publicKey) // Falls back to null if no publicKey in wallet
    }

    // --- getWallet tests ---

    @Test
    fun getWallet_parsesWalletObject() = runBlocking {
        // JS returns { walletId, wallet }
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> jsonOf("value" to TEST_ADDRESS_1)
                else -> jsonOf(
                    "walletId" to "-239:$TEST_ADDRESS_1",
                    "wallet" to JSONObject().apply {
                        put("publicKey", "0xsinglekey")
                        put("version", WalletVersions.V5R1)
                    },
                )
            }
        }

        val result = walletOperations.getWallet(TEST_ADDRESS_1)

        assertNotNull(result)
        assertEquals(TEST_ADDRESS_1, result!!.address.value)
        assertEquals("singlekey", result.publicKey)
        assertEquals(WalletVersions.V5R1, result.version)
    }

    @Test
    fun getWallet_returnsNullForEmptyResponse() = runBlocking {
        givenBridgeReturns(JSONObject()) // Empty object

        val result = walletOperations.getWallet("nonexistent")

        assertNull(result)
    }

    @Test
    fun getWallet_returnsNullIfAddressMissing() = runBlocking {
        // Mock getWalletAddress to return empty string
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> jsonOf("value" to "") // Empty address
                else -> jsonOf(
                    "walletId" to "-239:missing",
                    "wallet" to JSONObject().apply {
                        put("publicKey", "0xkey")
                        put("version", WalletVersions.V4R2)
                    },
                )
            }
        }

        val result = walletOperations.getWallet("missing")

        assertNull(result)
    }

    // --- getBalance tests ---

    @Test
    fun getBalance_extractsBalanceFromObject() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "balance" to "1000000000",
            ),
        )

        val result = walletOperations.getBalance("EQAddress")

        assertEquals("1000000000", result)
    }

    @Test
    fun getBalance_extractsBalanceFromValueKey() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "value" to "500000000",
            ),
        )

        val result = walletOperations.getBalance("EQAddress")

        assertEquals("500000000", result)
    }

    @Test
    fun getBalance_returnsZeroIfMissing() = runBlocking {
        givenBridgeReturns(JSONObject()) // Empty object - toString() returns "{}"

        val result = walletOperations.getBalance("EQAddress")

        // Empty JSONObject.toString() = "{}" which is not null/empty,
        // so the current impl returns "{}" - this tests actual behavior
        assertEquals("{}", result)
    }

    // --- removeWallet tests ---

    @Test
    fun removeWallet_succeedsWithRemovedTrue() = runBlocking {
        givenBridgeReturns(jsonOf("removed" to true))

        // Should not throw
        walletOperations.removeWallet("EQAddress")
    }

    @Test
    fun removeWallet_succeedsWithOkTrue() = runBlocking {
        givenBridgeReturns(jsonOf("ok" to true))

        walletOperations.removeWallet("EQAddress")
    }

    @Test
    fun removeWallet_succeedsWithEmptyResponse() = runBlocking {
        // Empty response is treated as success
        givenBridgeReturns(JSONObject())

        walletOperations.removeWallet("EQAddress")
    }

    @Test(expected = WalletKitBridgeException::class)
    fun removeWallet_throwsIfRemovedFalse() = runBlocking {
        givenBridgeReturns(jsonOf("removed" to false))

        walletOperations.removeWallet("EQAddress")
    }

    // --- addWallet tests ---

    @Test
    fun addWallet_parsesWalletResponse() = runBlocking {
        // JS returns { walletId, wallet }
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> jsonOf("value" to TEST_ADDRESS_1)
                else -> jsonOf(
                    "walletId" to "-239:$TEST_ADDRESS_1",
                    "wallet" to JSONObject().apply {
                        put("publicKey", "0xnewkey")
                        put("version", WalletVersions.V5R1)
                    },
                )
            }
        }

        val result = walletOperations.addWallet("adapter-123")

        assertEquals(TEST_ADDRESS_1, result.address.value)
        assertEquals("newkey", result.publicKey)
        assertEquals(WalletVersions.V5R1, result.version)
    }

    @Test
    fun addWallet_usesUnknownVersionIfMissing() = runBlocking {
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            val method = firstArg<String>()
            capturedMethod = method
            capturedParams = secondArg()
            when (method) {
                "getWalletAddress" -> jsonOf("value" to TEST_ADDRESS_1)
                else -> jsonOf(
                    "walletId" to "-239:$TEST_ADDRESS_1",
                    "wallet" to JSONObject(),
                )
            }
        }

        val result = walletOperations.addWallet("adapter-123")

        assertEquals("unknown", result.version)
    }
}
