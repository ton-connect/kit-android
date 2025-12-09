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
import io.ton.walletkit.demo.e2e.infrastructure.SendTransactionTest
import io.ton.walletkit.demo.e2e.infrastructure.SignDataTest
import io.ton.walletkit.demo.presentation.MainActivity
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for Merkle, Jetton & Sign Data.
 *
 * Tests cover:
 * - Send transaction in wallet browser
 * - User declined transaction
 * - Minting Jetton (deployed/undeployed)
 * - Merkle proof/update
 * - Sign Data (text, binary, cell)
 * - Extra Currency
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Feature("TonConnect")
@Story("Merkle & Jetton & Sign Data")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MerkleJettonSignDataE2ETest : BaseE2ETest() {

    companion object {
        @Volatile
        private var isConnected = false

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

    private fun ensureConnectedForSendTx() {
        if (isConnected) return

        dAppController.openBrowser()
        dAppController.waitForDAppPage()
        dAppController.clickSendTransaction()
        val connectUrl = dAppController.clickCopyLinkInModal()
        dAppController.closeBrowserFully()
        walletController.connectByUrl(connectUrl)
        waitFor(timeoutMs = 10000) { walletController.isConnectRequestVisible() }
        walletController.approveConnect()
        waitFor(timeoutMs = 5000) { !walletController.isConnectRequestVisible() }
        isConnected = true
    }

    private fun ensureConnectedForSignData() {
        if (isConnected) return

        dAppController.openBrowser()
        dAppController.waitForDAppPage()
        dAppController.clickSignData()
        val connectUrl = dAppController.clickCopyLinkInModal()
        dAppController.closeBrowserFully()
        walletController.connectByUrl(connectUrl)
        waitFor(timeoutMs = 10000) { walletController.isConnectRequestVisible() }
        walletController.approveConnect()
        waitFor(timeoutMs = 5000) { !walletController.isConnectRequestVisible() }
        isConnected = true
    }

    // =====================================================================
    // SEND TRANSACTION TESTS
    // =====================================================================

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_IN_WALLET_BROWSER)
    @Severity(SeverityLevel.CRITICAL)
    @Description("[In-Wallet browser] Send transaction")
    fun test01_SendTransactionInWalletBrowser() {
        runSendTxTest(AllureTestIds.TX_IN_WALLET_BROWSER, ensureConnected = ::ensureConnectedForSendTx)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_USER_DECLINED)
    @Severity(SeverityLevel.CRITICAL)
    @Description("User declined the transaction")
    fun test02_SendTransactionUserDeclined() {
        runSendTxTest(AllureTestIds.TX_USER_DECLINED, approve = false, ensureConnected = ::ensureConnectedForSendTx)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_MINTING_JETTON_DEPLOYED)
    @Severity(SeverityLevel.NORMAL)
    @Description("Minting Jetton with Deployed Contract")
    fun test03_MintingJettonDeployed() {
        runSendTxTest(AllureTestIds.TX_MINTING_JETTON_DEPLOYED, ensureConnected = ::ensureConnectedForSendTx)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_MINTING_JETTON_UNDEPLOYED)
    @Severity(SeverityLevel.NORMAL)
    @Description("Minting Jetton with Undeployed Contract")
    fun test04_MintingJettonUndeployed() {
        runSendTxTest(AllureTestIds.TX_MINTING_JETTON_UNDEPLOYED, ensureConnected = ::ensureConnectedForSendTx)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_SEND_MERKLE_PROOF)
    @Severity(SeverityLevel.NORMAL)
    @Description("Send merkle proof")
    fun test05_SendMerkleProof() {
        runSendTxTest(AllureTestIds.TX_SEND_MERKLE_PROOF, ensureConnected = ::ensureConnectedForSendTx)
    }

    @Test
    @SendTransactionTest
    @AllureId(AllureTestIds.TX_SEND_MERKLE_UPDATE)
    @Severity(SeverityLevel.NORMAL)
    @Description("Send merkle update")
    fun test06_SendMerkleUpdate() {
        runSendTxTest(AllureTestIds.TX_SEND_MERKLE_UPDATE, ensureConnected = ::ensureConnectedForSendTx)
    }

    // =====================================================================
    // SIGN DATA TESTS
    // =====================================================================

    @Test
    @SignDataTest
    @AllureId(AllureTestIds.SIGN_DATA_TEXT)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sign text")
    fun test07_SignDataText() {
        runSignDataTest(AllureTestIds.SIGN_DATA_TEXT, ensureConnected = ::ensureConnectedForSignData)
    }

    @Test
    @SignDataTest
    @AllureId(AllureTestIds.SIGN_DATA_BINARY)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sign binary")
    fun test08_SignDataBinary() {
        runSignDataTest(AllureTestIds.SIGN_DATA_BINARY, ensureConnected = ::ensureConnectedForSignData)
    }

    @Test
    @SignDataTest
    @AllureId(AllureTestIds.SIGN_DATA_CELL)
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sign cell")
    fun test09_SignDataCell() {
        runSignDataTest(AllureTestIds.SIGN_DATA_CELL, ensureConnected = ::ensureConnectedForSignData)
    }

    // =====================================================================
    // EXTRA CURRENCY TEST
    // =====================================================================

    // TODO: ExtraCurrency test requires a different flow - manual testing with a separate dApp:
    //   1. Open ExtraCurrency testing dApp: https://extra-currency-demo-stand-demo-dapp.vercel.app/
    //   2. Connect your wallet
    //   3. Follow the 12 scenarios in the dApp instructions
    //   This cannot be automated with our current test runner as it uses a different dApp.
    // @Test
    // @SendTransactionTest
    // @AllureId(AllureTestIds.EXTRA_CURRENCY)
    // @Severity(SeverityLevel.NORMAL)
    // @Description("Check ExtraCurrency feature")
    // fun test10_ExtraCurrency() {
    //     runSendTxTest(AllureTestIds.EXTRA_CURRENCY, ensureConnected = ::ensureConnectedForSendTx)
    // }
}
