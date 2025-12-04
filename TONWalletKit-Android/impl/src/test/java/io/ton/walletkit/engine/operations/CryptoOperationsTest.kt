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

import io.ton.walletkit.WalletKitBridgeException
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
 * Tests for CryptoOperations response parsing.
 *
 * Focus: Mnemonic parsing, key pair extraction, signature handling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class CryptoOperationsTest : OperationsTestBase() {

    private lateinit var cryptoOperations: CryptoOperations

    @Before
    override fun setup() {
        super.setup()
        cryptoOperations = CryptoOperations(
            ensureInitialized = ensureInitialized,
            rpcClient = rpcClient,
            json = json,
        )
    }

    // --- createTonMnemonic tests ---

    @Test
    fun createTonMnemonic_parsesItemsArray() = runBlocking {
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "items",
                    JSONArray().apply {
                        put("word1")
                        put("word2")
                        put("word3")
                    },
                )
            },
        )

        val result = cryptoOperations.createTonMnemonic(3)

        assertEquals(3, result.size)
        assertEquals("word1", result[0])
        assertEquals("word2", result[1])
        assertEquals("word3", result[2])
    }

    @Test
    fun createTonMnemonic_returnsEmptyListIfNoItems() = runBlocking {
        givenBridgeReturns(JSONObject()) // No items key

        val result = cryptoOperations.createTonMnemonic(12)

        assertTrue(result.isEmpty())
    }

    @Test
    fun createTonMnemonic_handles24Words() = runBlocking {
        val words = (1..24).map { "word$it" }
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "items",
                    JSONArray().apply {
                        words.forEach { put(it) }
                    },
                )
            },
        )

        val result = cryptoOperations.createTonMnemonic(24)

        assertEquals(24, result.size)
        assertEquals("word1", result[0])
        assertEquals("word24", result[23])
    }

    // --- mnemonicToKeyPair tests ---

    @Test
    fun mnemonicToKeyPair_parsesKeyPairFromArrays() = runBlocking {
        // Keys as JSONArray (Uint8Array serialization)
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "publicKey",
                    JSONArray().apply {
                        repeat(32) { put(it) } // 0, 1, 2, ..., 31
                    },
                )
                put(
                    "secretKey",
                    JSONArray().apply {
                        repeat(64) { put(it + 100) } // 100, 101, ..., 163
                    },
                )
            },
        )

        val result = cryptoOperations.mnemonicToKeyPair(listOf("test", "words"))

        assertEquals(32, result.publicKey.size)
        assertEquals(64, result.secretKey.size)
        assertEquals(0.toByte(), result.publicKey[0])
        assertEquals(31.toByte(), result.publicKey[31])
        assertEquals(100.toByte(), result.secretKey[0])
    }

    @Test
    fun mnemonicToKeyPair_parsesKeyPairFromIndexedObject() = runBlocking {
        // Keys as JSONObject with indexed keys (alternative Uint8Array serialization)
        givenBridgeReturns(
            JSONObject().apply {
                put(
                    "publicKey",
                    JSONObject().apply {
                        repeat(32) { put(it.toString(), it * 2) }
                    },
                )
                put(
                    "secretKey",
                    JSONObject().apply {
                        repeat(64) { put(it.toString(), it + 50) }
                    },
                )
            },
        )

        val result = cryptoOperations.mnemonicToKeyPair(listOf("test"))

        assertEquals(32, result.publicKey.size)
        assertEquals(64, result.secretKey.size)
        assertEquals(0.toByte(), result.publicKey[0])
        assertEquals(62.toByte(), result.publicKey[31]) // 31 * 2
    }

    @Test
    fun mnemonicToKeyPair_throwsIfPublicKeyMissing() {
        runBlocking {
            givenBridgeReturns(
                JSONObject().apply {
                    put("secretKey", JSONArray().apply { repeat(64) { put(it) } })
                    // No publicKey!
                },
            )

            assertThrows(WalletKitBridgeException::class.java) {
                runBlocking { cryptoOperations.mnemonicToKeyPair(listOf("test")) }
            }
        }
    }

    @Test
    fun mnemonicToKeyPair_throwsIfSecretKeyMissing() {
        runBlocking {
            givenBridgeReturns(
                JSONObject().apply {
                    put("publicKey", JSONArray().apply { repeat(32) { put(it) } })
                    // No secretKey!
                },
            )

            assertThrows(WalletKitBridgeException::class.java) {
                runBlocking { cryptoOperations.mnemonicToKeyPair(listOf("test")) }
            }
        }
    }

    // --- sign tests ---

    @Test
    fun sign_parsesHexSignature() = runBlocking {
        // Signature as hex string
        givenBridgeReturns(
            jsonOf(
                "signature" to "abcd1234ef567890",
            ),
        )

        val result = cryptoOperations.sign(
            data = byteArrayOf(1, 2, 3),
            secretKey = ByteArray(64) { it.toByte() },
        )

        // Hex "abcd1234ef567890" = 8 bytes
        assertEquals(8, result.size)
        assertEquals(0xab.toByte(), result[0])
        assertEquals(0xcd.toByte(), result[1])
    }

    @Test
    fun sign_handlesHexWithPrefix() = runBlocking {
        givenBridgeReturns(
            jsonOf(
                "signature" to "0xdeadbeef",
            ),
        )

        val result = cryptoOperations.sign(
            data = byteArrayOf(1),
            secretKey = ByteArray(64),
        )

        // Should handle 0x prefix - "0xdeadbeef" parsed as hex
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun sign_throwsIfSignatureMissing() {
        runBlocking {
            // When bridge returns result with "signature" key present but null value
            // The takeIf { it != "null" } check should catch this
            givenBridgeReturns(jsonOf("signature" to JSONObject.NULL))

            assertThrows(WalletKitBridgeException::class.java) {
                runBlocking {
                    cryptoOperations.sign(
                        data = byteArrayOf(1),
                        secretKey = ByteArray(64),
                    )
                }
            }
        }
    }

    @Test
    fun sign_throwsIfSignatureEmpty() {
        runBlocking {
            // Empty string signature should throw
            givenBridgeReturns(jsonOf("signature" to ""))

            assertThrows(WalletKitBridgeException::class.java) {
                runBlocking {
                    cryptoOperations.sign(
                        data = byteArrayOf(1),
                        secretKey = ByteArray(64),
                    )
                }
            }
        }
    }
}
