package io.ton.walletkit.demo.ui.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Link
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.model.ConnectRequestUi
import io.ton.walletkit.demo.model.SignDataRequestUi
import io.ton.walletkit.demo.model.TonNetwork
import io.ton.walletkit.demo.model.TransactionRequestUi
import io.ton.walletkit.demo.model.WalletSummary
import io.ton.walletkit.demo.state.SheetState
import io.ton.walletkit.demo.state.WalletUiState
import io.ton.walletkit.demo.ui.components.QuickActionsCard
import io.ton.walletkit.demo.ui.components.StatusHeader
import io.ton.walletkit.demo.ui.dialog.UrlPromptDialog
import io.ton.walletkit.demo.ui.preview.PreviewData
import io.ton.walletkit.demo.ui.sections.EventLogSection
import io.ton.walletkit.demo.ui.sections.SessionsSection
import io.ton.walletkit.demo.ui.sections.WalletsSection
import io.ton.walletkit.demo.ui.sheet.AddWalletSheet
import io.ton.walletkit.demo.ui.sheet.ConnectRequestSheet
import io.ton.walletkit.demo.ui.sheet.SignDataSheet
import io.ton.walletkit.demo.ui.sheet.TransactionRequestSheet
import io.ton.walletkit.demo.ui.sheet.WalletDetailsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    state: WalletUiState,
    onAddWalletClick: () -> Unit,
    onUrlPromptClick: () -> Unit,
    onRefresh: () -> Unit,
    onDismissSheet: () -> Unit,
    onWalletDetails: (String) -> Unit,
    onDisconnectSession: (String) -> Unit,
    onImportWallet: (String, TonNetwork, List<String>) -> Unit,
    onGenerateWallet: (String, TonNetwork) -> Unit,
    onApproveConnect: (ConnectRequestUi, WalletSummary) -> Unit,
    onRejectConnect: (ConnectRequestUi) -> Unit,
    onApproveTransaction: (TransactionRequestUi) -> Unit,
    onRejectTransaction: (TransactionRequestUi) -> Unit,
    onApproveSignData: (SignDataRequestUi) -> Unit,
    onRejectSignData: (SignDataRequestUi) -> Unit,
    onHandleUrl: (String) -> Unit,
    onDismissUrlPrompt: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheet = state.sheetState
    val showSheet = sheet !is SheetState.None
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState = sheetState,
            dragHandle = null,
        ) {
            when (sheet) {
                SheetState.AddWallet -> AddWalletSheet(
                    onDismiss = onDismissSheet,
                    onImportWallet = onImportWallet,
                    onGenerateWallet = onGenerateWallet,
                    walletCount = state.wallets.size,
                )

                is SheetState.Connect -> ConnectRequestSheet(
                    request = sheet.request,
                    wallets = state.wallets,
                    onApprove = { wallet -> onApproveConnect(sheet.request, wallet) },
                    onReject = { onRejectConnect(sheet.request) },
                )

                is SheetState.Transaction -> TransactionRequestSheet(
                    request = sheet.request,
                    onApprove = { onApproveTransaction(sheet.request) },
                    onReject = { onRejectTransaction(sheet.request) },
                )

                is SheetState.SignData -> SignDataSheet(
                    request = sheet.request,
                    onApprove = { onApproveSignData(sheet.request) },
                    onReject = { onRejectSignData(sheet.request) },
                )

                is SheetState.WalletDetails -> WalletDetailsSheet(
                    wallet = sheet.wallet,
                    onDismiss = onDismissSheet,
                )

                SheetState.None -> Unit
            }
        }
    }

    if (state.isUrlPromptVisible) {
        UrlPromptDialog(
            onDismiss = onDismissUrlPrompt,
            onConfirm = onHandleUrl,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TonWallet Demo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onUrlPromptClick) {
                        Icon(Icons.Outlined.Link, contentDescription = "Handle URL")
                    }
                    IconButton(onClick = onAddWalletClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Wallet")
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
            if (state.isLoadingWallets || state.isLoadingSessions) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            StatusHeader(state)

            QuickActionsCard(
                onHandleUrl = onUrlPromptClick,
                onAddWallet = onAddWalletClick,
                onRefresh = onRefresh,
            )

            WalletsSection(
                wallets = state.wallets,
                onWalletSelected = onWalletDetails,
            )

            SessionsSection(
                sessions = state.sessions,
                onDisconnect = onDisconnectSession,
            )

            if (state.events.isNotEmpty()) {
                EventLogSection(events = state.events)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WalletScreenPreview() {
    WalletScreen(
        state = PreviewData.uiState,
        onAddWalletClick = {},
        onUrlPromptClick = {},
        onRefresh = {},
        onDismissSheet = {},
        onWalletDetails = {},
        onDisconnectSession = {},
        onImportWallet = { _, _, _ -> },
        onGenerateWallet = { _, _ -> },
        onApproveConnect = { _, _ -> },
        onRejectConnect = {},
        onApproveTransaction = {},
        onRejectTransaction = {},
        onApproveSignData = {},
        onRejectSignData = {},
        onHandleUrl = {},
        onDismissUrlPrompt = {},
    )
}
