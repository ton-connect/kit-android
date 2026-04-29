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
import io.ton.walletkit.testfixtures.TestKeyPairArrays
import io.ton.walletkit.testfixtures.TestKeyPairIndexedObjects
import io.ton.walletkit.testfixtures.TestPublicKeyOnly
import io.ton.walletkit.testfixtures.TestSecretKeyOnly
import io.ton.walletkit.testfixtures.TestSignatureBody
import io.ton.walletkit.testfixtures.TestStringItemsBody
import io.ton.walletkit.testfixtures.jsonObjectOf
import io.ton.walletkit.testfixtures.jsonObjectOfExplicitNulls
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
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
            jsonObjectOf(TestStringItemsBody(items = listOf("word1", "word2", "word3"))),
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
        givenBridgeReturns(jsonObjectOf(TestStringItemsBody(items = words)))

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
            jsonObjectOf(
                TestKeyPairArrays(
                    publicKey = (0..31).toList(),
                    secretKey = (0..63).map { it + 100 },
                ),
            ),
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
        val publicKeyMap = (0..31).associate { it.toString() to it * 2 }
        val secretKeyMap = (0..63).associate { it.toString() to it + 50 }
        givenBridgeReturns(
            jsonObjectOf(
                TestKeyPairIndexedObjects(
                    publicKey = publicKeyMap,
                    secretKey = secretKeyMap,
                ),
            ),
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
                jsonObjectOf(TestSecretKeyOnly(secretKey = (0..63).toList())),
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
                jsonObjectOf(TestPublicKeyOnly(publicKey = (0..31).toList())),
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
        givenBridgeReturns(jsonObjectOf(TestSignatureBody(signature = "abcd1234ef567890")))

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
    fun sign_parsesHexSignatureFromValueField() = runBlocking {
        givenBridgeReturns(jsonObjectOf(TestSignValueBody(value = "0xdeadbeef")))

        val result = cryptoOperations.sign(
            data = byteArrayOf(1, 2, 3),
            secretKey = ByteArray(64) { it.toByte() },
        )

        assertEquals(4, result.size)
        assertEquals(0xde.toByte(), result[0])
        assertEquals(0xad.toByte(), result[1])
        assertEquals(0xbe.toByte(), result[2])
        assertEquals(0xef.toByte(), result[3])
    }

    @Test
    fun sign_handlesHexWithPrefix() = runBlocking {
        givenBridgeReturns(jsonObjectOf(TestSignatureBody(signature = "0xdeadbeef")))

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
            givenBridgeReturns(jsonObjectOfExplicitNulls(TestNullableSignatureBody(signature = null)))

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
            givenBridgeReturns(jsonObjectOf(TestSignatureBody(signature = "")))

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

    /** `{ "value": "<hex>" }` — alternate signature wrapper (legacy `value` key). */
    @Serializable
    private data class TestSignValueBody(val value: String)

    /** `{ "signature": null }` — explicit JSON-null signature for the missing-value test. */
    @Serializable
    private data class TestNullableSignatureBody(val signature: String?)
}
