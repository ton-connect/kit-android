package io.ton.walletkit

import io.ton.walletkit.model.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            index = 0
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
            type = TransactionType.INCOMING
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
