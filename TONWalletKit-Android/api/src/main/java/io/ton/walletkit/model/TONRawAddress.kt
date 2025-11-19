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

import io.ton.walletkit.WalletKitUtils
import kotlinx.serialization.Serializable
import org.ton.block.AddrStd

/**
 * Represents a TON blockchain address in raw format.
 *
 * This is a lightweight wrapper around ton-kotlin's [AddrStd] for raw address representation.
 * Raw address format consists of workchain and hash separated by colon.
 * Example: "0:83dfd552e63729b472fcbcc8c45ebcc6691702558b68ec7527e1ba403a0f31a8"
 *
 * @property workchain The workchain ID (usually 0 for basechain, -1 for masterchain)
 * @property hash The 32-byte address hash
 */
@Serializable
data class TONRawAddress(
    val workchain: Byte,
    val hash: ByteArray,
) {
    init {
        require(hash.size == 32) { "Hash must be exactly 32 bytes" }
    }

    /**
     * Raw address string representation: "workchain:hash"
     */
    val string: String
        get() = "${workchain.toInt()}:${WalletKitUtils.byteArrayToHexNoPrefix(hash)}"

    /**
     * Creates a TONRawAddress from a string representation.
     *
     * @param string Raw address string in format "workchain:hash"
     * @throws IllegalArgumentException if the format is invalid
     */
    constructor(string: String) : this(
        workchain = AddrStd.parseRaw(string).workchainId.toByte(),
        hash = AddrStd.parseRaw(string).address.toByteArray(),
    )

    /**
     * Converts this raw address to a user-friendly address.
     *
     * @param isBounceable Whether the address should be bounceable (default: true)
     * @param isTestnetOnly Whether the address is for testnet only (default: false)
     * @return User-friendly TON address
     */
    fun toUserFriendly(
        isBounceable: Boolean = true,
        isTestnetOnly: Boolean = false,
    ): TONUserFriendlyAddress {
        return TONUserFriendlyAddress(
            rawAddress = this,
            isBounceable = isBounceable,
            isTestnetOnly = isTestnetOnly,
        )
    }

    /**
     * Converts to ton-kotlin's AddrStd.
     */
    private fun toAddrStd(): AddrStd = AddrStd(
        workchainId = workchain.toInt(),
        address = hash,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TONRawAddress) return false
        if (workchain != other.workchain) return false
        if (!hash.contentEquals(other.hash)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = workchain.toInt()
        result = 31 * result + hash.contentHashCode()
        return result
    }

    override fun toString(): String = string

    companion object {
        /**
         * Parses a raw address string.
         *
         * @param string Raw address in "workchain:hash" format
         * @return Parsed TONRawAddress
         * @throws IllegalArgumentException if the format is invalid
         */
        fun parse(string: String): TONRawAddress = TONRawAddress(string)
    }
}
