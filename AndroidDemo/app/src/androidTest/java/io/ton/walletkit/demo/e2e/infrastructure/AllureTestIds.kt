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
 * Allure TestOps test result IDs for E2E tests.
 *
 * These IDs correspond to test results in https://tontech.testops.cloud (project 67, launch 6874)
 * and are used for:
 * - Linking test results to test cases via @AllureId annotation
 * - Fetching preconditions and expected results from API via /api/testresult/
 *
 * IMPORTANT: These are TEST RESULT IDs from the API (/api/testresult/),
 * NOT test case IDs from /api/rs/testcase/.
 * Test result IDs contain the JSON precondition/expectedResult data that can be parsed.
 *
 * Tests are organized into 5 groups for parallel CI execution:
 * - Group 1: Connect tests
 * - Group 2: Transaction - Address & Amount
 * - Group 3: Transaction - From & Messages & Network
 * - Group 4: Transaction - Payload, StateInit, ValidUntil
 * - Group 5: Transaction - Merkle & Jetton + Sign Data
 */
object AllureTestIds {

    // =====================================================================
    // GROUP 1: CONNECT TESTS
    // =====================================================================

    /** Test Result 54898: [In-Wallet browser] Connect (testCaseId: 1289) */
    const val CONNECT_IN_WALLET_BROWSER = "54898"

    /** Test Result 54898: [In-Wallet browser] Connect - injected WebView (testCaseId: 1289) */
    const val CONNECT_INJECTED_WEBVIEW = "54898"

    /** Test Result 55098: User declined the connection (testCaseId: 1095) */
    const val CONNECT_USER_DECLINED = "55098"

    /** Test Result 54888: Connect without ton_proof (testCaseId: 1117) */
    const val CONNECT_WITHOUT_TON_PROOF = "54888"

    /** Test Result 55107: [Fake manifest URL] Connect error (Universal QR) (testCaseId: 1110) */
    const val CONNECT_FAKE_MANIFEST_URL_UNIVERSAL_QR = "55107"

    /** Test Result 55063: [Fake manifest URL] Connect error (Custom QR) (testCaseId: 1111) */
    const val CONNECT_FAKE_MANIFEST_URL_CUSTOM_QR = "55063"

    /** Test Result 54857: [Fake URL in manifest] Connect error (Universal QR) (testCaseId: 1112) */
    const val CONNECT_FAKE_URL_IN_MANIFEST_UNIVERSAL_QR = "54857"

    /** Test Result 54865: [Fake URL in manifest] Connect error (Custom QR) (testCaseId: 1113) */
    const val CONNECT_FAKE_URL_IN_MANIFEST_CUSTOM_QR = "54865"

    /** Test Result 55293: [Desktop browser] Connect (Custom QR) (testCaseId: 1294) */
    const val CONNECT_DESKTOP_CUSTOM_QR = "55293"

    /** Test Result 54894: [Desktop browser] Connect (Universal QR) (testCaseId: 1295) */
    const val CONNECT_DESKTOP_UNIVERSAL_QR = "54894"

    /** Test Result 54899: [Mobile Chrome] Connect (testCaseId: 1288) */
    const val CONNECT_MOBILE_CHROME = "54899"

    /** Test Result 54868: [TMA dApp] Connect (Custom QR) (testCaseId: 1296) */
    const val CONNECT_TMA_CUSTOM_QR = "54868"

    /** Test Result 54912: [TMA dApp] Connect (Universal QR) (testCaseId: 1638) */
    const val CONNECT_TMA_UNIVERSAL_QR = "54912"

    // =====================================================================
    // GROUP 2: TRANSACTION - ADDRESS & AMOUNT
    // =====================================================================

    /** Test Result 54867: [address] Error if absent (testCaseId: 1015) */
    const val TX_ADDRESS_ERROR_ABSENT = "54867"

    /** Test Result 54890: [address] Error if in HEX format (testCaseId: 1040) */
    const val TX_ADDRESS_ERROR_HEX_FORMAT = "54890"

    /** Test Result 54884: [address] Error if invalid value (testCaseId: 1024) */
    const val TX_ADDRESS_ERROR_INVALID = "54884"

