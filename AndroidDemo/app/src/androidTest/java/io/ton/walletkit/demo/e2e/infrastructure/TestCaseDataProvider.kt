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
 * IMPORTANT: These hardcoded IDs are TEST RESULT IDs from /api/testresult/,
 * NOT test case IDs. Test result IDs (54898, 55063, etc.) contain the JSON
 * precondition/expectedResult data that can be parsed by the test runner.
 *
 * @see https://allure-test-runner.vercel.app/e2e
 */
object TestCaseDataProvider {

    /**
     * Get test case data by Allure ID.
     * First tries the Allure API (if client provided), falls back to hardcoded data.
     */
    fun getTestCaseData(allureId: String, allureClient: AllureApiClient? = null): TestCaseData? {
        // Try Allure API first if client is provided
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
     *
     * IDs map to Allure TestOps TEST RESULT IDs (from /api/testresult/):
     * - 54898: [In-Wallet browser] Connect (testCaseId: 1289)
     * - 55098: User declined the connection (testCaseId: 1095)
     * - 54889: [In-Wallet browser] Send transaction (testCaseId: 1297)
     * - 54913: User declined the transaction (testCaseId: 1122)
     * - 54903: Sign text (testCaseId: 1148)
     * - 54916: Sign binary (testCaseId: 1149)
     * - 54901: Sign cell (testCaseId: 1150)
     */
    private fun getHardcodedTestCaseData(allureId: String): TestCaseData? = when (allureId) {
        // ========================================
        // Connect tests (Test Result IDs from launch 6874)
        // ========================================
        "54898" -> TestCaseData( // CONNECT_IN_WALLET_BROWSER, CONNECT_INJECTED_WEBVIEW
            precondition = CONNECT_SUCCESS_PRECONDITION,
            expectedResult = CONNECT_SUCCESS_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "55098" -> TestCaseData( // CONNECT_USER_DECLINED
            precondition = CONNECT_SUCCESS_PRECONDITION,
            expectedResult = CONNECT_REJECT_EXPECTED_RESULT,
            isPositiveCase = false,
        )
        "54888" -> TestCaseData( // CONNECT_WITHOUT_TON_PROOF
            precondition = CONNECT_NO_TON_PROOF_PRECONDITION,
            expectedResult = CONNECT_NO_TON_PROOF_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "55107", "55063" -> TestCaseData( // CONNECT_FAKE_MANIFEST_URL (Universal/Custom QR)
            precondition = CONNECT_FAKE_MANIFEST_PRECONDITION,
            expectedResult = CONNECT_FAKE_MANIFEST_EXPECTED_RESULT,
            isPositiveCase = false,
        )
        "54857", "54865" -> TestCaseData( // CONNECT_FAKE_URL_IN_MANIFEST (Universal/Custom QR)
            precondition = CONNECT_FAKE_MANIFEST_PRECONDITION,
            expectedResult = CONNECT_FAKE_URL_IN_MANIFEST_EXPECTED_RESULT,
            isPositiveCase = false,
        )
        "55293", "54894", "54899" -> TestCaseData( // Desktop/Mobile Chrome Connect
            precondition = CONNECT_SUCCESS_PRECONDITION,
            expectedResult = CONNECT_SUCCESS_EXPECTED_RESULT,
            isPositiveCase = true,
        )

        // ========================================
        // Send Transaction tests (Test Result IDs)
        // ========================================
        "54889" -> TestCaseData( // TX_IN_WALLET_BROWSER, TX_SEND_INJECTED_WEBVIEW
            precondition = SEND_TX_PRECONDITION,
            expectedResult = SEND_TX_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "54876", "54905", "54885" -> TestCaseData( // TX_SEND_HTTP_BRIDGE, Mobile Chrome, TMA
            precondition = SEND_TX_PRECONDITION,
            expectedResult = SEND_TX_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "54913" -> TestCaseData( // TX_USER_DECLINED
            precondition = SEND_TX_PRECONDITION,
            expectedResult = SEND_TX_REJECT_EXPECTED_RESULT,
            isPositiveCase = false,
        )

        // ========================================
        // Sign Data tests (Test Result IDs)
        // ========================================
        "54903" -> TestCaseData( // SIGN_DATA_TEXT
            precondition = SIGN_DATA_TEXT_PRECONDITION,
            expectedResult = SIGN_DATA_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "54916" -> TestCaseData( // SIGN_DATA_BINARY
            precondition = SIGN_DATA_BINARY_PRECONDITION,
            expectedResult = SIGN_DATA_EXPECTED_RESULT,
            isPositiveCase = true,
        )
        "54901" -> TestCaseData( // SIGN_DATA_CELL
            precondition = SIGN_DATA_CELL_PRECONDITION,
            expectedResult = SIGN_DATA_EXPECTED_RESULT,
            isPositiveCase = true,
        )

        // ========================================
        // Legacy IDs (keep for backward compatibility)
        // ========================================
        else -> null
    }

    // ========================================
    // Connect Test Data - Success
    // ========================================

    private val CONNECT_SUCCESS_PRECONDITION = """
```json
{
  "__meta": {}
}
```
    """.trimIndent()

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
    // Connect Test Data - Reject
    // ========================================

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
    // Connect Test Data - No ton_proof
    // ========================================

    private val CONNECT_NO_TON_PROOF_PRECONDITION = """
```json
{
  "__meta": {
    "excludeTonProof": true
  }
}
```
    """.trimIndent()

    private val CONNECT_NO_TON_PROOF_EXPECTED_RESULT = """
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
    // Connect Test Data - Fake manifest URL
    // ========================================

    private val CONNECT_FAKE_MANIFEST_PRECONDITION = """
```json
{
  "__meta": {
    "manifestUrl": "https://tonkeeper/tonconnect-manifest.json"
  }
}
```
    """.trimIndent()

    private val CONNECT_FAKE_MANIFEST_EXPECTED_RESULT = """
```json
{
    "event": "connect_error",
    "payload": {
        "code": 2,
        "message": "App manifest not found"
    },
    "id": isNonNegativeInt
}
```
    """.trimIndent()

    private val CONNECT_FAKE_URL_IN_MANIFEST_EXPECTED_RESULT = """
```json
{
    "event": "connect_error",
    "payload": {
        "code": 3,
        "message": isValidString
    },
    "id": isNonNegativeInt
}
```
    """.trimIndent()

    // ========================================
    // Send Transaction Test Data
    // ========================================

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

    private val SEND_TX_EXPECTED_RESULT = """
```json
{
  "result": isValidSendTransactionBoc,
  "id": isValidSendTransactionId
}
```
    """.trimIndent()

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

    private val SIGN_DATA_TEXT_PRECONDITION = """
```json
{
  "type": "text",
  "text": "I confirm this test signature request."
}
```
    """.trimIndent()

    private val SIGN_DATA_BINARY_PRECONDITION = """
```json
{
  "type": "binary",
  "bytes": "SSBjb25maXJtIHRoaXMgdGVzdCBzaWduYXR1cmUgcmVxdWVzdC4="
}
```
    """.trimIndent()

    private val SIGN_DATA_CELL_PRECONDITION = """
```json
{
  "type": "cell",
  "schema": "message#_ len:uint16 text:(bits len) = Message",
  "cell": "te6ccsEBAQEAFgAAKAAoSSBjb25maXJtIHRoaXMgdGVzdC5lJ2Uc"
}
```
    """.trimIndent()

    private val SIGN_DATA_EXPECTED_RESULT = """
```json
{
  "id": isValidSignDataId,
  "result": {
    "signature": isValidDataSignature,
    "timestamp": isValidCurrentTimestamp
  }
}
```
    """.trimIndent()
}
