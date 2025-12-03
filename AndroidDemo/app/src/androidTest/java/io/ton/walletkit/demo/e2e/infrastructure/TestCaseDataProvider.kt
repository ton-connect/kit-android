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
package io.ton.walletkit.demo.e2e.infrastructure

/**
 * Provides test case data for E2E tests.
 *
 * This class provides a fallback mechanism for when the Allure API is not available.
 * It contains hardcoded test data that mirrors what's in Allure TestOps.
 *
 * The data format follows the test runner's JSON format:
 * - Precondition and Expected Result are JSON wrapped in markdown code fences
 * - The e2e page parses these using evalFenceCondition()
 * - Predicates like isValidString, isValidRawAddressString are available
 *
 * @see https://allure-test-runner.vercel.app/e2e
 */
object TestCaseDataProvider {

    /**
     * Get test case data by Allure ID.
     * First tries the Allure API (if client provided), falls back to hardcoded data.
     */
    fun getTestCaseData(allureId: String, allureClient: AllureApiClient? = null): TestCaseData? {
        // Try Allure API first
        allureClient?.let { client ->
            try {
                return client.getTestCaseData(allureId)
            } catch (e: Exception) {
                android.util.Log.w("TestCaseDataProvider", "Failed to fetch from Allure API: ${e.message}")
            }
        }

        // Fall back to hardcoded data
        return getHardcodedTestCaseData(allureId)
    }

