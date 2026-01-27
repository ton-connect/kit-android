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
package io.ton.walletkit.demo.presentation.util

/**
 * Test tags for UI components - used by Espresso/Compose testing.
 *
 * These tags mirror the testIds used in the web demo-wallet for consistency.
 */
object TestTags {
    // SetupPasswordScreen
    const val PASSWORD_FIELD = "password"
    const val PASSWORD_CONFIRM_FIELD = "password-confirm"
    const val PASSWORD_SUBMIT_BUTTON = "password-submit"
    const val SETUP_PASSWORD_TITLE = "title"
    const val SETUP_PASSWORD_SUBTITLE = "subtitle"

    // UnlockWalletScreen
    const val UNLOCK_TITLE = "unlock-title"
    const val UNLOCK_PASSWORD_FIELD = "unlock-password"
    const val UNLOCK_SUBMIT_BUTTON = "unlock-submit"
    const val UNLOCK_RESET_BUTTON = "unlock-reset"

    // AddWalletSheet
    const val ADD_WALLET_TAB_IMPORT = "add-wallet-tab-import"
    const val ADD_WALLET_TAB_GENERATE = "add-wallet-tab-generate"
    const val IMPORT_WALLET_BUTTON = "import-wallet"
    const val GENERATE_WALLET_BUTTON = "generate-wallet"
    const val MNEMONIC_FIELD = "mnemonic"
    const val MNEMONIC_PROCESS_BUTTON = "mnemonic-process"
    const val IMPORT_WALLET_PROCESS_BUTTON = "import-wallet-process"
    const val GENERATE_WALLET_PROCESS_BUTTON = "generate-wallet-process"
    const val PASTE_ALL_BUTTON = "paste-all"

    // WalletScreen
    const val WALLET_TITLE = "wallet-title"
    const val WALLET_MENU_BUTTON = "wallet-menu"
    const val WALLET_BALANCE = "wallet-balance"
    const val ADD_WALLET_BUTTON = "add-wallet"
    const val REFRESH_BUTTON = "refresh"
    const val BROWSER_NO_INJECT_BUTTON = "browser-no-inject"

    // ConnectRequestSheet
    const val CONNECT_REQUEST_SHEET = "request"
    const val CONNECT_REQUEST_TITLE = "connect-request-title"
    const val CONNECT_APPROVE_BUTTON = "connect-approve"
    const val CONNECT_REJECT_BUTTON = "connect-reject"

    // TransactionRequestSheet
    const val TRANSACTION_REQUEST_SHEET = "transaction-request"
    const val TRANSACTION_REQUEST_TITLE = "transaction-request-title"
    const val SEND_TRANSACTION_APPROVE_BUTTON = "send-transaction-approve"
    const val SEND_TRANSACTION_REJECT_BUTTON = "send-transaction-reject"

    // SignDataSheet
    const val SIGN_DATA_REQUEST_SHEET = "sign-data-request"
    const val SIGN_DATA_REQUEST_TITLE = "sign-data-request-title"
    const val SIGN_DATA_APPROVE_BUTTON = "sign-data-approve"
    const val SIGN_DATA_REJECT_BUTTON = "sign-data-reject"

    // SignMessageSheet (gasless transactions)
    const val SIGN_MESSAGE_REQUEST_SHEET = "sign-message-request"
    const val SIGN_MESSAGE_APPROVE_BUTTON = "sign-message-approve"
    const val SIGN_MESSAGE_REJECT_BUTTON = "sign-message-reject"

    // TonConnect URL input (for manual connection testing)
    const val HANDLE_URL_BUTTON = "handle-url"
    const val TONCONNECT_URL_FIELD = "tonconnect-url"
    const val TONCONNECT_PROCESS_BUTTON = "tonconnect-process"

    // Settings
    const val AUTO_LOCK_TOGGLE = "auto-lock"
    const val HOLD_TO_SIGN_TOGGLE = "hold-to-sign"
    const val NETWORK_SELECT = "network-select"

    // Additional wallet screen elements
    const val WALLET_ADDRESS = "wallet-address"
    const val WALLET_COPY_ADDRESS_BUTTON = "copy-address"
    const val WALLET_RECEIVE_BUTTON = "receive"
    const val WALLET_SEND_BUTTON = "send"

    // Mnemonic paste field (for clipboard import)
    const val MNEMONIC_PASTE_FIELD = "mnemonic-paste-field"

    // Sheet action buttons (aliases for consistency)
    const val SIGN_DATA_SHEET = SIGN_DATA_REQUEST_SHEET
    const val TRANSACTION_APPROVE_BUTTON = SEND_TRANSACTION_APPROVE_BUTTON
    const val TRANSACTION_REJECT_BUTTON = SEND_TRANSACTION_REJECT_BUTTON
}
