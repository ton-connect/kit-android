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
package io.ton.walletkit.bridge

import io.ton.walletkit.model.Transaction
import io.ton.walletkit.model.TransactionType
import io.ton.walletkit.model.WalletAccount
import io.ton.walletkit.model.WalletState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for wallet data models.
 */
class ModelTest {

    @Test
    fun `WalletAccount has required fields`() {
        val wallet = WalletAccount(
            address = "EQDexample...",
            publicKey = "pubkey",
            name = "My Wallet",
            version = "v5r1",
            network = "mainnet",
            index = 0,
        )

        assertEquals("EQDexample...", wallet.address)
        assertEquals("v5r1", wallet.version)
    }

    @Test
    fun `WalletState contains balance and transactions`() {
        val tx = Transaction(
            hash = "hash123",
            timestamp = 1000L,
            amount = "100",
            type = TransactionType.INCOMING,
        )

        val state = WalletState(
            balance = "5000000000",
            transactions = listOf(tx),
        )

        assertEquals("5000000000", state.balance)
        assertEquals(1, state.transactions.size)
    }

    @Test
    fun `Transaction supports different types`() {
        val incoming = Transaction(
            hash = "1",
            timestamp = 100L,
            amount = "50",
            type = TransactionType.INCOMING,
        )

        val outgoing = Transaction(
            hash = "2",
            timestamp = 200L,
            amount = "30",
            type = TransactionType.OUTGOING,
        )

        assertEquals(TransactionType.INCOMING, incoming.type)
        assertEquals(TransactionType.OUTGOING, outgoing.type)
    }

    @Test
    fun `Transaction optional fields can be null`() {
        val tx = Transaction(
            hash = "hash",
            timestamp = 1000L,
            amount = "100",
        )

        assertNotNull(tx.hash)
        assertNull(tx.fee)
        assertNull(tx.comment)
    }
}