    /** Test Result 54880: [address] Success if in bounceable format (testCaseId: 1020) */
    const val TX_ADDRESS_SUCCESS_BOUNCEABLE = "54880"

    /** Test Result 55105: [address] Success if in non-bounceable format (testCaseId: 1021) */
    const val TX_ADDRESS_SUCCESS_NON_BOUNCEABLE = "55105"

    /** Test Result 54915: [amount] Error if absent (testCaseId: 1043) */
    const val TX_AMOUNT_ERROR_ABSENT = "54915"

    /** Test Result 54878: [amount] Error if as a number (testCaseId: 1025) */
    const val TX_AMOUNT_ERROR_AS_NUMBER = "54878"

    /** Test Result 55104: [amount] Error if insufficient balance (testCaseId: 1041) */
    const val TX_AMOUNT_ERROR_INSUFFICIENT_BALANCE = "55104"

    /** Test Result 54873: [amount] Success if '0' (testCaseId: 1672) */
    const val TX_AMOUNT_SUCCESS_ZERO = "54873"

    /** Test Result 54881: [amount] Success if as a string (testCaseId: 1017) */
    const val TX_AMOUNT_SUCCESS_AS_STRING = "54881"

    // =====================================================================
    // GROUP 3: TRANSACTION - FROM & MESSAGES & NETWORK
    // =====================================================================

    /** Test Result 54872: [from] Error if address doesn't match the user's wallet address (testCaseId: 1048) */
    const val TX_FROM_ERROR_ADDRESS_MISMATCH = "54872"

    /** Test Result 54870: [from] Error if invalid value (testCaseId: 1016) */
    const val TX_FROM_ERROR_INVALID = "54870"

    /** Test Result 54886: [from] Success if in bounceable format (testCaseId: 1049) */
    const val TX_FROM_SUCCESS_BOUNCEABLE = "54886"

    /** Test Result 54887: [from] Success if in HEX format (testCaseId: 1023) */
    const val TX_FROM_SUCCESS_HEX = "54887"

    /** Test Result 55099: [from] Success if in non-bounceable format (testCaseId: 1030) */
    const val TX_FROM_SUCCESS_NON_BOUNCEABLE = "55099"

    /** Test Result 54907: [messages] Error if array is empty (testCaseId: 1032) */
    const val TX_MESSAGES_ERROR_EMPTY_ARRAY = "54907"

    /** Test Result 54895: [messages] Error if contains invalid message (testCaseId: 1038) */
    const val TX_MESSAGES_ERROR_INVALID_MESSAGE = "54895"

    /** Test Result 54882: [messages] Success if contains maximum messages (testCaseId: 1346) */
    const val TX_MESSAGES_SUCCESS_MAX_MESSAGES = "54882"

    /** Test Result 54866: [network] Error if '-3' (testnet) (testCaseId: 1046) */
    const val TX_NETWORK_ERROR_TESTNET = "54866"

    /** Test Result 54892: [network] Error if as a number (testCaseId: 1028) */
    const val TX_NETWORK_ERROR_AS_NUMBER = "54892"

    /** Test Result 54906: [network] Success if '-239' (mainnet) (testCaseId: 1045) */
    const val TX_NETWORK_SUCCESS_MAINNET = "54906"

    // =====================================================================
    // GROUP 4: TRANSACTION - PAYLOAD, STATEINIT, VALIDUNTIL
    // =====================================================================

    /** Test Result 54863: [payload] Error if invalid value (testCaseId: 1042) */
    const val TX_PAYLOAD_ERROR_INVALID = "54863"

    /** Test Result 54879: [payload] Success if absent (testCaseId: 1022) */
    const val TX_PAYLOAD_SUCCESS_ABSENT = "54879"

    /** Test Result 54860: [payload] Success if valid value (testCaseId: 1050) */
    const val TX_PAYLOAD_SUCCESS_VALID = "54860"

    /** Test Result 54908: [stateInit] Error if invalid value (testCaseId: 1044) */
    const val TX_STATEINIT_ERROR_INVALID = "54908"

    /** Test Result 55103: [stateInit] Success if absent (testCaseId: 1027) */
    const val TX_STATEINIT_SUCCESS_ABSENT = "55103"

    /** Test Result 54874: [stateInit] Success if valid value (testCaseId: 1018) */
    const val TX_STATEINIT_SUCCESS_VALID = "54874"

