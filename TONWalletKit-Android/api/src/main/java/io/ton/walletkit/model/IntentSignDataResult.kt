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
 * Result of approving a sign data intent.
 *
 * Contains the signature and metadata about the signed data.
 *
 * @property signature Base64 encoded signature bytes
 * @property address Wallet address in raw format
 * @property timestamp Unix timestamp when data was signed
 * @property domain App domain name (plain string)
 * @property payload Original payload from the request
 */
data class IntentSignDataResult(
    val signature: String,
    val address: String,
    val timestamp: Long,
    val domain: String,
    val payload: SignDataIntentPayload,
)

/**
 * Payload from a sign data intent request.
 */
sealed interface SignDataIntentPayload {
    /**
     * Text payload.
     * @property text Plain text to sign
     */
    data class Text(val text: String) : SignDataIntentPayload

    /**
     * Binary payload.
     * @property bytes Base64 encoded binary data
     */
    data class Binary(val bytes: String) : SignDataIntentPayload

    /**
     * Cell payload.
     * @property schema Schema identifier
     * @property cell Base64 encoded cell
     */
    data class Cell(val schema: String, val cell: String) : SignDataIntentPayload
}
