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
package io.ton.walletkit.demo.domain.model

/**
 * Wallet interface type determines how the wallet handles signing operations.
 */
enum class WalletInterfaceType(val value: String) {
    /**
     * Standard mnemonic-based wallet that automatically handles signing.
     */
    MNEMONIC("mnemonic"),

    /**
     * Secret key (private key) based wallet that automatically handles signing.
     */
    SECRET_KEY("secretKey"),

    /**
     * Custom signer wallet that requires user confirmation for each signing operation.
     */
    SIGNER("signer"),
    ;

    companion object {
        fun fromValue(value: String): WalletInterfaceType = entries.find { it.value == value } ?: MNEMONIC
    }
}
