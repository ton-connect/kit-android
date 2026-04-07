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
package io.ton.walletkit.engine.operations.requests

import kotlinx.serialization.Serializable

/**
 * Internal bridge request models for swap operations.
 *
 * @suppress Internal bridge communication only.
 */

@Serializable
internal data class CreateOmnistonSwapProviderRequest(
    val config: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
internal data class CreateDeDustSwapProviderRequest(
    val config: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
internal data class RegisterSwapProviderRequest(
    val providerId: String,
)

@Serializable
internal data class GetSwapQuoteRequest(
    val params: kotlinx.serialization.json.JsonElement,
    val providerId: String? = null,
)

@Serializable
internal data class BuildSwapTransactionRequest(
    val params: kotlinx.serialization.json.JsonElement,
)

@Serializable
internal data class SetDefaultSwapProviderRequest(
    val providerId: String,
)

@Serializable
internal data class HasSwapProviderRequest(
    val providerId: String,
)