    /**
     * Get hardcoded test case data for known test IDs.
     * This mirrors the data structure in Allure TestOps.
     */
    private fun getHardcodedTestCaseData(allureId: String): TestCaseData? = when (allureId) {
        // Connect tests
        "2294" -> TestCaseData(
            precondition = CONNECT_SUCCESS_PRECONDITION,
            expectedResult = CONNECT_SUCCESS_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "2295" -> TestCaseData(
            precondition = CONNECT_REJECT_PRECONDITION,
            expectedResult = CONNECT_REJECT_EXPECTED_RESULT,
            isPositiveCase = false,
        )
        // Send Transaction tests
        "2297" -> TestCaseData(
            precondition = SEND_TX_PRECONDITION,
            expectedResult = SEND_TX_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "2298" -> TestCaseData(
            precondition = SEND_TX_PRECONDITION,
            expectedResult = SEND_TX_REJECT_EXPECTED_RESULT,
            isPositiveCase = false,
        )
        // Sign Data tests
        "2300" -> TestCaseData(
            precondition = SIGN_DATA_PRECONDITION,
            expectedResult = SIGN_DATA_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "2301" -> TestCaseData(
            precondition = SIGN_DATA_PRECONDITION,
            expectedResult = SIGN_DATA_REJECT_EXPECTED_RESULT,
            isPositiveCase = false,
        )
        else -> null
    }

    // ========================================
    // Connect Test Data (ID: 2294 - Success)
    // ========================================

    /**
     * Precondition for successful connect test.
     * Empty precondition - standard connect without special requirements.
     * The __meta field can optionally specify:
     * - manifestUrl: custom manifest URL
     * - excludeTonProof: whether to exclude ton_proof from request
     */
    private val CONNECT_SUCCESS_PRECONDITION = """
```json
{
  "__meta": {}
}
```
    """.trimIndent()

    /**
     * Expected result for successful connect test.
     * From Allure TestOps - [Mobile Chrome] Connect test case.
     *
     * Uses predicates from the test runner:
     * - isNonNegativeInt: validates positive integer (for id)
     * - isValidRawAddressString: validates TON address format
     * - isValidNetwork: validates network value (-239 mainnet, -3 testnet)
     * - isValidStateInitString: validates stateInit
     * - isValidPublicKey: validates public key format
     * - isValidCurrentTimestamp: validates timestamp is recent
     * - appHostLength(): returns domain length
     * - appHost(): returns domain value
     * - tonProofPayload(): returns the ton_proof payload
     * - isValidTonProofSignature: validates signature format
     * - isValidString: validates non-empty string
     * - isValidFeatureList: validates features array
     */
    private val CONNECT_SUCCESS_EXPECTED_RESULT = """
```json
{
    "event": "connect",
    "id": isNonNegativeInt,
    "payload": {
        "items": [
            {
                "name": "ton_addr",
                "address": isValidRawAddressString,
                "network": isValidNetwork,
                "walletStateInit": isValidStateInitString,
                "publicKey": isValidPublicKey
            },
            {
                "name": "ton_proof",
                "proof": {
                    "timestamp": isValidCurrentTimestamp,
                    "domain": {
                        "lengthBytes": appHostLength(),
                        "value": appHost()
                    },
                    "payload": tonProofPayload(),
                    "signature": isValidTonProofSignature
                }
            }
        ],
        "device": {
            "platform": "android",
            "appName": isValidString,
            "appVersion": isValidString,
            "maxProtocolVersion": 2,
            "features": isValidFeatureList
        }
    }
}
```
    """.trimIndent()

    // ========================================
    // Connect Test Data (ID: 2295 - Reject)
    // ========================================

    /**
     * Precondition for reject connect test.
     * Same as success - user will manually reject.
     */
    private val CONNECT_REJECT_PRECONDITION = """
```json
{
  "__meta": {}
}
```
    """.trimIndent()

    /**
     * Expected result for rejected connect test.
     * Validates that the connect response contains:
     * - event: "connect_error"
     * - payload.code: 300 (user declined connection)
     */
    private val CONNECT_REJECT_EXPECTED_RESULT = """
```json
{
  "event": "connect_error",
  "payload": {
    "code": 300,
    "message": isValidString
  }
}
```
    """.trimIndent()

    // ========================================
    // Send Transaction Test Data
    // ========================================

    /**
     * Precondition for send transaction test.
     * Contains the transaction payload to send.
     */
    private val SEND_TX_PRECONDITION = """
```json
{
  "messages": [
    {
      "address": "EQCKWpx7cNMpvmcN5ObM5lLUZHZRFKqYA4xmw9jOry0ZsF9M",
      "amount": "1000",
      "stateInit": "te6cckEBBAEAOgACATQCAQAAART/APSkE/S88sgLAwBI0wHQ0wMBcbCRW+D6QDBwgBDIywVYzxYh+gLLagHPFsmAQPsAlxCarA==",
      "payload": "te6ccsEBAQEADAAMABQAAAAASGVsbG8hCaTc/g=="
    }
  ]
}
```
    """.trimIndent()

    /**
     * Expected result for send transaction test.
     */
    private val SEND_TX_EXPECTED_RESULT = """
```json
{
  "result": isValidSendTransactionBoc,
  "id": isValidSendTransactionId
}
```
    """.trimIndent()

    /**
     * Expected result for rejected send transaction test.
     */
    private val SEND_TX_REJECT_EXPECTED_RESULT = """
```json
{
  "error": {
    "code": 300,
    "message": isValidString
  },
  "id": isValidSendTransactionId
}
```
    """.trimIndent()

    // ========================================
    // Sign Data Test Data
    // ========================================

    /**
     * Precondition for sign data test.
     * Contains the data to sign.
     */
    private val SIGN_DATA_PRECONDITION = """
```json
{
  "type": "binary",
  "bytes": "SSBjb25maXJtIHRoaXMgdGVzdCBzaWduYXR1cmUgcmVxdWVzdC4=",
  "from": sender('raw'),
  "network": "-239"
}
```
    """.trimIndent()

    /**
     * Expected result for sign data test.
     */
    private val SIGN_DATA_EXPECTED_RESULT = """
```json
{
  "id": isValidSignDataId,
  "result": {
    "signature": isValidDataSignature,
    "address": isValidRawAddressString,
    "timestamp": isValidCurrentTimestamp,
    "domain": appHost(),
    "payload": {
      "type": "binary",
      "bytes": "SSBjb25maXJtIHRoaXMgdGVzdCBzaWduYXR1cmUgcmVxdWVzdC4=",
      "from": sender('raw'),
      "network": "-239"
    }
  }
}
```
    """.trimIndent()

    /**
     * Expected result for rejected sign data test.
     */
    private val SIGN_DATA_REJECT_EXPECTED_RESULT = """
```json
{
  "error": {
    "code": 300,
    "message": isValidString
  },
  "id": isValidSignDataId
}
```
    """.trimIndent()
}
