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
package io.ton.walletkit.demo.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.api.generated.TONNetwork
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.domain.model.WalletInterfaceType
import io.ton.walletkit.demo.presentation.actions.WalletActions
import io.ton.walletkit.demo.presentation.model.ConnectRequestUi
import io.ton.walletkit.demo.presentation.model.JettonDetails
import io.ton.walletkit.demo.presentation.model.JettonSummary
import io.ton.walletkit.demo.presentation.model.NFTDetails
import io.ton.walletkit.demo.presentation.model.SignDataRequestUi
import io.ton.walletkit.demo.presentation.model.TransactionRequestUi
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.state.SheetState
import io.ton.walletkit.demo.presentation.state.WalletUiState
import io.ton.walletkit.demo.presentation.ui.components.QuickActionsCard
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeAssetIcon
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeAssetItem
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeContent
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeHeader
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeNFTPreview
import io.ton.walletkit.demo.presentation.ui.dialog.SignerConfirmationDialog
import io.ton.walletkit.demo.presentation.ui.dialog.UrlPromptDialog
import io.ton.walletkit.demo.presentation.ui.sheet.AddWalletSheet
import io.ton.walletkit.demo.presentation.ui.sheet.BrowserSession
import io.ton.walletkit.demo.presentation.ui.sheet.BrowserSheet
import io.ton.walletkit.demo.presentation.ui.sheet.ConnectRequestSheet
import io.ton.walletkit.demo.presentation.ui.sheet.JettonDetailsSheet
import io.ton.walletkit.demo.presentation.ui.sheet.SignDataSheet
import io.ton.walletkit.demo.presentation.ui.sheet.StakingSheet
import io.ton.walletkit.demo.presentation.ui.sheet.SwapSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransactionDetailSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransactionRequestSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransferJettonSheet
import io.ton.walletkit.demo.presentation.ui.sheet.WalletDetailsSheet
import io.ton.walletkit.demo.presentation.ui.sheet.WalletsBottomSheet
import io.ton.walletkit.demo.presentation.viewmodel.NFTsListViewModel
import io.ton.walletkit.demo.presentation.viewmodel.SwapViewModel

// URL for the TonConnect E2E test runner dApp
// This is the same dApp used by web demo-wallet E2E tests
private const val DEFAULT_DAPP_URL = "https://allure-test-runner.vercel.app/e2e"

private const val MAX_ASSETS = 3
private const val MAX_NFTS = 5
private const val MAX_FRACTION_DIGITS = 5

private object WalletHomeTopBar {
    fun shortAddress(address: String): String {
        if (address.length <= 10) return address
        return "${address.take(4)}...${address.takeLast(6)}"
    }
}

private fun trimFraction(value: String?, maxFractionDigits: Int): String {
    val raw = value.orEmpty().ifBlank { "0" }
    val dotIndex = raw.indexOf('.')
    if (dotIndex < 0) return raw
    val fractionStart = dotIndex + 1
    val fraction = raw.substring(fractionStart)
    if (fraction.length <= maxFractionDigits) return raw
    var truncated = raw.substring(0, fractionStart + maxFractionDigits)
    while (truncated.endsWith('0')) truncated = truncated.dropLast(1)
    if (truncated.endsWith('.')) truncated = truncated.dropLast(1)
    return truncated
}

private fun splitBalance(rawBalance: String?, maxFractionDigits: Int): Pair<String, String> {
    val trimmed = trimFraction(rawBalance, maxFractionDigits)
    val dotIndex = trimmed.indexOf('.')
    return if (dotIndex < 0) {
        trimmed to ""
    } else {
        trimmed.substring(0, dotIndex) to trimmed.substring(dotIndex)
    }
}

private fun buildAssetList(
    rawBalance: String?,
    jettons: List<JettonSummary>,
    maxFractionDigits: Int,
    maxAssets: Int,
): List<WalletHomeAssetItem> {
    val tonAmount = trimFraction(rawBalance, maxFractionDigits)
    val tonItem = WalletHomeAssetItem(
        id = "ton",
        name = "Toncoin",
        symbol = "TON",
        formattedAmount = "$tonAmount TON",
        icon = WalletHomeAssetIcon.Ton,
    )
    val items = mutableListOf(tonItem)
    jettons.take(maxAssets - 1).forEach { jetton ->
        val amount = trimFraction(jetton.balance, maxFractionDigits)
        val icon = jetton.imageUrl?.takeIf { it.isNotBlank() }
            ?.let { WalletHomeAssetIcon.Url(it) }
            ?: WalletHomeAssetIcon.Placeholder(jetton.symbol)
        items += WalletHomeAssetItem(
            id = jetton.address,
            name = jetton.name,
            symbol = jetton.symbol,
            formattedAmount = "$amount ${jetton.symbol}",
            icon = icon,
        )
    }
    return items
}

