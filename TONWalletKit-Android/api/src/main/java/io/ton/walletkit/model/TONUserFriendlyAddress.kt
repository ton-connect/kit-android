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

import kotlinx.serialization.Serializable
import org.ton.block.AddrStd

/**
 * Represents a TON blockchain address in user-friendly base64 format.
 *
 * This is a lightweight wrapper around ton-kotlin's [AddrStd] which provides:
 * - Parsing from user-friendly Base64 strings with CRC16 validation
 * - Parsing from raw "workchain:hash" format
 * - Conversion to user-friendly format with bounceable/testnet flags
 * - URL-safe Base64 encoding
 *
 * Example: "EQCjk1hh952vWaE2-K7xfGRiCDOQnhFKT6yGLrNY6KzZP6O2"
 *
 * @property value The base64-encoded user-friendly address string
 */
@Serializable
data class TONUserFriendlyAddress(
    val value: String,
) {
    /**
     * The underlying ton-kotlin AddrStd instance.
     * Lazy-initialized from the value string.
     */
    private val addrStd: AddrStd by lazy {
        AddrStd.parse(value)
    }

    /**
     * The raw address representation.
     */
    val raw: TONRawAddress
        get() = TONRawAddress(
            workchain = addrStd.workchainId.toByte(),
            hash = addrStd.address.toByteArray(),
        )

    /**
     * The workchain ID (-1 for masterchain, 0 for basechain).
     */
    val workchain: Int
        get() = addrStd.workchainId

    /**
     * The 32-byte address hash.
     */
    val hash: ByteArray
        get() = addrStd.address.toByteArray()

    /**
     * Creates a TONUserFriendlyAddress from a raw address with flags.
     *
     * @param rawAddress The raw address
     * @param isBounceable Whether the address should be bounceable (default: true)
     * @param isTestnetOnly Whether the address is for testnet only (default: false)
     */
    constructor(
        rawAddress: TONRawAddress,
        isBounceable: Boolean = true,
        isTestnetOnly: Boolean = false,
    ) : this(
        value = AddrStd(
            workchainId = rawAddress.workchain.toInt(),
            address = rawAddress.hash,
        ).toString(
            userFriendly = true,
            urlSafe = true,
            testOnly = isTestnetOnly,
            bounceable = isBounceable,
        ),
    )

    /**
     * Converts this address to a different format.
     *
     * @param isBounceable Whether to use bounceable format
     * @param isTestnetOnly Whether to mark as testnet-only
     * @param urlSafe Whether to use URL-safe Base64 (default: true)
     * @return New address string in the requested format
     */
    fun toString(
        isBounceable: Boolean = true,
        isTestnetOnly: Boolean = false,
        urlSafe: Boolean = true,
    ): String = addrStd.toString(
        userFriendly = true,
        urlSafe = urlSafe,
        testOnly = isTestnetOnly,
        bounceable = isBounceable,
    )

    /**
     * Returns the raw address format: "workchain:hash"
     * Uses lowercase hex to match iOS implementation.
     */
    fun toRawString(): String = raw.string

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TONUserFriendlyAddress) return false
        // Compare the normalized addresses (ignore bounceable/testnet flags)
        return addrStd.workchainId == other.addrStd.workchainId &&
            addrStd.address == other.addrStd.address
    }

    override fun hashCode(): Int {
        var result = addrStd.workchainId
        result = 31 * result + addrStd.address.hashCode()
        return result
    }

    override fun toString(): String = value

    companion object {
        /**
         * Parses an address from either user-friendly Base64 or raw format.
         *
         * @param address Address string in user-friendly or raw format
         * @return Parsed TONUserFriendlyAddress
         * @throws IllegalArgumentException if the address is invalid
         */
        fun parse(address: String): TONUserFriendlyAddress =
            TONUserFriendlyAddress(
                AddrStd.parse(address).toString(
                    userFriendly = true,
                    urlSafe = true,
                    testOnly = false,
                    bounceable = true,
                ),
            )

        /**
         * Parses a raw address in "workchain:hash" format.
         *
         * @param rawAddress Raw address string
         * @return Parsed TONUserFriendlyAddress
         * @throws IllegalArgumentException if the address is invalid
         */
        fun parseRaw(rawAddress: String): TONUserFriendlyAddress =
            TONUserFriendlyAddress(
                AddrStd.parseRaw(rawAddress).toString(
                    userFriendly = true,
                    urlSafe = true,
                    testOnly = false,
                    bounceable = true,
                ),
            )

        /**
         * Parses a user-friendly Base64 address.
         *
         * @param userFriendlyAddress User-friendly address string
         * @return Parsed TONUserFriendlyAddress
         * @throws IllegalArgumentException if the address is invalid
         */
        fun parseUserFriendly(userFriendlyAddress: String): TONUserFriendlyAddress =
            TONUserFriendlyAddress(
                AddrStd.parseUserFriendly(userFriendlyAddress).toString(
                    userFriendly = true,
                    urlSafe = true,
                    testOnly = false,
                    bounceable = true,
                ),
            )
    }
}
