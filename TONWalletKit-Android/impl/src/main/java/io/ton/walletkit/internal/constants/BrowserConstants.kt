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
 * Constants for the internal browser and TonConnect bridge.
 */
internal object BrowserConstants {

    // JavaScript Interface
    const val JS_INTERFACE_NAME = "AndroidTonConnect"

    // Message Types
    const val MESSAGE_TYPE_BRIDGE_REQUEST = "TONCONNECT_BRIDGE_REQUEST"
    const val MESSAGE_TYPE_BRIDGE_RESPONSE = "TONCONNECT_BRIDGE_RESPONSE"
    const val MESSAGE_TYPE_BRIDGE_EVENT = "TONCONNECT_BRIDGE_EVENT"

    // JSON Keys
    const val KEY_TYPE = "type"
    const val KEY_FRAME_ID = "frameId"
    const val KEY_MESSAGE_ID = "messageId"
    const val KEY_METHOD = "method"
    const val KEY_EVENT = "event"
    const val KEY_SUCCESS = "success"
    const val KEY_PAYLOAD = "payload"
    const val KEY_REQUEST = "request"

    // Default Values
    const val DEFAULT_FRAME_ID = "main"
    const val DEFAULT_METHOD = "unknown"
    const val EVENT_CONNECT = "connect"

    // Asset Paths
    const val INJECT_SCRIPT_PATH = "walletkit/inject.mjs"
}