private sealed interface HomeSubScreen {
    data object None : HomeSubScreen
    data object AllAssets : HomeSubScreen
    data object AllNFTs : HomeSubScreen
}

private fun nftPreviewFrom(nft: TONNFT): WalletHomeNFTPreview {
    val info = nft.info
    return WalletHomeNFTPreview(
        id = nft.address.value,
        name = info?.name ?: "Unknown NFT",
        address = nft.address.value,
        imageUrl = info?.image?.mediumUrl ?: info?.image?.url,
    )
}

private fun networkLabelFor(network: TONNetwork?): String = when (network?.chainId) {
    io.ton.walletkit.api.ChainIds.MAINNET -> "MainNet"
    io.ton.walletkit.api.ChainIds.TESTNET -> "TestNet"
    io.ton.walletkit.api.ChainIds.TETRA -> "Tetra"
    null -> "MainNet"
    else -> "TON"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    state: WalletUiState,
    walletKit: ITONWalletKit,
    nftsViewModel: NFTsListViewModel?,
    swapViewModel: SwapViewModel?,
    actions: WalletActions,
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val onRefreshAll: () -> Unit = {
        actions.onRefresh()
        actions.onRefreshJettons()
        nftsViewModel?.refresh()
    }

    // State for NFT details bottom sheet
    var selectedNFT by remember { mutableStateOf<TONNFT?>(null) }
    val nftDetailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Two independent browser sessions — one with WalletKit injection, one without.
    // Kept at this level so tabs survive Connect/Transaction overlay sheets.
    val injectedSession = remember { BrowserSession(injectTonConnect = true) }
    val plainSession = remember { BrowserSession(injectTonConnect = false) }

    DisposableEffect(Unit) {
        onDispose {
            injectedSession.destroyAllWebViews()
            plainSession.destroyAllWebViews()
        }
    }

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheet = state.sheetState
    val showSheet = sheet !is SheetState.None
    LaunchedEffect(state.sheetState) {
        if (state.sheetState is SheetState.None && sheetState.isVisible) {
            sheetState.hide()
        }
        // Open initial tab when browser sheet is first shown for each session type
        val browserSheet = state.sheetState as? SheetState.Browser ?: return@LaunchedEffect
        val session = if (browserSheet.injectTonConnect) injectedSession else plainSession
        if (session.tabs.isEmpty()) session.openTab(browserSheet.url)
    }
    val activeWallet = state.wallets.firstOrNull { it.address == state.activeWalletAddress }
        ?: state.wallets.firstOrNull()
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = actions::onDismissSheet,
            sheetState = sheetState,
            dragHandle = null,
        ) {
            when (sheet) {
                SheetState.AddWallet -> AddWalletSheet(
                    onDismiss = actions::onDismissSheet,
                    onImportWallet = actions::onImportWallet,
                    onGenerateWallet = actions::onGenerateWallet,
                    walletCount = state.wallets.size,
                )

                is SheetState.Connect -> ConnectRequestSheet(
                    request = sheet.request,
                    wallets = state.wallets,
                    onApprove = actions::onApproveConnect,
                    onReject = actions::onRejectConnect,
                )

                is SheetState.Transaction -> TransactionRequestSheet(
                    request = sheet.request,
                    onApprove = { actions.onApproveTransaction(sheet.request) },
                    onReject = { actions.onRejectTransaction(sheet.request) },
                )

                is SheetState.SignData -> SignDataSheet(
                    request = sheet.request,
                    onApprove = { actions.onApproveSignData(sheet.request) },
                    onReject = { actions.onRejectSignData(sheet.request) },
                )

                is SheetState.WalletDetails -> WalletDetailsSheet(
                    wallet = sheet.wallet,
                    onDismiss = actions::onDismissSheet,
                )

                is SheetState.SendTransaction -> SendTransactionScreen(
                    wallet = sheet.wallet,
                    onBack = actions::onDismissSheet,
                    onSend = { recipient, amount, comment ->
                        actions.onSendTransaction(sheet.wallet.address, recipient, amount, comment)
                    },
                    error = state.error,
                    isLoading = state.isSendingTransaction,
                )

                is SheetState.Staking -> StakingSheet(
                    wallet = sheet.wallet,
                    walletKit = walletKit,
                    sheetKey = sheet.openedAt,
                    onDismiss = actions::onDismissSheet,
                )

                is SheetState.TransactionDetail -> TransactionDetailSheet(
                    transaction = sheet.transaction,
                    onDismiss = actions::onDismissSheet,
                )

                is SheetState.Browser -> BrowserSheet(
                    session = if (sheet.injectTonConnect) injectedSession else plainSession,
                    onClose = actions::onDismissSheet,
                    walletKit = walletKit,
                )

                is SheetState.JettonDetails -> {
                    JettonDetailsSheet(
                        jetton = sheet.jetton,
                        onDismiss = actions::onDismissSheet,
                        onTransfer = { actions.onShowTransferJetton(sheet.jetton) },
                    )
                }

                is SheetState.TransferJetton -> {
                    TransferJettonSheet(
                        jetton = sheet.jetton,
                        onDismiss = actions::onDismissSheet,
                        onTransfer = { recipient, amount, comment ->
                            actions.onTransferJetton(sheet.jetton.jettonAddress ?: "", recipient, amount, comment)
                        },
                        isLoading = false,
                    )
                }

                is SheetState.Swap -> {
                    swapViewModel?.let { vm ->
                        SwapSheet(viewModel = vm, onDismiss = actions::onDismissSheet)
                    }
                }

                SheetState.None -> Unit
            }
        }
    }

    if (state.isUrlPromptVisible) {
        UrlPromptDialog(
            onDismiss = actions::onDismissUrlPrompt,
            onConfirm = actions::onHandleUrl,
        )
    }

    // Show confirmation dialog for Signer wallets
    state.pendingSignerConfirmation?.let { request ->
        SignerConfirmationDialog(
            request = request,
            onConfirm = actions::onConfirmSignerApproval,
            onCancel = actions::onCancelSignerApproval,
        )
    }

    val activeIndex = state.wallets.indexOfFirst { it.address == state.activeWalletAddress }
    val homeTitle = if (activeIndex >= 0) "Wallet ${activeIndex + 1}" else "Wallet"
    val homeAddress = activeWallet?.address.orEmpty()
    val homeNetworkLabel = networkLabelFor(activeWallet?.network)
    val nftsList by (nftsViewModel?.nfts?.collectAsState() ?: remember { mutableStateOf(emptyList<TONNFT>()) })
    val (totalInteger, totalFraction) = remember(activeWallet?.balance) {
        splitBalance(activeWallet?.balance, MAX_FRACTION_DIGITS)
    }
    val assetItems = remember(activeWallet?.balance, state.jettons) {
        buildAssetList(activeWallet?.balance, state.jettons, MAX_FRACTION_DIGITS, MAX_ASSETS)
    }
    val hasMoreAssets = state.jettons.size > (MAX_ASSETS - 1)
    val nftPreviews = remember(nftsList) {
        nftsList.take(MAX_NFTS).map(::nftPreviewFrom)
    }
    val hasMoreNFTs = nftsList.size > MAX_NFTS

    var showWalletsSheet by remember { mutableStateOf(false) }
    var subScreen by remember { mutableStateOf<HomeSubScreen>(HomeSubScreen.None) }
    val walletsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = androidx.compose.ui.platform.LocalContext.current

    // Drop the wallet picker sheet automatically when we navigate to a sub-screen,
    // and reset the sub-screen when the active wallet changes (e.g. user switched
    // wallets while looking at all-assets — bounce them back to home for the new
    // context, mirroring the iOS NavigationStack popping on `.id(active.id)`).
    LaunchedEffect(state.activeWalletAddress) {
        subScreen = HomeSubScreen.None
    }
    if (subScreen != HomeSubScreen.None) {
        when (val current = subScreen) {
            HomeSubScreen.AllAssets -> {
                AllAssetsScreen(
                    assets = remember(activeWallet?.balance, state.jettons) {
                        buildAssetList(
                            rawBalance = activeWallet?.balance,
                            jettons = state.jettons,
                            maxFractionDigits = MAX_FRACTION_DIGITS,
                            // No cap — show everything on the all-assets screen.
                            maxAssets = Int.MAX_VALUE,
                        )
                    },
                    onBack = { subScreen = HomeSubScreen.None },
                )
            }
            HomeSubScreen.AllNFTs -> {
                AllNFTsScreen(
                    nfts = remember(nftsList) { nftsList.map(::nftPreviewFrom) },
                    onTap = { preview ->
                        nftsList.firstOrNull { it.address.value == preview.address }
                            ?.let { selectedNFT = it }
                    },
                    onBack = { subScreen = HomeSubScreen.None },
                )
            }
            HomeSubScreen.None -> Unit
            else -> Unit
        }
        // Drain the rest of the composable to render only the sub-screen + its modals.
        // Falling through would also render the home Scaffold underneath.
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (activeWallet != null) {
                        WalletHomeHeader(
                            title = homeTitle,
                            networkLabel = homeNetworkLabel,
                            truncatedAddress = WalletHomeTopBar.shortAddress(homeAddress),
                            onClick = { showWalletsSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.wallet_screen_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showWalletsSheet = true }) {
                        TonIconImage(
                            icon = TonIcon.Settings24,
                            size = 24.dp,
                            tint = TonTheme.colors.textPrimary,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Always reserve space for progress indicator to prevent content shift.
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                if (state.isLoadingWallets || state.isLoadingSessions) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            WalletHomeContent(
                totalBalanceInteger = totalInteger,
                totalBalanceFraction = totalFraction,
                assets = assetItems,
                nfts = nftPreviews,
                hasMoreAssets = hasMoreAssets,
                hasMoreNFTs = hasMoreNFTs,
                onDeposit = { activeWallet?.let { actions.onWalletDetails(it.address) } },
                onSend = { activeWallet?.let { actions.onSendFromWallet(it.address) } },
                onReceive = { activeWallet?.let { actions.onWalletDetails(it.address) } },
                onShowAllAssets = { subScreen = HomeSubScreen.AllAssets },
                onShowAllNFTs = { subScreen = HomeSubScreen.AllNFTs },
                onNFTTap = { preview ->
                    nftsList.firstOrNull { it.address.value == preview.address }?.let { selectedNFT = it }
                },
            )
        }
    }

    if (showWalletsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWalletsSheet = false },
            sheetState = walletsSheetState,
            dragHandle = null,
        ) {
            WalletsBottomSheet(
                wallets = state.wallets,
                activeWalletAddress = state.activeWalletAddress,
                onSelect = { wallet ->
                    actions.onSwitchWallet(wallet.address)
                    showWalletsSheet = false
                },
                onCopyAddress = { address ->
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(
                        android.content.ClipData.newPlainText("Wallet address", address),
                    )
                },
                onAddWallet = {
                    showWalletsSheet = false
                    actions.onAddWalletClick()
                },
                onDelete = { wallet ->
                    actions.onRemoveWallet(wallet.address)
                },
                onClose = { showWalletsSheet = false },
            )
        }
    }

    // NFT Details Modal Bottom Sheet (separate from main sheet state)
    selectedNFT?.let { nft ->
        ModalBottomSheet(
            onDismissRequest = { selectedNFT = null },
            sheetState = nftDetailsSheetState,
            dragHandle = null,
        ) {
            // Get the wallet to pass to NFTDetailsScreen
            activeWallet?.let { wallet ->
                val nftDetails = NFTDetails.from(nft)
                NFTDetailsScreen(
                    walletAddress = wallet.address,
                    walletKit = walletKit,
                    nftDetails = nftDetails,
                    onClose = { selectedNFT = null },
                    onTransferSuccess = {
                        val nftAddress = selectedNFT?.address?.value
                        nftAddress?.let { addr -> nftsViewModel?.removeNft(addr) }
                        nftsViewModel?.refreshWithDelay()
                    },
                )
            }
        }
    }
}

// Preview temporarily disabled - requires ITONWalletKit instance
// @Preview(showBackground = true)
// @Composable
// private fun WalletScreenPreview() {
//     WalletScreen(
//         state = PreviewData.uiState,
//         walletKit = ..., // TODO: Mock instance
//         onAddWalletClick = {},
//         ...
//     )
// }
