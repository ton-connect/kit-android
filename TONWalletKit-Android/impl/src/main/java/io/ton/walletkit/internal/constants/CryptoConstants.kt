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
package io.ton.walletkit.internal.constants

/**
 * Constants for cryptographic operations and Android KeyStore.
 * Defines algorithm specifications, key sizes, and keystore parameters.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object CryptoConstants {
    // Keystore and Cipher
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val DEFAULT_KEYSTORE_ALIAS = "walletkit_master_key"
    const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    // Key Sizes
    const val AES_KEY_SIZE = 256
    const val GCM_IV_SIZE = 12 // 96 bits recommended for GCM

    // Log Tags
    const val TAG_CRYPTO_MANAGER = "CryptoManager"
}
