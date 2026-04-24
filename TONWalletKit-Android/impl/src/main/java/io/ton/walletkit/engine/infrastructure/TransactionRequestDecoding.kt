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
package io.ton.walletkit.engine.infrastructure

import io.ton.walletkit.api.generated.TONTransactionRequest
import io.ton.walletkit.exceptions.JSValueConversionException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Shared decoder for the JS bridge's [TONTransactionRequest] JSON envelope, used by both
 * [io.ton.walletkit.swap.BuiltInSwapProvider] and [io.ton.walletkit.staking.BuiltInStakingProvider].
 * A bridge method returning a transaction (swap / stake / unstake) hands back a JSON string that
 * callers then resubmit through the normal wallet transaction flow, so the failure mode is the
 * same for every caller: wrap [SerializationException] in [JSValueConversionException.DecodingError].
 */
internal fun decodeTransactionRequest(json: String): TONTransactionRequest = try {
    Json.decodeFromString(TONTransactionRequest.serializer(), json)
} catch (e: SerializationException) {
    throw JSValueConversionException.DecodingError(
        message = "Failed to decode TONTransactionRequest: ${e.message}",
        cause = e,
    )
}
