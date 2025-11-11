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
 * JSON constants used for @SerialName annotations in the API module.
 * These must match the values in impl module's JsonConstants.
 */
internal object JsonConstants {
    const val KEY_VALID_UNTIL = "valid_until"
    const val VALUE_SIGN_DATA_TEXT = "text"
    const val VALUE_SIGN_DATA_BINARY = "binary"
    const val VALUE_SIGN_DATA_CELL = "cell"
    const val VALUE_ASSET_TON = "ton"
    const val VALUE_ASSET_JETTON = "jetton"
    const val VALUE_PREVIEW_ERROR = "error"
    const val VALUE_PREVIEW_SUCCESS = "success"
}
