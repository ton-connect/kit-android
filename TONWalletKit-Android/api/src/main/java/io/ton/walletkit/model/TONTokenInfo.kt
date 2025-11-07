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
import kotlinx.serialization.json.JsonObject

/**
 * Token metadata information.
 *
 * @property description Token or NFT description
 * @property image Image URL
 * @property name Token or NFT name
 * @property nftIndex NFT index in collection (for NFTs)
 * @property symbol Token symbol
 * @property type Token type
 * @property valid Whether the metadata is valid
 * @property extra Extra metadata fields (e.g., _image_medium, _image_big, _image_small, uri, etc.)
 */
@Serializable
data class TONTokenInfo(
    val description: String? = null,
    val image: String? = null,
    val name: String? = null,
    val nftIndex: String? = null,
    val symbol: String? = null,
    val type: String? = null,
    val valid: Boolean? = null,
    val extra: JsonObject? = null,
)
