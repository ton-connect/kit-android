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
 * Miscellaneous string constants used throughout the WalletKit SDK.
 * Contains various literals that don't fit into other constant categories.
 *
 * @suppress Internal implementation constants. Not part of public API.
 */
internal object MiscConstants {
    // String Delimiters and Separators
    const val SPACE_DELIMITER = " "
    const val EMPTY_STRING = ""

    // URL Schemes
    const val SCHEME_HTTPS = "https://"
    const val SCHEME_HTTP = "http://"

    // Numeric Constants
    const val BRIDGE_STORAGE_KEYS_COUNT_SUFFIX = " bridge storage keys"
}
