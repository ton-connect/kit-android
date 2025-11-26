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
package io.ton.walletkit.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TONRawAddressTest {

    @Test
    fun `parse basechain raw address`() {
        val rawString = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        val address = TONRawAddress(rawString)

        assertEquals(0.toByte(), address.workchain)
        assertEquals(32, address.hash.size)
        assertEquals(rawString, address.string)
    }

    @Test
    fun `parse masterchain raw address`() {
        val rawString = "-1:3333333333333333333333333333333333333333333333333333333333333333"
        val address = TONRawAddress(rawString)

        assertEquals((-1).toByte(), address.workchain)
        assertEquals(32, address.hash.size)
        assertEquals(rawString, address.string)
    }

    @Test
    fun `create from workchain and hash`() {
        val workchain: Byte = 0
        val hash = ByteArray(32) { it.toByte() }
        val address = TONRawAddress(workchain, hash)

        assertEquals(workchain, address.workchain)
        assertArrayEquals(hash, address.hash)
    }

    @Test
    fun `string representation is correct format`() {
        val workchain: Byte = 0
        val hash = "83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val address = TONRawAddress(workchain, hash)
        val expected = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"

        assertEquals(expected, address.string)
        assertEquals(expected, address.toString())
    }

    @Test
    fun `masterchain address has correct workchain`() {
        val workchain: Byte = -1
        val hash = ByteArray(32) { 0x33 }
        val address = TONRawAddress(workchain, hash)

        assertEquals(workchain, address.workchain)
        assertTrue(address.string.startsWith("-1:"))
    }

    @Test
    fun `parse and toString are reversible`() {
        val original = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        val address = TONRawAddress(original)
        val reconstructed = address.string

        assertEquals(original, reconstructed)
    }

    @Test
    fun `equals compares workchain and hash`() {
        val hash = ByteArray(32) { it.toByte() }
        val addr1 = TONRawAddress(0, hash)
        val addr2 = TONRawAddress(0, hash.copyOf())

        assertEquals(addr1, addr2)
    }

    @Test
    fun `different workchains are not equal`() {
        val hash = ByteArray(32) { it.toByte() }
        val addr1 = TONRawAddress(0, hash)
        val addr2 = TONRawAddress(-1, hash)

        assertNotEquals(addr1, addr2)
    }

    @Test
    fun `different hashes are not equal`() {
        val hash1 = ByteArray(32) { 0x11 }
        val hash2 = ByteArray(32) { 0x22 }
        val addr1 = TONRawAddress(0, hash1)
        val addr2 = TONRawAddress(0, hash2)

        assertNotEquals(addr1, addr2)
    }

    @Test
    fun `hashCode is consistent`() {
        val hash = ByteArray(32) { it.toByte() }
        val addr1 = TONRawAddress(0, hash)
        val addr2 = TONRawAddress(0, hash.copyOf())

        assertEquals(addr1.hashCode(), addr2.hashCode())
    }

    @Test
    fun `toUserFriendly creates bounceable by default`() {
        val rawString = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        val raw = TONRawAddress(rawString)
        val friendly = raw.toUserFriendly()

        assertEquals(0, friendly.workchain)
        assertArrayEquals(raw.hash, friendly.hash)
    }

    @Test
    fun `toUserFriendly with non-bounceable flag`() {
        val rawString = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        val raw = TONRawAddress(rawString)
        val bounceable = raw.toUserFriendly(isBounceable = true)
        val nonBounceable = raw.toUserFriendly(isBounceable = false)

        // Both should point to same raw address
        assertEquals(raw.workchain.toInt(), bounceable.workchain)
        assertEquals(raw.workchain.toInt(), nonBounceable.workchain)

        // But have different string representations
        assertNotEquals(bounceable.value, nonBounceable.value)
    }

    @Test
    fun `toUserFriendly with testnet flag`() {
        val rawString = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        val raw = TONRawAddress(rawString)
        val mainnet = raw.toUserFriendly(isTestnetOnly = false)
        val testnet = raw.toUserFriendly(isTestnetOnly = true)

        // Both should point to same raw address
        assertEquals(raw.workchain.toInt(), mainnet.workchain)
        assertEquals(raw.workchain.toInt(), testnet.workchain)

        // But have different string representations
        assertNotEquals(mainnet.value, testnet.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hash must be 32 bytes`() {
        val invalidHash = ByteArray(31)
        TONRawAddress(0, invalidHash)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse invalid format throws exception`() {
        TONRawAddress("invalid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse invalid hash length throws exception`() {
        TONRawAddress("0:1234") // Too short
    }

    @Test
    fun `companion parse method works`() {
        val rawString = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
        val address = TONRawAddress.parse(rawString)

        assertEquals(0.toByte(), address.workchain)
        assertEquals(rawString, address.string)
    }

    @Test
    fun `real world basechain address`() {
        val rawString = "0:0ab558f4db84fd31f61a273535c670c091ffc619b1cdbbe5769a0bf28d3b8fea"
        val address = TONRawAddress(rawString)

        assertEquals(0.toByte(), address.workchain)
        assertEquals(32, address.hash.size)
        assertEquals(rawString, address.string)
    }

    @Test
    fun `real world masterchain address`() {
        val rawString = "-1:dd24c4a1f2b88f8b7053513b5cc6c5a31bc44b2a72dcb4d8c0338af0f0d37ec5"
        val address = TONRawAddress(rawString)

        assertEquals((-1).toByte(), address.workchain)
        assertEquals(32, address.hash.size)
        assertEquals(rawString, address.string)
    }
}
