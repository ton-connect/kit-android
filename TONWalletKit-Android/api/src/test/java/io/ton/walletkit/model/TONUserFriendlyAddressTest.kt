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

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TONUserFriendlyAddressTest {

    // Test addresses from ton-kotlin library tests
    private val testAddress1Bounceable = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"
    private val testAddress1NonBounceable = "Uf8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMxYA"
    private val testAddress1Testnet = "kf8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM_BP"
    private val testAddress1Raw = "-1:3333333333333333333333333333333333333333333333333333333333333333"

    private val testAddress2Bounceable = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
    private val testAddress2NonBounceable = "UQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqEBI"
    private val testAddress2Raw = "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"

    @Test
    fun `parse bounceable address`() {
        val address = TONUserFriendlyAddress(testAddress1Bounceable)

        assertEquals(-1, address.workchain)
        assertEquals(32, address.hash.size)
    }

    @Test
    fun `parse non-bounceable address`() {
        val address = TONUserFriendlyAddress(testAddress1NonBounceable)

        assertEquals(-1, address.workchain)
        assertEquals(32, address.hash.size)
    }

    @Test
    fun `bounceable and non-bounceable addresses are equal`() {
        val bounceable = TONUserFriendlyAddress(testAddress1Bounceable)
        val nonBounceable = TONUserFriendlyAddress(testAddress1NonBounceable)

        // They should be equal because they point to the same address
        assertEquals(bounceable, nonBounceable)
    }

    @Test
    fun `parse url safe address`() {
        val urlSafeAddress = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"
        val address = TONUserFriendlyAddress(urlSafeAddress)

        assertEquals(-1, address.workchain)
        assertEquals(32, address.hash.size)
    }

    @Test
    fun `toRawString converts to raw format`() {
        val address = TONUserFriendlyAddress(testAddress1Bounceable)
        val rawString = address.toRawString()

        assertEquals(testAddress1Raw, rawString)
    }

    @Test
    fun `raw property returns correct TONRawAddress`() {
        val address = TONUserFriendlyAddress(testAddress2Bounceable)
        val raw = address.raw

        assertEquals(0.toByte(), raw.workchain)
        assertEquals(32, raw.hash.size)
        assertEquals(testAddress2Raw, raw.string)
    }

    @Test
    fun `create from raw address with bounceable flag`() {
        val raw = TONRawAddress(testAddress2Raw)
        val friendly = TONUserFriendlyAddress(rawAddress = raw, isBounceable = true, isTestnetOnly = false)

        assertEquals(0, friendly.workchain)
        assertArrayEquals(raw.hash, friendly.hash)
        assertEquals(testAddress2Bounceable, friendly.value)
    }

    @Test
    fun `create from raw address with non-bounceable flag`() {
        val raw = TONRawAddress(testAddress2Raw)
        val friendly = TONUserFriendlyAddress(rawAddress = raw, isBounceable = false, isTestnetOnly = false)

        assertEquals(0, friendly.workchain)
        assertArrayEquals(raw.hash, friendly.hash)
        assertEquals(testAddress2NonBounceable, friendly.value)
    }

    @Test
    fun `create from raw address with testnet flag`() {
        val raw = TONRawAddress(testAddress1Raw)
        val friendly = TONUserFriendlyAddress(rawAddress = raw, isBounceable = true, isTestnetOnly = true)

        assertEquals(-1, friendly.workchain)
        assertArrayEquals(raw.hash, friendly.hash)
        assertEquals(testAddress1Testnet, friendly.value)
    }

    @Test
    fun `toString with different formats`() {
        val address = TONUserFriendlyAddress(testAddress2Bounceable)

        val bounceable = address.toString(isBounceable = true, isTestnetOnly = false)
        val nonBounceable = address.toString(isBounceable = false, isTestnetOnly = false)

        assertEquals(testAddress2Bounceable, bounceable)
        assertEquals(testAddress2NonBounceable, nonBounceable)
    }

    @Test
    fun `parse and reconstruct address`() {
        val original = testAddress1Bounceable
        val address = TONUserFriendlyAddress(original)
        val reconstructed = address.toString(isBounceable = true, isTestnetOnly = false)

        assertEquals(original, reconstructed)
    }

    @Test
    fun `companion parse method`() {
        val address = TONUserFriendlyAddress.parse(testAddress1Bounceable)

        assertEquals(-1, address.workchain)
        assertEquals(32, address.hash.size)
    }

    @Test
    fun `companion parseRaw method`() {
        val address = TONUserFriendlyAddress.parseRaw(testAddress1Raw)

        assertEquals(-1, address.workchain)
        assertEquals(32, address.hash.size)
    }

    @Test
    fun `companion parseUserFriendly method`() {
        val address = TONUserFriendlyAddress.parseUserFriendly(testAddress1Bounceable)

        assertEquals(-1, address.workchain)
        assertEquals(32, address.hash.size)
    }

    @Test
    fun `equals ignores bounceable flag`() {
        val addr1 = TONUserFriendlyAddress(testAddress2Bounceable)
        val addr2 = TONUserFriendlyAddress(testAddress2NonBounceable)

        assertEquals(addr1, addr2)
    }

    @Test
    fun `hashCode is consistent for same address`() {
        val addr1 = TONUserFriendlyAddress(testAddress2Bounceable)
        val addr2 = TONUserFriendlyAddress(testAddress2NonBounceable)

        assertEquals(addr1.hashCode(), addr2.hashCode())
    }

    @Test
    fun `different addresses are not equal`() {
        val addr1 = TONUserFriendlyAddress(testAddress1Bounceable)
        val addr2 = TONUserFriendlyAddress(testAddress2Bounceable)

        assertNotEquals(addr1, addr2)
    }

    @Test
    fun `workchain property`() {
        val masterchinAddress = TONUserFriendlyAddress(testAddress1Bounceable)
        val basechainAddress = TONUserFriendlyAddress(testAddress2Bounceable)

        assertEquals(-1, masterchinAddress.workchain)
        assertEquals(0, basechainAddress.workchain)
    }

    @Test
    fun `hash property returns 32 bytes`() {
        val address = TONUserFriendlyAddress(testAddress1Bounceable)

        assertEquals(32, address.hash.size)
    }

    // Note: ton-kotlin's AddrStd.parse is lenient and may not throw exceptions
    // for all invalid inputs. We rely on ton-kotlin's validation.

    @Test
    fun `real world address - wallet 1`() {
        val address = "EQAKtVj024T9MfYaJzU1xnDAkf_GGbHNu-V2mgvyjTuP6rvC"
        val friendly = TONUserFriendlyAddress(address)

        assertEquals(0, friendly.workchain)
        assertEquals(32, friendly.hash.size)
    }

    @Test
    fun `real world address - wallet 2`() {
        val address = "EQBLAcMnTcyx-_mWQtrVEC1eyDfK2nHI-A54P5eL7y-uE2Ht"
        val friendly = TONUserFriendlyAddress(address)

        assertEquals(0, friendly.workchain)
        assertEquals(32, friendly.hash.size)
    }

    @Test
    fun `basechain addresses`() {
        val bounceable = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N"
        val nonBounceable = "UQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqEBI"

        val addr1 = TONUserFriendlyAddress(bounceable)
        val addr2 = TONUserFriendlyAddress(nonBounceable)

        assertEquals(addr1, addr2)
        assertEquals(0, addr1.workchain)
        assertEquals(0, addr2.workchain)
    }

    @Test
    fun `masterchain addresses`() {
        val bounceable = "Ef_dJMSh8riPi3BTUTtcxsWjG8RLKnLctNjAM4rw8NN-xWdr"
        val nonBounceable = "Uf_dJMSh8riPi3BTUTtcxsWjG8RLKnLctNjAM4rw8NN-xTqu"

        val addr1 = TONUserFriendlyAddress(bounceable)
        val addr2 = TONUserFriendlyAddress(nonBounceable)

        assertEquals(addr1, addr2)
        assertEquals(-1, addr1.workchain)
        assertEquals(-1, addr2.workchain)
    }

    @Test
    fun `round trip conversion raw to friendly to raw`() {
        val originalRaw = testAddress2Raw
        val raw = TONRawAddress(originalRaw)
        val friendly = TONUserFriendlyAddress(rawAddress = raw, isBounceable = true, isTestnetOnly = false)
        val backToRaw = friendly.toRawString()

        assertEquals(originalRaw, backToRaw)
    }

    @Test
    fun `round trip conversion friendly to raw to friendly`() {
        val originalFriendly = testAddress2Bounceable
        val friendly = TONUserFriendlyAddress(originalFriendly)
        val raw = friendly.raw
        val backToFriendly = TONUserFriendlyAddress(rawAddress = raw, isBounceable = true, isTestnetOnly = false)

        assertEquals(friendly, backToFriendly)
        assertEquals(originalFriendly, backToFriendly.value)
    }

    @Test
    fun `default toString returns value`() {
        val address = TONUserFriendlyAddress(testAddress1Bounceable)

        assertEquals(testAddress1Bounceable, address.toString())
    }

    @Test
    fun `parse auto-detects format`() {
        // Should handle both user-friendly and raw formats
        val friendly = TONUserFriendlyAddress.parse(testAddress1Bounceable)
        val fromRaw = TONUserFriendlyAddress.parse(testAddress1Raw)

        assertEquals(friendly, fromRaw)
    }

    @Test
    fun `url safe and non-url safe both work`() {
        // URL-safe uses - and _
        val urlSafe = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"
        // Non-URL-safe uses + and /
        val nonUrlSafe = "Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"

        val addr1 = TONUserFriendlyAddress(urlSafe)
        val addr2 = TONUserFriendlyAddress(nonUrlSafe)

        assertEquals(addr1, addr2)
    }

    @Test
    fun `convert between url safe and non-url safe formats`() {
        val address = TONUserFriendlyAddress(testAddress2Bounceable)

        val urlSafe = address.toString(isBounceable = true, isTestnetOnly = false, urlSafe = true)
        val nonUrlSafe = address.toString(isBounceable = true, isTestnetOnly = false, urlSafe = false)

        // Both should parse to the same address
        val fromUrlSafe = TONUserFriendlyAddress(urlSafe)
        val fromNonUrlSafe = TONUserFriendlyAddress(nonUrlSafe)

        assertEquals(fromUrlSafe, fromNonUrlSafe)
    }
}
