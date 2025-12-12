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
package io.ton.walletkit.demo.e2e.tests

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Severity
import io.qameta.allure.kotlin.SeverityLevel
import io.qameta.allure.kotlin.Story
import io.ton.walletkit.demo.e2e.infrastructure.AllureTestIds
import io.ton.walletkit.demo.e2e.infrastructure.BaseE2ETest
import io.ton.walletkit.demo.e2e.infrastructure.ConnectTest
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.After
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E tests for TonConnect connection flow.
 *
 * Tests verify wallet can successfully connect to a dApp using TonConnect protocol
 * via different methods: HTTP bridge (copy link), injected WebView, and error cases.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Connect")
class ConnectE2ETest : BaseE2ETest() {

    companion object {
        private const val TAG = "ConnectE2ETest"

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            cleanupAllData()
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    override fun setUp() {
        super.setUp()
        composeTestRule.waitForIdle()
        walletController.init(composeTestRule)
        dAppController.init(composeTestRule)
        ensureWalletReady()
    }

    @After
    fun tearDown() {
        // Clean up dApp sessions to ensure test isolation for connect tests
        try {
            dAppController.openBrowser()
            dAppController.waitForDAppPage(timeoutMs = 10000)
            dAppController.clearDAppStorage()
            dAppController.closeBrowserFully()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to clean up dApp sessions in tearDown: ${e.message}")
        }
    }

    // =========================================================================
    // Basic Connect Tests
    // =========================================================================

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_IN_WALLET_BROWSER)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify wallet can successfully connect to a dApp via HTTP bridge")
    fun testSuccessfulConnect() = runHttpBridgeConnectTest(AllureTestIds.CONNECT_IN_WALLET_BROWSER)

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_USER_DECLINED)
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify user can reject a connection request")
    fun testRejectConnect() = runHttpBridgeConnectTest(AllureTestIds.CONNECT_USER_DECLINED, approve = false)

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_USER_DECLINED_1889)
    @Severity(SeverityLevel.NORMAL)
    @Description("User declined the connection")
    fun test01_ConnectUserDeclined() = runHttpBridgeConnectTest(AllureTestIds.CONNECT_USER_DECLINED_1889, approve = false)

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_INJECTED_WEBVIEW)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[In-Wallet browser][Mobile wallet] Connect wallet to dApp via injected WebView")
    fun testConnectViaInjectedWebView() = runInjectedConnectTest(AllureTestIds.CONNECT_INJECTED_WEBVIEW)

    // =========================================================================
    // Connect with/without ton_proof
    // =========================================================================

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_WITHOUT_TON_PROOF)
    @Severity(SeverityLevel.NORMAL)
    @Description("Wallet supports connect to dApp with no ton_proof required")
    fun testConnectWithoutTonProof() = runHttpBridgeConnectTest(AllureTestIds.CONNECT_WITHOUT_TON_PROOF)

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_IN_WALLET_BROWSER)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wallet supports connect to dApp with ton_proof required")
    fun testConnectWithTonProof() = runHttpBridgeConnectTest(AllureTestIds.CONNECT_IN_WALLET_BROWSER)

    // =========================================================================
    // Fake manifest URL tests (error cases - no connect sheet expected)
    // =========================================================================

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_MANIFEST_URL_CUSTOM_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake manifest domain via Wallet QR Code")
    fun testConnectFakeManifestUrlWalletQR() = runHttpBridgeConnectTest(
        AllureTestIds.CONNECT_FAKE_MANIFEST_URL_CUSTOM_QR,
        expectConnectSheet = false,
    )

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_MANIFEST_URL_UNIVERSAL_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake manifest domain via universal QR Code")
    fun testConnectFakeManifestUrlUniversalQR() = runHttpBridgeConnectTest(
        AllureTestIds.CONNECT_FAKE_MANIFEST_URL_UNIVERSAL_QR,
        expectConnectSheet = false,
    )

    // =========================================================================
    // Fake URL in manifest tests (error cases)
    // =========================================================================

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_CUSTOM_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake URL in manifest via Wallet QR Code")
    fun testConnectFakeUrlInManifestWalletQR() = runHttpBridgeConnectTest(
        AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_CUSTOM_QR,
        expectConnectSheet = false,
    )

    @Test
    @ConnectTest
    @AllureId(AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_UNIVERSAL_QR)
    @Severity(SeverityLevel.NORMAL)
    @Description("[Desktop browser][Mobile wallet] Connect wallet to dApp with fake URL in manifest via universal QR Code")
    fun testConnectFakeUrlInManifestUniversalQR() = runHttpBridgeConnectTest(
        AllureTestIds.CONNECT_FAKE_URL_IN_MANIFEST_UNIVERSAL_QR,
        expectConnectSheet = false,
    )
}
