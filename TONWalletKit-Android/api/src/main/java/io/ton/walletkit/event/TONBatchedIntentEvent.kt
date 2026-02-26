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
package io.ton.walletkit.event

import org.json.JSONObject

/**
 * A batched intent event containing multiple intent items that should be
 * processed as a group.
 *
 * The SDK splits multi-item transaction intents into per-item
 * [TONIntentEvent]s so the wallet can display each action separately while
 * approving or rejecting the whole batch atomically.
 *
 * Use cases:
 * - send TON + connect (intent with connect request)
 * - action intent that resolves to multiple steps
 */
data class TONBatchedIntentEvent(
    /** Unique batch identifier (matches the original txIntent id). */
    val id: String,
    /** How the batch reached the wallet (deepLink, objectStorage, etc.). */
    val origin: String,
    /** Client public key for response routing (optional for fire-and-forget). */
    val clientId: String?,
    /** Whether a connect flow should follow after all intents are approved. */
    val hasConnectRequest: Boolean,
    /** The individual intent events in this batch. */
    val intents: List<TONIntentEvent>,
    /** Raw JSON for bridge passthrough. */
    val rawJson: JSONObject,
)