    /** Test Result 54897: [validUntil] Error if as a string (testCaseId: 1033) */
    const val TX_VALIDUNTIL_ERROR_AS_STRING = "54897"

    /** Test Result 54871: [validUntil] Error if expired (testCaseId: 1029) */
    const val TX_VALIDUNTIL_ERROR_EXPIRED = "54871"

    /** Test Result 55106: [validUntil] Error if has expired during confirmation (testCaseId: 1031) */
    const val TX_VALIDUNTIL_ERROR_EXPIRED_DURING_CONFIRM = "55106"

    /** Test Result 54862: [validUntil] if NaN (testCaseId: 1035) */
    const val TX_VALIDUNTIL_NAN = "54862"

    /** Test Result 54875: [validUntil] if NULL (testCaseId: 1037) */
    const val TX_VALIDUNTIL_NULL = "54875"

    /** Test Result 55108: [validUntil] Success if absent (testCaseId: 1034) */
    const val TX_VALIDUNTIL_SUCCESS_ABSENT = "55108"

    /** Test Result 54891: [validUntil] Success if less than in 5 minutes (testCaseId: 1019) */
    const val TX_VALIDUNTIL_SUCCESS_LESS_THAN_5_MIN = "54891"

    /** Test Result 54864: [validUntil] Success if more then in 5 minutes (testCaseId: 1026) */
    const val TX_VALIDUNTIL_SUCCESS_MORE_THAN_5_MIN = "54864"

    // =====================================================================
    // GROUP 5: TRANSACTION - MERKLE & JETTON + SIGN DATA
    // =====================================================================

    /** Test Result 54889: [In-Wallet browser] Send transaction (testCaseId: 1297) */
    const val TX_IN_WALLET_BROWSER = "54889"

    /** Test Result 54889: [In-Wallet browser] Send transaction - injected WebView (testCaseId: 1297) */
    const val TX_SEND_INJECTED_WEBVIEW = "54889"

    /** Test Result 54876: [Desktop browser] Send transaction (testCaseId: 1291) */
    const val TX_SEND_HTTP_BRIDGE = "54876"

    /** Test Result 54905: [Mobile Chrome] Send transaction (testCaseId: 1290) */
    const val TX_SEND_MOBILE_CHROME = "54905"

    /** Test Result 54885: [TMA dApp] Send transaction (testCaseId: 1293) */
    const val TX_SEND_TMA = "54885"

    /** Test Result 54913: User declined the transaction (testCaseId: 1122) */
    const val TX_USER_DECLINED = "54913"

    /** Test Result 54904: Minting Jetton with Deployed Contract (testCaseId: 1109) */
    const val TX_MINTING_JETTON_DEPLOYED = "54904"

    /** Test Result 54883: Minting Jetton with Undeployed Contract (testCaseId: 1108) */
    const val TX_MINTING_JETTON_UNDEPLOYED = "54883"

    /** Test Result 54900: Send merkle proof (testCaseId: 1146) */
    const val TX_SEND_MERKLE_PROOF = "54900"

    /** Test Result 54858: Send merkle update (testCaseId: 1147) */
    const val TX_SEND_MERKLE_UPDATE = "54858"

    // =====================================================================
    // SIGN DATA TESTS
    // =====================================================================

    /** Test Result 54903: Sign text (testCaseId: 1148) */
    const val SIGN_DATA_TEXT = "54903"

    /** Test Result 54916: Sign binary (testCaseId: 1149) */
    const val SIGN_DATA_BINARY = "54916"

    /** Test Result 54901: Sign cell (testCaseId: 1150) */
    const val SIGN_DATA_CELL = "54901"

    // =====================================================================
    // EXTRA CURRENCY
    // =====================================================================

    /** Test Result 54914: Check ExtraCurrency feature (testCaseId: 1119) */
    const val EXTRA_CURRENCY = "54914"

    // =====================================================================
    // BACKWARD COMPATIBILITY (aliases for existing tests)
    // =====================================================================

    /** Alias for CONNECT_IN_WALLET_BROWSER for existing tests */
    const val CONNECT_SUCCESS = CONNECT_IN_WALLET_BROWSER
}
