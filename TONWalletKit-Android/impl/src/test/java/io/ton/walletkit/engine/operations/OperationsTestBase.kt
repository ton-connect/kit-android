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
package io.ton.walletkit.engine.operations

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.ton.walletkit.bridge.BridgeCodec
import io.ton.walletkit.engine.infrastructure.BridgeRpcClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Before

abstract class OperationsTestBase {

    internal lateinit var rpcClient: BridgeRpcClient
    protected val json = Json { ignoreUnknownKeys = true }

    protected var capturedMethod: String? = null
    protected var capturedParams: Any? = null

    private var mockResponse: JsonObject = JsonObject(emptyMap())
    private var mockRawResponse: JsonElement = JsonObject(emptyMap())

    @Before
    open fun setup() {
        rpcClient = mockk(relaxed = true)
        every { rpcClient.json } returns json
        installMocks()
    }

    protected fun givenBridgeReturns(response: JsonObject) {
        mockResponse = response
        mockRawResponse = response
        installMocks()
    }

    protected fun givenBridgeReturns(jsonString: String) {
        givenBridgeReturns(json.parseToJsonElement(jsonString).let { it as JsonObject })
    }

    protected fun givenBridgeReturnsRaw(response: JsonElement) {
        mockRawResponse = response
        installMocks()
    }

    protected fun givenBridgeReturnsRaw(string: String) {
        givenBridgeReturnsRaw(JsonPrimitive(string))
    }

    protected fun givenBridgeReturnsRawNull() {
        givenBridgeReturnsRaw(JsonNull)
    }

    private fun installMocks() {
        coEvery { rpcClient.call(any(), any()) } coAnswers {
            capturedMethod = firstArg()
            capturedParams = secondArg()
            mockResponse
        }
        coEvery { rpcClient.call(any()) } coAnswers {
            capturedMethod = firstArg()
            capturedParams = null
            mockResponse
        }
        coEvery { rpcClient.send(any(), any()) } coAnswers {
            capturedMethod = firstArg()
            capturedParams = secondArg()
            mockRawResponse
        }
        coEvery { rpcClient.send(any()) } coAnswers {
            capturedMethod = firstArg()
            capturedParams = null
            mockRawResponse
        }
    }

    protected fun encodeCapturedParams(): JsonElement = BridgeCodec(json).encode(capturedParams)

    protected fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject =
        JsonObject(
            pairs.associate { (key, value) ->
                key to when (value) {
                    null -> JsonNull
                    is JsonElement -> value
                    is String -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    else -> JsonPrimitive(value.toString())
                }
            },
        )
}
