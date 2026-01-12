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
package io.ton.walletkit.engine.state

import io.ton.walletkit.WalletKitUtils
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.WalletSigner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SignerManager - custom signer registration and lookup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SignerManagerTest {

    private lateinit var manager: SignerManager

    @Before
    fun setup() {
        manager = SignerManager()
    }

    // --- Register Signer Tests ---

    @Test
    fun registerSigner_returnsUniqueId() = runBlocking {
        val signer = createMockSigner()

        val signerId = manager.registerSigner(signer)

        assertTrue("Signer ID should start with 'signer_'", signerId.startsWith("signer_"))
        assertTrue("Signer ID should contain timestamp", signerId.contains("_"))
    }

    @Test
    fun registerSigner_multipleSigners_returnsDistinctIds() = runBlocking {
        val signer1 = createMockSigner()
        val signer2 = createMockSigner()
        val signer3 = createMockSigner()

        val id1 = manager.registerSigner(signer1)
        val id2 = manager.registerSigner(signer2)
        val id3 = manager.registerSigner(signer3)

        assertNotEquals("IDs should be unique", id1, id2)
        assertNotEquals("IDs should be unique", id2, id3)
        assertNotEquals("IDs should be unique", id1, id3)
    }

    // --- Has Custom Signer Tests ---

    @Test
    fun hasCustomSigner_registeredSigner_returnsTrue() = runBlocking {
        val signer = createMockSigner()
        val signerId = manager.registerSigner(signer)

        val hasSigner = manager.hasCustomSigner(signerId)

        assertTrue("Should have registered signer", hasSigner)
    }

    @Test
    fun hasCustomSigner_unknownId_returnsFalse() {
        val hasSigner = manager.hasCustomSigner("signer_unknown_123")

        assertFalse("Should not have unknown signer", hasSigner)
    }

    @Test
    fun hasCustomSigner_afterRemove_returnsFalse() = runBlocking {
        val signer = createMockSigner()
        val signerId = manager.registerSigner(signer)
        manager.removeSigner(signerId)

        val hasSigner = manager.hasCustomSigner(signerId)

        assertFalse("Should not have removed signer", hasSigner)
    }

    // --- Get Signer Tests ---

    @Test
    fun getSigner_registeredSigner_returnsCorrectInstance() = runBlocking {
        val signer = createMockSigner()
        val signerId = manager.registerSigner(signer)

        val retrieved = manager.getSigner(signerId)

        assertSame("Should return same signer instance", signer, retrieved)
    }

    @Test
    fun getSigner_unknownId_returnsNull() {
        val retrieved = manager.getSigner("signer_unknown_123")

        assertNull("Should return null for unknown ID", retrieved)
    }

    @Test
    fun getSigner_afterRemove_returnsNull() = runBlocking {
        val signer = createMockSigner()
        val signerId = manager.registerSigner(signer)
        manager.removeSigner(signerId)

        val retrieved = manager.getSigner(signerId)

        assertNull("Should return null after removal", retrieved)
    }

    // --- Remove Signer Tests ---

    @Test
    fun removeSigner_existingSigner_removes() = runBlocking {
        val signer = createMockSigner()
        val signerId = manager.registerSigner(signer)

        manager.removeSigner(signerId)

        assertFalse("Signer should be removed", manager.hasCustomSigner(signerId))
    }

    @Test
    fun removeSigner_unknownId_doesNotThrow() = runBlocking {
        // Should not throw
        manager.removeSigner("signer_unknown_123")
    }

    // --- Current Signer IDs Tests ---

    @Test
    fun currentSignerIds_returnsAllRegisteredIds() = runBlocking {
        val id1 = manager.registerSigner(createMockSigner())
        val id2 = manager.registerSigner(createMockSigner())
        val id3 = manager.registerSigner(createMockSigner())

        val ids = manager.currentSignerIds()

        assertEquals("Should have 3 signer IDs", 3, ids.size)
        assertTrue("Should contain id1", ids.contains(id1))
        assertTrue("Should contain id2", ids.contains(id2))
        assertTrue("Should contain id3", ids.contains(id3))
    }

    @Test
    fun currentSignerIds_afterRemove_excludesRemoved() = runBlocking {
        val id1 = manager.registerSigner(createMockSigner())
        val id2 = manager.registerSigner(createMockSigner())
        manager.removeSigner(id1)

        val ids = manager.currentSignerIds()

        assertEquals("Should have 1 signer ID", 1, ids.size)
        assertFalse("Should not contain removed id1", ids.contains(id1))
        assertTrue("Should contain id2", ids.contains(id2))
    }

    // --- Concurrent Operations Tests ---

    @Test
    fun concurrentRegister_allSucceed() = runBlocking {
        val signers = (1..20).map { createMockSigner() }

        val jobs = signers.map { signer ->
            async { manager.registerSigner(signer) }
        }

        val ids = jobs.awaitAll()

        assertEquals("All 20 signers should be registered", 20, ids.toSet().size)
        assertEquals("All IDs should be unique", 20, ids.distinct().size)
    }

    // --- Helper ---

    private fun createMockSigner(): WalletSigner = object : WalletSigner {
        override fun publicKey(): TONHex = TONHex("0x" + "00".repeat(32))
        override suspend fun sign(data: ByteArray): TONHex = TONHex(WalletKitUtils.byteArrayToHex(data))
    }
}
