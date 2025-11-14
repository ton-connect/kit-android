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

import io.ton.walletkit.model.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelTest {

    @Test
    fun testWalletAccountHasRequiredFields() {
        val wallet = WalletAccount(
            address = "EQDexample",
            publicKey = "pubkey",
            name = "My Wallet",
            version = "v5r1",
            network = "mainnet",
            index = 0,
        )
        assertEquals("EQDexample", wallet.address)
        assertEquals("v5r1", wallet.version)
    }

    @Test
    fun testTransactionHasRequiredFields() {
        val tx = Transaction(
            hash = "hash123",
            timestamp = 1000L,
            amount = "100000000",
            type = TransactionType.INCOMING,
        )
        assertEquals("hash123", tx.hash)
        assertNull(tx.fee)
    }

    @Test
    fun testTONNetworkValues() {
        assertEquals("-239", TONNetwork.MAINNET.value)
        assertEquals("-3", TONNetwork.TESTNET.value)
    }
}
