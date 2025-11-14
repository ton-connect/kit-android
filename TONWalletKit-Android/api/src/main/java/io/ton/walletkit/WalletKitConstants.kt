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

/**
 * Constants used throughout the WalletKit SDK.
 *
 * Matches the constants from the JavaScript @ton/walletkit library.
 */
object WalletKitConstants {
    /**
     * Default wallet ID for V5R1 wallets.
     *
     * This value is used to make wallet addresses unique when creating multiple
     * wallets from the same mnemonic or private key. The wallet ID is part of
     * the smart contract initialization data.
     *
     * Corresponds to `defaultWalletIdV5R1` in @ton/walletkit.
     */
    const val DEFAULT_WALLET_ID_V5R1: Long = 2147483409L

    /**
     * Default wallet ID for V4R2 wallets.
     *
     * This value is used to make wallet addresses unique when creating multiple
     * wallets from the same mnemonic or private key. The wallet ID is part of
     * the smart contract initialization data.
     *
     * Corresponds to `defaultWalletIdV4R2` in @ton/walletkit.
     */
    const val DEFAULT_WALLET_ID_V4R2: Long = 698983191L

    /**
     * Default workchain for wallet contracts.
     *
     * - 0: Basechain (default, recommended for most use cases)
     * - -1: Masterchain (requires more fees, used for validators)
     */
    const val DEFAULT_WORKCHAIN: Int = 0
}
