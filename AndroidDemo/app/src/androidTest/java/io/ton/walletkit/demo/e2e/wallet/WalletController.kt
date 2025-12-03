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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import io.qameta.allure.kotlin.Step
import io.ton.walletkit.demo.presentation.MainActivity
import io.ton.walletkit.demo.presentation.util.TestTags
import io.ton.walletkit.demo.presentation.viewmodel.WalletKitViewModel

/**
 * Controller for interacting with the wallet demo app via Espresso/Compose testing.
 *
 * This mirrors the wallet controller concept from the web demo-wallet.
 */
class WalletController(composeTestRule: ComposeTestRule? = null) {

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
    // Screen Detection
    // ===========================================

    /**
     * Check if we're on the SetupPasswordScreen (first time setup).
     */
    fun isOnSetupPasswordScreen(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_FIELD)
            .assertExists()
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD)
            .assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Check if we're on the UnlockWalletScreen (password already set).
     */
    fun isOnUnlockScreen(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.UNLOCK_PASSWORD_FIELD)
            .assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    /**
     * Check if we're on the AddWalletSheet (wallet setup needed).
     */
    fun isOnAddWalletSheet(): Boolean = try {
        composeTestRule.onNodeWithTag(TestTags.MNEMONIC_FIELD)
            .assertExists()
        true
    } catch (e: AssertionError) {
        false
    }

    // ===========================================
    // Wallet Setup Actions
    // ===========================================

    /**
     * Set up password for the wallet (first time user).
     */
    @Step("Set up password")
    fun setupPassword(password: String = "testpass123") {
        // Wait for setup password screen
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.PASSWORD_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Enter password
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_FIELD)
            .performTextInput(password)

        // Enter confirmation
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD)
            .performTextInput(password)

        // Submit
        composeTestRule.onNodeWithTag(TestTags.PASSWORD_SUBMIT_BUTTON)
            .performClick()

        // Wait for transition
        composeTestRule.waitForIdle()
    }

    /**
     * Unlock the wallet with existing password.
     */
    @Step("Unlock wallet")
    fun unlockWallet(password: String = "testpass123") {
        // Wait for unlock screen
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.UNLOCK_PASSWORD_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Enter password
        composeTestRule.onNodeWithTag(TestTags.UNLOCK_PASSWORD_FIELD)
            .performTextInput(password)

        // Submit
        composeTestRule.onNodeWithTag(TestTags.UNLOCK_SUBMIT_BUTTON)
            .performClick()

        // Wait for transition to complete
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Give extra time for app to finish state transition
        android.util.Log.d("WalletController", "Unlock completed, waiting for app to stabilize...")
    }

    /**
     * Reset the wallet (clears all data).
     * Use this when password is unknown or to start fresh.
     */
    @Step("Reset wallet")
    fun resetWallet() {
        // Wait for unlock screen with reset button
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.UNLOCK_RESET_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click reset button
        composeTestRule.onNodeWithTag(TestTags.UNLOCK_RESET_BUTTON)
            .performClick()

        // Confirm in dialog (look for "Reset" text button)
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithText("Reset")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click confirm button in dialog
        composeTestRule.onNodeWithText("Reset")
            .performClick()

        // Wait for transition to setup screen
        composeTestRule.waitForIdle()
    }

    /**
     * Ensure we're in a clean state and ready to set up.
     * If on unlock screen, resets the wallet first.
     */
    @Step("Ensure clean state")
    fun ensureCleanState(password: String = "testpass123") {
        composeTestRule.waitForIdle()

        // Give the UI time to settle
        Thread.sleep(500)

        // Check which screen we're on
        val onUnlock = try {
            composeTestRule.onNodeWithTag(TestTags.UNLOCK_PASSWORD_FIELD)
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (onUnlock) {
            // We're on unlock screen - reset to get clean state
            resetWallet()
        }

        // Now we should be on setup password screen
    }

    /**
     * Check if we're on the home screen with a wallet ready (not showing AddWalletSheet).
     * Returns true only if the browser button is visible AND the AddWalletSheet is NOT showing.
     */
    fun isOnHomeScreen(): Boolean {
        // First check if browser button exists (we're on WalletScreen)
        val browserButtonExists = try {
            composeTestRule.onNodeWithTag(TestTags.BROWSER_NO_INJECT_BUTTON)
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (!browserButtonExists) return false

        // Now check if AddWalletSheet is showing (means we need to generate/import wallet)
        val addWalletSheetShowing = try {
            composeTestRule.onNodeWithTag(TestTags.ADD_WALLET_TAB_GENERATE)
                .assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        // We're on home screen only if browser button exists AND AddWalletSheet is NOT showing
        return !addWalletSheetShowing
    }

    /**
     * Handle the authentication flow - either setup, unlock, or skip if already on home.
     * Automatically detects which screen we're on.
     */
    @Step("Authenticate")
    fun authenticate(password: String = "testpass123") {
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Give app time to settle after activity start

        // Wait for one of the expected screens to appear (up to 15 seconds)
        // This handles the case when the app is starting up
        var screenDetected = false
        var attempts = 0
        val maxAttempts = 30 // 30 * 500ms = 15 seconds

        while (!screenDetected && attempts < maxAttempts) {
            composeTestRule.waitForIdle()

            // Check if already on home screen (no auth needed)
            if (isOnHomeScreen()) {
                android.util.Log.d("WalletController", "Detected home screen - no authentication needed")
                return
            }

            // Check if on unlock screen FIRST (more likely if wallet exists from previous test)
            val onUnlock = try {
                composeTestRule.onNodeWithTag(TestTags.UNLOCK_PASSWORD_FIELD)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (onUnlock) {
                android.util.Log.d("WalletController", "Detected unlock screen")
                unlockWalletSafe(password)
                screenDetected = true
                return
            }

            // Check if on setup password screen (has confirm field)
            val onSetup = try {
                composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD)
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (onSetup) {
                android.util.Log.d("WalletController", "Detected setup password screen")
                setupPasswordSafe(password)
                screenDetected = true
                return
            }

            // None detected yet, wait and try again
            android.util.Log.d("WalletController", "No auth screen detected yet (attempt ${attempts + 1}/$maxAttempts)")
            attempts++
            Thread.sleep(500)
        }

        // If we get here, check one more time for home screen (maybe transitions happened)
        if (isOnHomeScreen()) {
            android.util.Log.d("WalletController", "Detected home screen after waiting")
            return
        }

        // Last resort - try unlock as fallback
        android.util.Log.w("WalletController", "No auth screen detected after $maxAttempts attempts, trying unlock")
        unlockWalletSafe(password)
    }

    /**
     * Safe version of setupPassword that doesn't throw on timeout.
     */
    private fun setupPasswordSafe(password: String) {
        try {
            // Enter password (field should already exist from detection)
            composeTestRule.onNodeWithTag(TestTags.PASSWORD_FIELD)
                .performTextInput(password)

            // Enter confirmation
            composeTestRule.onNodeWithTag(TestTags.PASSWORD_CONFIRM_FIELD)
                .performTextInput(password)

            // Submit
            composeTestRule.onNodeWithTag(TestTags.PASSWORD_SUBMIT_BUTTON)
                .performClick()

            // Wait for transition
            composeTestRule.waitForIdle()
            Thread.sleep(500)
        } catch (e: Exception) {
            android.util.Log.w("WalletController", "setupPasswordSafe failed: ${e.message}")
        }
    }

    /**
     * Safe version of unlockWallet that doesn't throw on timeout.
     */
    private fun unlockWalletSafe(password: String) {
        try {
            // Enter password (field should already exist from detection)
            composeTestRule.onNodeWithTag(TestTags.UNLOCK_PASSWORD_FIELD)
                .performTextInput(password)

            // Submit
            composeTestRule.onNodeWithTag(TestTags.UNLOCK_SUBMIT_BUTTON)
                .performClick()

            // Wait for transition to complete
            composeTestRule.waitForIdle()
            Thread.sleep(1000) // Give extra time for app to finish state transition
            android.util.Log.d("WalletController", "Unlock completed, waiting for app to stabilize...")
        } catch (e: Exception) {
            android.util.Log.w("WalletController", "unlockWalletSafe failed: ${e.message}")
        }
    }

    /**
     * Import a wallet using mnemonic.
     */
    @Step("Import wallet with mnemonic")
    fun importWallet(mnemonic: List<String>) {
        val mnemonicString = mnemonic.joinToString(" ")

        // Wait for mnemonic field (the paste field in AddWalletSheet)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.MNEMONIC_FIELD)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Enter mnemonic
        composeTestRule.onNodeWithTag(TestTags.MNEMONIC_FIELD)
            .performTextInput(mnemonicString)

        // Wait a bit for auto-parsing
        Thread.sleep(500)

        // Click import button
        composeTestRule.onNodeWithTag(TestTags.IMPORT_WALLET_PROCESS_BUTTON)
            .performClick()

        // Wait for wallet to be imported
        composeTestRule.waitForIdle()
    }

    /**
     * Generate a new wallet (simplest way to create a wallet for testing).
     * This switches to the Generate tab and clicks the generate button.
     */
    @Step("Generate new wallet")
    fun generateWallet() {
        // Wait for the AddWalletSheet to be visible (look for the Generate tab)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag(TestTags.ADD_WALLET_TAB_GENERATE)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click the Generate tab
        composeTestRule.onNodeWithTag(TestTags.ADD_WALLET_TAB_GENERATE)
            .performClick()

        composeTestRule.waitForIdle()

        // Wait for the generate button to appear
        composeTestRule.waitUntil(3000) {
            composeTestRule.onAllNodesWithTag(TestTags.GENERATE_WALLET_PROCESS_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click the generate button
        composeTestRule.onNodeWithTag(TestTags.GENERATE_WALLET_PROCESS_BUTTON)
            .performClick()

        // Wait for wallet to be generated
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Give time for wallet generation
    }

    /**
     * Complete the full wallet setup flow using generated wallet.
     * This is the simplest way - just authenticate and generate a wallet.
     * If already on home screen with a wallet, skips setup.
     */
    @Step("Setup wallet (generate)")
    fun setupWalletGenerate(password: String = "testpass123") {
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // Check if we're already on home screen with a wallet (no AddWalletSheet showing)
        if (isOnHomeScreen()) {
            android.util.Log.d("WalletController", "Already on home screen with wallet - setup not needed")
            return
        }

        // Check if AddWalletSheet is already showing (password already set, just need to generate)
        if (isAddWalletSheetShowing()) {
            android.util.Log.d("WalletController", "AddWalletSheet already showing - generating wallet directly")
            generateWallet()
            return
        }

        // First authenticate (handles both setup and unlock)
        authenticate(password)

        // After authentication, wait for UI to stabilize
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // After authentication, check again if we're on home screen with wallet
        for (i in 1..5) {
            if (isOnHomeScreen()) {
                android.util.Log.d("WalletController", "On home screen after auth (check $i) - wallet exists")
                return
            }

            // Check if AddWalletSheet is showing (need to generate wallet)
            if (isAddWalletSheetShowing()) {
                android.util.Log.d("WalletController", "AddWalletSheet showing after auth (check $i) - generating wallet")
                generateWallet()
                return
            }

            android.util.Log.d("WalletController", "Waiting for home/addWallet screen (check $i)...")
            Thread.sleep(500)
        }

        // Last resort: try to generate wallet anyway
        android.util.Log.d("WalletController", "Proceeding to generate wallet (fallback)")
        generateWallet()
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
     */
    @Step("Complete wallet setup")
    fun setupWallet(mnemonic: List<String>, password: String = "testpass123") {
        // First authenticate (handles both setup and unlock)
        authenticate(password)

        // Then import wallet
        importWallet(mnemonic)
    }

    /**
     * Complete the full wallet setup flow, starting from a clean state.
     * If on unlock screen, resets first, then sets up password and imports wallet.
     */
    @Step("Setup wallet from clean state")
    fun setupWalletClean(mnemonic: List<String>, password: String = "testpass123") {
        // Ensure clean state (reset if on unlock screen)
        ensureCleanState(password)

        // Setup password
        setupPassword(password)

        // Import wallet
        importWallet(mnemonic)
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
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag(TestTags.BROWSER_NO_INJECT_BUTTON)
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
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag(TestTags.CONNECT_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Small delay to let the sheet animation complete
        Thread.sleep(500)

        // Click approve button (no scroll needed as button should be visible)
        composeTestRule.onNodeWithTag(TestTags.CONNECT_APPROVE_BUTTON)
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Reject a TonConnect connection request.
     */
    @Step("Reject connection request")
    fun rejectConnect() {
        // Wait for connect request sheet
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag(TestTags.CONNECT_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

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
        // Wait for transaction request sheet
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag(TestTags.TRANSACTION_REQUEST_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click approve button
        composeTestRule.onNodeWithTag(TestTags.TRANSACTION_APPROVE_BUTTON)
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Reject a transaction request.
     */
    @Step("Reject transaction request")
    fun rejectTransaction() {
        // Wait for transaction request sheet
        composeTestRule.waitUntil(10000) {
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
        composeTestRule.waitUntil(10000) {
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
        composeTestRule.waitUntil(10000) {
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

    /**
     * Verify that sign data sheet is visible with details.
     */
    @Step("Verify sign data sheet is visible")
    fun verifySignDataSheetVisible() {
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag(TestTags.SIGN_DATA_SHEET)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(TestTags.SIGN_DATA_SHEET)
            .assertIsDisplayed()
    }

    /**
     * Wait for wallet screen to be visible.
     * Checks for Handle URL button presence as indicator.
     */
    @Step("Wait for wallet screen")
    fun waitForWalletScreen() {
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithTag(TestTags.HANDLE_URL_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Wait for the wallet home screen to be displayed.
     */
    @Step("Wait for wallet home screen")
    fun waitForWalletHome(timeoutMs: Long = 10000) {
        // Wait for any wallet UI element that indicates we're on the home screen
        // This could be balance display, account name, etc.
        composeTestRule.waitUntil(timeoutMs) {
            // Check for any main wallet screen element
            composeTestRule.onAllNodesWithTag(TestTags.WALLET_BALANCE)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag(TestTags.WALLET_ADDRESS)
                    .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
