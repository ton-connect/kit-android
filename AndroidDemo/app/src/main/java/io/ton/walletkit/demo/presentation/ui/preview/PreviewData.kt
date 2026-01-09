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
package io.ton.walletkit.demo.presentation.ui.preview

import io.ton.walletkit.api.MAINNET
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNFTCollection
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.api.generated.TONTokenImage
import io.ton.walletkit.api.generated.TONTokenInfo
import io.ton.walletkit.demo.presentation.model.ConnectPermissionUi
import io.ton.walletkit.demo.presentation.model.ConnectRequestUi
import io.ton.walletkit.demo.presentation.model.SessionSummary
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.model.TransactionDetailUi
import io.ton.walletkit.demo.presentation.model.TransactionMessageUi
import io.ton.walletkit.demo.presentation.model.TransactionRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.state.SheetState
import io.ton.walletkit.demo.presentation.state.WalletUiState
import io.ton.walletkit.model.TONUserFriendlyAddress
import org.json.JSONObject

object PreviewData {
    val wallet: WalletSummary = WalletSummary(
        address = "EQpreviewaddressExampleToShowWalletKit123",
        name = "Preview Wallet",
        network = TONNetwork.MAINNET,
        version = "v5r1",
        publicKey = "preview-public-key",
        balanceNano = "123456789",
        balance = "0.1234",
        transactions = null,
        lastUpdated = System.currentTimeMillis(),
    )

    val session: SessionSummary = SessionSummary(
        sessionId = "session-preview",
        dAppName = "Preview dApp",
        walletAddress = wallet.address,
        dAppUrl = "https://preview.app",
        manifestUrl = "https://preview.app/manifest",
        iconUrl = null,
        createdAt = System.currentTimeMillis(),
        lastActivity = System.currentTimeMillis(),
    )

    val connectRequest: ConnectRequestUi = ConnectRequestUi(
        id = "request-preview",
        dAppName = session.dAppName,
        dAppUrl = session.dAppUrl.orEmpty(),
        manifestUrl = session.manifestUrl.orEmpty(),
        iconUrl = null,
        permissions = listOf(
            ConnectPermissionUi(name = "ton_addr", title = "Wallet address", description = "Access to your wallet address"),
        ),
        requestedItems = listOf("ton_proof"),
        raw = JSONObject(mapOf("id" to "request-preview")),
    )

    val transactionRequest: TransactionRequestUi = TransactionRequestUi(
        id = "tx-preview",
        walletAddress = wallet.address,
        dAppName = session.dAppName,
        validUntil = null,
        messages = listOf(
            TransactionMessageUi(
                to = "EQrecipientAddressForPreviewTest12345678",
                amount = "1000000000",
                comment = null,
                payload = "payload",
                stateInit = null,
            ),
        ),
        preview = "{\n  \"action\": \"transfer\"\n}",
        raw = JSONObject(mapOf("id" to "tx-preview")),
    )

    val signDataRequest: SignDataRequestUi = SignDataRequestUi(
        id = "sign-preview",
        walletAddress = wallet.address,
        dAppName = "Preview dApp",
        payloadType = "ton_proof",
        payloadContent = "{\n  \"domain\": \"preview\"\n}",
        preview = null,
        raw = JSONObject(mapOf("id" to "sign-preview")),
    )

    val transactionDetail: TransactionDetailUi = TransactionDetailUi(
        hash = "abc123def456hash789preview",
        timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
        isOutgoing = true,
        amount = "1.5 TON",
        fee = "0.005 TON",
        fromAddress = wallet.address,
        toAddress = "EQrecipientAddressForPreviewTest12345678",
        comment = "Payment for services",
        status = "Confirmed",
        blockSeqno = 12345678,
        lt = "9876543210",
    )

    val uiState: WalletUiState = WalletUiState(
        initialized = true,
        status = "WalletKit ready",
        wallets = listOf(wallet),
        sessions = listOf(session),
        sheetState = SheetState.None,
        isUrlPromptVisible = false,
        isLoadingWallets = false,
        isLoadingSessions = false,
        error = null,
        events = listOf("Handled TON Connect URL", "Approved transaction"),
        lastUpdated = System.currentTimeMillis(),
    )

    val nftCollection: TONNFTCollection = TONNFTCollection(
        address = TONUserFriendlyAddress("EQcollectionAddressPreview123456789012"),
        name = "Preview Collection",
        nextItemIndex = "100",
        ownerAddress = TONUserFriendlyAddress(wallet.address),
    )

    val nftItem: TONNFT = TONNFT(
        address = TONUserFriendlyAddress("EQnftItemAddressPreview1234567890123456"),
        index = "1",
        ownerAddress = TONUserFriendlyAddress(wallet.address),
        collection = nftCollection,
        info = TONTokenInfo(
            name = "Cool NFT #1",
            description = "A preview NFT item",
            image = TONTokenImage(
                url = "https://picsum.photos/seed/nft1/400/400",
            ),
        ),
    )

    val nftItem2: TONNFT = TONNFT(
        address = TONUserFriendlyAddress("EQnftItemAddressPreview2345678901234567"),
        index = "2",
        ownerAddress = TONUserFriendlyAddress(wallet.address),
        collection = nftCollection,
        info = TONTokenInfo(
            name = "Cool NFT #2",
            description = "Another preview NFT item",
            image = TONTokenImage(
                url = "https://picsum.photos/seed/nft2/400/400",
            ),
        ),
    )

    val nftItems = listOf(nftItem, nftItem2)
}
