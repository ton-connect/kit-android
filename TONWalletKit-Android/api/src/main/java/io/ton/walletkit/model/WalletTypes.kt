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

/**
 * Custom wallet signer interface for hardware wallet integration.
 *
 * Implement this interface to provide custom signing logic for hardware wallets
 * or other external signers.
 *
 * Mirrors iOS TONWalletSignerProtocol for cross-platform consistency.
 */
interface WalletSigner {
    /**
     * Sign data bytes.
     *
     * @param data Data to sign
     * @return Signature as hex string
     */
    suspend fun sign(data: ByteArray): TONHex

    /**
     * Get public key.
     *
     * @return Public key as hex string
     */
    fun publicKey(): TONHex
}

/**
 * Result of creating a signer.
 *
 * @property signerId Internal signer identifier
 * @property publicKey Public key as hex string
 */
data class WalletSignerInfo(
    val signerId: String,
    val publicKey: TONHex,
)

/**
 * Result of creating a wallet adapter.
 *
 * @property adapterId Internal adapter identifier
 * @property address Wallet address
 */
data class WalletAdapterInfo(
    val adapterId: String,
    val address: TONUserFriendlyAddress,
)

/**
 * Ed25519 key pair.
 *
 * @property publicKey Public key (32 bytes)
 * @property secretKey Secret key (64 bytes, includes public key)
 */
data class KeyPair(
    val publicKey: ByteArray,
    val secretKey: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyPair
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!secretKey.contentEquals(other.secretKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + secretKey.contentHashCode()
        return result
    }
}
