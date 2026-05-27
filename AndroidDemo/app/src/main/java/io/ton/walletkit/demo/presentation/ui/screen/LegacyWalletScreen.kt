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

import android.widget.Toast
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.generated.TONNFT
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.actions.WalletActions
import io.ton.walletkit.demo.presentation.dev.DevPreferences
import io.ton.walletkit.demo.presentation.dev.devToggleTaps
import io.ton.walletkit.demo.presentation.model.NFTDetails
import io.ton.walletkit.demo.presentation.state.SheetState
import io.ton.walletkit.demo.presentation.state.WalletUiState
import io.ton.walletkit.demo.presentation.ui.components.QuickActionsCard
import io.ton.walletkit.demo.presentation.ui.components.StatusHeader
import io.ton.walletkit.demo.presentation.ui.components.WalletSwitcher
import io.ton.walletkit.demo.presentation.ui.dialog.SignerConfirmationDialog
import io.ton.walletkit.demo.presentation.ui.dialog.UrlPromptDialog
import io.ton.walletkit.demo.presentation.ui.icons.Language
import io.ton.walletkit.demo.presentation.ui.sections.EventLogSection
import io.ton.walletkit.demo.presentation.ui.sections.JettonsSection
import io.ton.walletkit.demo.presentation.ui.sections.MasterchainInfoSection
import io.ton.walletkit.demo.presentation.ui.sections.NFTsSection
import io.ton.walletkit.demo.presentation.ui.sections.SessionsSection
import io.ton.walletkit.demo.presentation.ui.sections.WalletsSection
import io.ton.walletkit.demo.presentation.ui.sheet.AddWalletSheet
import io.ton.walletkit.demo.presentation.ui.sheet.BrowserSession
import io.ton.walletkit.demo.presentation.ui.sheet.BrowserSheet
import io.ton.walletkit.demo.presentation.ui.sheet.ConnectRequestSheet
import io.ton.walletkit.demo.presentation.ui.sheet.JettonDetailsSheet
import io.ton.walletkit.demo.presentation.ui.sheet.SignDataSheet
import io.ton.walletkit.demo.presentation.ui.sheet.SignMessageRequestSheet
import io.ton.walletkit.demo.presentation.ui.sheet.StakingSheet
import io.ton.walletkit.demo.presentation.ui.sheet.SwapSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransactionDetailSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransactionRequestSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransferJettonSheet
import io.ton.walletkit.demo.presentation.ui.sheet.WalletDetailsSheet
import io.ton.walletkit.demo.presentation.util.TestTags
import io.ton.walletkit.demo.presentation.viewmodel.NFTsListViewModel
import io.ton.walletkit.demo.presentation.viewmodel.SwapViewModel

// Pre-redesign main screen, kept behind a dev toggle. 5 taps on the title bar
// flips back to the new home screen.
private const val DEFAULT_DAPP_URL = "https://allure-test-runner.vercel.app/e2e"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyWalletScreen(
    state: WalletUiState,
    walletKit: ITONWalletKit,
    nftsViewModel: NFTsListViewModel?,
    swapViewModel: SwapViewModel?,
    actions: WalletActions,
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val onRefreshAll: () -> Unit = {
        actions.onRefresh()
        actions.onRefreshJettons()
        nftsViewModel?.refresh()
    }

    var selectedNFT by remember { mutableStateOf<TONNFT?>(null) }
    val nftDetailsSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )

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

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    val sheet = state.sheetState
    val showSheet = sheet !is SheetState.None
    LaunchedEffect(state.sheetState) {
        if (state.sheetState is SheetState.None && sheetState.isVisible) {
            sheetState.hide()
        }
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
            containerColor = TonTheme.colors.bgPrimary,
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
                    wallet = state.wallets.firstOrNull { it.address == sheet.request.walletAddress }
                        ?: activeWallet,
                )

                is SheetState.SignMessage -> SignMessageRequestSheet(
                    request = sheet.request,
                    onApprove = { actions.onApproveSignMessage(sheet.request) },
                    onReject = { actions.onRejectSignMessage(sheet.request) },
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

    state.pendingSignerConfirmation?.let { request ->
        SignerConfirmationDialog(
            request = request,
            onConfirm = actions::onConfirmSignerApproval,
            onCancel = actions::onCancelSignerApproval,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wallet_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.devToggleTaps {
                            val nowLegacy = DevPreferences.toggleLegacyMainScreen(context)
                            Toast.makeText(
                                context,
                                if (nowLegacy) "Legacy main screen ON" else "Legacy main screen OFF",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                },
                actions = {
                    IconButton(onClick = { actions.onOpenBrowser(DEFAULT_DAPP_URL) }) {
                        Icon(painterResource(R.drawable.ic_ton), contentDescription = "Open TonConnect Browser")
                    }
                    IconButton(
                        onClick = { actions.onOpenBrowser(DEFAULT_DAPP_URL, injectTonConnect = false) },
                        modifier = Modifier.testTag(TestTags.BROWSER_NO_INJECT_BUTTON),
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Open Plain Browser")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                if (state.isLoadingWallets || state.isLoadingSessions) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            StatusHeader(state)

            QuickActionsCard(
                onHandleUrl = actions::onUrlPromptClick,
                onAddWallet = actions::onAddWalletClick,
                onRefresh = onRefreshAll,
                onSwap = actions::onSwapClick,
            )

            if (state.wallets.size > 1) {
                WalletSwitcher(
                    wallets = state.wallets,
                    activeWalletAddress = state.activeWalletAddress,
                    isExpanded = state.isWalletSwitcherExpanded,
                    onToggle = actions::onToggleWalletSwitcher,
                    onSwitchWallet = actions::onSwitchWallet,
                    onRemoveWallet = actions::onRemoveWallet,
                    onRenameWallet = actions::onRenameWallet,
                )
            }

            WalletsSection(
                activeWallet = activeWallet,
                totalWallets = state.wallets.size,
                onWalletSelected = actions::onWalletDetails,
                onSendFromWallet = actions::onSendFromWallet,
                onStakeFromWallet = actions::onStakeFromWallet,
                isStreamingConnected = state.isStreamingConnected,
                onRefresh = actions::onRefresh,
            )

            if (nftsViewModel != null) {
                NFTsSection(
                    viewModel = nftsViewModel,
                    onNFTClick = { nft -> selectedNFT = nft },
                )
            }

            JettonsSection(
                jettons = state.jettons,
                isLoading = state.isLoadingJettons,
                error = state.jettonsError,
                canLoadMore = state.canLoadMoreJettons,
                onJettonClick = actions::onShowJettonDetails,
                onLoadMore = actions::onLoadMoreJettons,
                onRefresh = actions::onRefreshJettons,
            )

            if (activeWallet != null) {
                MasterchainInfoSection(network = activeWallet.network)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coming Soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SessionsSection(
                sessions = state.sessions,
                onDisconnect = actions::onDisconnectSession,
            )

            if (state.events.isNotEmpty()) {
                EventLogSection(events = state.events)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    selectedNFT?.let { nft ->
        ModalBottomSheet(
            onDismissRequest = { selectedNFT = null },
            sheetState = nftDetailsSheetState,
            containerColor = TonTheme.colors.bgPrimary,
            dragHandle = null,
        ) {
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
