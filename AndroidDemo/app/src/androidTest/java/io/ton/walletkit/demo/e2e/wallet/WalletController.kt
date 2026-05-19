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
package io.ton.walletkit.demo.e2e.wallet

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import io.qameta.allure.kotlin.Step
import io.ton.walletkit.demo.presentation.MainActivity
import io.ton.walletkit.demo.presentation.util.TestTags

/**
 * Controller for interacting with the wallet demo app via Espresso/Compose testing.
 *
 * This mirrors the wallet controller concept from the web demo-wallet.
 */
class WalletController(composeTestRule: ComposeTestRule? = null) {

    companion object {
        // Timeouts - longer for CI emulators which are slower than local machines
        const val SHEET_APPEAR_TIMEOUT = 30_000L // Wait for sheets to appear (depends on WebView/network)
        const val BUTTON_ENABLE_TIMEOUT = 15_000L // Wait for buttons to become enabled
        const val UI_IDLE_TIMEOUT = 10_000L // General UI idle/animation timeout

        const val PIN_LENGTH = 4
        const val DEFAULT_PIN = "1234"
    }

    private lateinit var composeTestRule: ComposeTestRule
    private var androidComposeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>? = null

    init {
        composeTestRule?.let { this.composeTestRule = it }
    }

    /**
     * Initialize the controller with a ComposeTestRule.
     */
    fun init(rule: ComposeTestRule) {
        this.composeTestRule = rule
        if (rule is AndroidComposeTestRule<*, *>) {
            @Suppress("UNCHECKED_CAST")
            androidComposeTestRule = rule as? AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
        }
    }

    // ===========================================
    // Wallet Setup Actions
    // ===========================================

    /**
     * Legacy home screen ([LegacyWalletScreen]) ready and not occluded by [AddWalletSheet].
     */
    fun isOnHomeScreen(): Boolean {
        if (!isOnLegacyHome()) return false
        return !isAddWalletSheetShowing()
    }

    private fun isOnLegacyHome(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.BROWSER_NO_INJECT_BUTTON).assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    private fun isOnModernHome(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.WALLET_BALANCE).assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Handle the authentication flow - either setup, unlock, or skip if already on home.
     * Automatically detects which screen we're on.
     */
    @Step("Authenticate")
    fun authenticate(pin: String = DEFAULT_PIN) {
        composeTestRule.waitForIdle()
        // Give app time to settle after activity start

        // Wait for one of the expected screens to appear (up to 15 seconds)
        // This handles the case when the app is starting up
        var screenDetected = false
        var attempts = 0
        val maxAttempts = 30 // 30 * 500ms = 15 seconds

        while (!screenDetected && attempts < maxAttempts) {
            composeTestRule.waitForIdle()

            // Check if already on home screen (no auth needed)
            if (isOnHomeScreen()) {
                Log.d("WalletController", "Detected home screen - no authentication needed")
                return
            }

            // Check if on unlock screen FIRST (more likely if wallet exists from previous test)
            val onUnlock = try {
                composeTestRule.onNodeWithTag(TestTags.UNLOCK_PIN_FIELD)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (onUnlock) {
                Log.d("WalletController", "Detected unlock screen")
                unlockPinSafe(pin)
                screenDetected = true
                return
            }

            // Check if on create PIN screen
            val onSetup = try {
                composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_FIELD)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (onSetup) {
                Log.d("WalletController", "Detected create PIN screen")
                createPinSafe(pin)
                screenDetected = true
                return
            }

            // None detected yet, wait and try again
            Log.d("WalletController", "No auth screen detected yet (attempt ${attempts + 1}/$maxAttempts)")
            attempts++
        }

        // If we get here, check one more time for home screen (maybe transitions happened)
        if (isOnHomeScreen()) {
            Log.d("WalletController", "Detected home screen after waiting")
            return
        }

        // Last resort - try unlock as fallback
        Log.w("WalletController", "No auth screen detected after $maxAttempts attempts, trying unlock")
        unlockPinSafe(pin)
    }

    /**
     * Two-phase PIN setup: type 4 digits, wait for the Save button to appear (proves the
     * 800ms onComplete grace fired and the screen advanced to Confirming with an empty
     * field), type 4 more digits, wait for Save to enable, tap it.
     */
    private fun createPinSafe(pin: String) {
        try {
            require(pin.length == PIN_LENGTH) {
                "PIN must be $PIN_LENGTH digits, got '${pin.length}'"
            }

            composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_FIELD)
                .performTextInput(pin)

            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
                composeTestRule.onAllNodesWithTag(TestTags.CREATE_PIN_SAVE_BUTTON)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_FIELD)
                .performTextInput(pin)

            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
                try {
                    composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_SAVE_BUTTON)
                        .assertIsEnabled()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }

            composeTestRule.onNodeWithTag(TestTags.CREATE_PIN_SAVE_BUTTON)
                .performClick()

            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            Log.w("WalletController", "createPinSafe failed: ${e.message}")
        }
    }

    private fun unlockPinSafe(pin: String) {
        try {
            require(pin.length == PIN_LENGTH) {
                "PIN must be $PIN_LENGTH digits, got '${pin.length}'"
            }
            composeTestRule.onNodeWithTag(TestTags.UNLOCK_PIN_FIELD)
                .performTextInput(pin)

            try {
                composeTestRule.waitUntil(UI_IDLE_TIMEOUT) {
                    composeTestRule.onAllNodesWithTag(TestTags.UNLOCK_PIN_FIELD)
                        .fetchSemanticsNodes().isEmpty()
                }
            } catch (e: Exception) {
                Log.w("WalletController", "Unlock screen did not dismiss within timeout: ${e.message}")
            }
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            Log.w("WalletController", "unlockPinSafe failed: ${e.message}")
        }
    }

    /**
     * After import we land on the modern [WalletScreen]; the e2e suite drives the legacy
     * one, reachable via the 5-tap dev gesture on the balance tile. No-op if already there.
     */
    @Step("Switch to legacy wallet screen")
    fun ensureLegacyScreen() {
        composeTestRule.waitForIdle()
        if (isOnLegacyHome()) return

        try {
            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) { isOnModernHome() }
        } catch (e: Exception) {
            Log.w("WalletController", "ensureLegacyScreen: modern home never appeared: ${e.message}")
            return
        }

        repeat(5) {
            composeTestRule.onNodeWithTag(TestTags.WALLET_BALANCE).performClick()
        }
        composeTestRule.waitForIdle()

        try {
            composeTestRule.waitUntil(UI_IDLE_TIMEOUT) { isOnLegacyHome() }
        } catch (e: Exception) {
            Log.w("WalletController", "ensureLegacyScreen: legacy home did not appear: ${e.message}")
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Import a wallet using mnemonic.
     */
    @Step("Import wallet with mnemonic")
    fun importWallet(mnemonic: List<String>) {
        Log.d("WalletController", "importWallet called with ${mnemonic.size} words")
        val mnemonicString = mnemonic.joinToString(" ")
        Log.d("WalletController", "Mnemonic to enter: '${mnemonicString.take(50)}...' (${mnemonicString.length} chars)")

        // Wait for mnemonic field (the paste field in AddWalletSheet)
        Log.d("WalletController", "Waiting for MNEMONIC_FIELD...")
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.MNEMONIC_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }
        Log.d("WalletController", "MNEMONIC_FIELD found, entering mnemonic...")

        // Clear field first and enter mnemonic
        composeTestRule.onNodeWithTag(TestTags.MNEMONIC_FIELD)
            .performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.MNEMONIC_FIELD)
            .performTextInput(mnemonicString)
        Log.d("WalletController", "Mnemonic text input performed")

        // Wait for auto-parsing to complete
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Give time for mnemonic parsing
        Log.d("WalletController", "Waited for parsing")

        // Scroll to the import button (it may be below the visible area)
        Log.d("WalletController", "Scrolling to IMPORT_WALLET_PROCESS_BUTTON...")
        try {
            composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_PROCESS_BUTTON)
                .performScrollTo()
            Log.d("WalletController", "Scrolled to import button")
        } catch (e: Exception) {
            Log.w("WalletController", "Could not scroll to import button: ${e.message}")
        }

        composeTestRule.waitForIdle()
        Thread.sleep(500) // Let scroll animation complete

        // Click import button
        Log.d("WalletController", "Clicking IMPORT_WALLET_PROCESS_BUTTON...")
        try {
            composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_PROCESS_BUTTON)
                .performClick()
            Log.d("WalletController", "Import button clicked successfully")
        } catch (e: Exception) {
            Log.e("WalletController", "Failed to click import button: ${e.message}")
            throw e
        }

        composeTestRule.waitForIdle()
        try {
            composeTestRule.waitUntil(15000) { isOnLegacyHome() || isOnModernHome() }
            Log.d("WalletController", "Wallet loaded — legacy=${isOnLegacyHome()}, modern=${isOnModernHome()}")
        } catch (e: Exception) {
            Log.e("WalletController", "Wallet home not visible after import: ${e.message}")
        }
    }

    /**
     * Check if the AddWalletSheet is currently showing.
     */
    private fun isAddWalletSheetShowing(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.ADD_WALLET_TAB_GENERATE)
            .assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Complete the full wallet setup flow.
     * Handles both fresh setup (SetupPasswordScreen) and existing setup (UnlockWalletScreen).
     *
     * For e2e tests that need a specific wallet:
     * - If no wallet exists: sets up password and imports wallet
     * - If wallet already exists: unlocks and checks if AddWalletSheet is shown
     *   - If AddWalletSheet: imports wallet
     *   - If home screen: wallet already set up, skip import (assume same wallet)
     */
    @Step("Complete wallet setup")
    fun setupWallet(mnemonic: List<String>, pin: String = DEFAULT_PIN) {
        Log.d("WalletController", "=== setupWallet called with ${mnemonic.size} word mnemonic ===")
        composeTestRule.waitForIdle()

        if (isOnHomeScreen()) {
            Log.d("WalletController", "Already on legacy home - wallet exists, skipping setup")
            return
        }
        if (isAddWalletSheetShowing()) {
            Log.d("WalletController", "AddWalletSheet showing - importing then toggling to legacy")
            importWallet(mnemonic)
            ensureLegacyScreen()
            return
        }
        if (isOnModernHome()) {
            Log.d("WalletController", "On modern home - toggling to legacy")
            ensureLegacyScreen()
            return
        }

        authenticate(pin)
        composeTestRule.waitForIdle()

        for (i in 1..10) {
            composeTestRule.waitForIdle()

            if (isOnHomeScreen()) {
                Log.d("WalletController", "Legacy home after auth (check $i)")
                return
            }
            if (isAddWalletSheetShowing()) {
                Log.d("WalletController", "AddWalletSheet after auth (check $i) - importing")
                importWallet(mnemonic)
                ensureLegacyScreen()
                return
            }
            if (isOnModernHome()) {
                Log.d("WalletController", "Modern home after auth (check $i) - toggling to legacy")
                ensureLegacyScreen()
                return
            }

            Thread.sleep(200)
        }

        Log.w("WalletController", "No expected screen after 10 checks, falling back to import")
        importWallet(mnemonic)
        ensureLegacyScreen()
    }

    // ===========================================
    // TonConnect URL Handling
    // ===========================================

    /**
     * Connect to a dApp by pasting a TonConnect URL.
     * This opens the "Handle URL" dialog, pastes the URL, and processes it.
     *
     * @param url The TonConnect URL (tc://... or https://...)
     */
    @Step("Connect by TonConnect URL")
    fun connectByUrl(url: String) {
        // Wait for wallet home screen with Handle URL button
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.HANDLE_URL_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click "Handle URL" button to open dialog
        composeTestRule.onNodeWithTag(TestTags.HANDLE_URL_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for the URL input field in the dialog
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithTag(TestTags.TONCONNECT_URL_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Enter the TonConnect URL
        composeTestRule.onNodeWithTag(TestTags.TONCONNECT_URL_FIELD)
            .performTextInput(url)

        // Click the process button
        composeTestRule.onNodeWithTag(TestTags.TONCONNECT_PROCESS_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Wait for the wallet home screen to be visible.
     * Uses the BROWSER_NO_INJECT_BUTTON which is reliably present on the home screen.
     */
    @Step("Wait for wallet home screen")
    fun waitForWalletHome() {
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.WALLET_ADDRESS)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ===========================================
    // TonConnect Actions
    // ===========================================

    /**
     * Approve a TonConnect connection request.
     */
    @Step("Approve connection request")
    fun approveConnect() {
        // Wait for connect request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.CONNECT_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait a bit for the sheet to fully load and button to become enabled
        Thread.sleep(500)
        composeTestRule.waitForIdle()

        // Wait for button to be enabled (may take time to verify manifest)
        composeTestRule.waitUntil(BUTTON_ENABLE_TIMEOUT) {
            try {
                composeTestRule.onNodeWithTag(TestTags.CONNECT_APPROVE_BUTTON)
                    .assertIsEnabled()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Click approve button
        composeTestRule.onNodeWithTag(TestTags.CONNECT_APPROVE_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Reject a TonConnect connection request.
     */
    @Step("Reject connection request")
    fun rejectConnect() {
        // Wait for connect request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.CONNECT_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Wait a bit for the sheet to fully load
        Thread.sleep(500)
        composeTestRule.waitForIdle()

        // Click reject button
        composeTestRule.onNodeWithTag(TestTags.CONNECT_REJECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Approve a transaction request.
     */
    @Step("Approve transaction request")
    fun approveTransaction() {
        Log.d("WalletController", "approveTransaction: waiting for sheet")
        // Wait for transaction request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        Log.d("WalletController", "approveTransaction: clicking approve button")
        // Click approve button
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_APPROVE_BUTTON)
            .performClick()

        Log.d("WalletController", "approveTransaction: waiting for idle")
        composeTestRule.waitForIdle()
        Log.d("WalletController", "approveTransaction: done")
    }

    /**
     * Reject a transaction request.
     */
    @Step("Reject transaction request")
    fun rejectTransaction() {
        // Wait for transaction request sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click reject button
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_REJECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Approve a sign data request.
     */
    @Step("Approve sign data request")
    fun approveSignData() {
        // Wait for sign data sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.SIGN_DATA_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click approve button
        composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_APPROVE_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Reject a sign data request.
     */
    @Step("Reject sign data request")
    fun rejectSignData() {
        // Wait for sign data sheet
        composeTestRule.waitUntil(SHEET_APPEAR_TIMEOUT) {
            composeTestRule.onAllNodesWithTag(TestTags.SIGN_DATA_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click reject button
        composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_REJECT_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    // ===========================================
    // Verification
    // ===========================================

    /**
     * Check if the connect request sheet is visible.
     */
    fun isConnectRequestVisible(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.CONNECT_REQUEST_SHEET)
            .assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Check if the transaction request sheet is visible.
     */
    fun isTransactionRequestVisible(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
            .assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Check if the sign data sheet is visible.
     */
    fun isSignDataRequestVisible(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_SHEET)
            .assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }
}
