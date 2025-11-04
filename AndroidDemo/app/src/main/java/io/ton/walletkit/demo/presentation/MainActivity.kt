package io.ton.walletkit.demo.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.core.TONWalletKitHelper
import io.ton.walletkit.demo.core.WalletKitDemoApp
import io.ton.walletkit.demo.presentation.ui.screen.SetupPasswordScreen
import io.ton.walletkit.demo.presentation.ui.screen.UnlockWalletScreen
import io.ton.walletkit.demo.presentation.ui.screen.WalletScreen
import io.ton.walletkit.demo.presentation.viewmodel.WalletKitViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: WalletKitViewModel by viewModels {
        val app = application as WalletKitDemoApp
        WalletKitViewModel.factory(app, app.storage, app.sdkEvents, app.sdkInitialized)
    }
    
    private var walletKit: io.ton.walletkit.presentation.TONWalletKit? = null
    private var walletKitError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize WalletKit ONCE before setContent to avoid composition cancellation issues
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🔄 Starting WalletKit initialization...")
                val app = application as WalletKitDemoApp
                walletKit = TONWalletKitHelper.mainnet(app)
                Log.d("MainActivity", "✅ WalletKit initialized successfully")
                // Trigger recomposition after initialization
                setContent {
                    MainContent()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Failed to initialize WalletKit", e)
                walletKitError = e.message
                // Show content even if initialization failed
                setContent {
                    MainContent()
                }
            }
        }
        
        // Show initial content with loading state
        setContent {
            MainContent()
        }
    }
    
    @Composable
    private fun MainContent() {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                AppNavigation(
                    viewModel = viewModel,
                    walletKit = walletKit,
                    walletKitError = walletKitError
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    viewModel: WalletKitViewModel,
    walletKit: io.ton.walletkit.presentation.TONWalletKit?,
    walletKitError: String?
) {
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val state by viewModel.state.collectAsState()
    val nftsViewModel by viewModel.nftsViewModel.collectAsState()
    val hasWallet = state.wallets.isNotEmpty()

    when {
        // Step 1: Setup password (first time user)
        !isPasswordSet -> {
            SetupPasswordScreen(
                onPasswordSet = { password ->
                    viewModel.setupPassword(password)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Step 2: Unlock wallet (password set but locked)
        !isUnlocked -> {
            UnlockWalletScreen(
                onUnlock = { password ->
                    viewModel.unlockWallet(password)
                },
                onReset = {
                    viewModel.resetWallet()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Step 3 & 4: Main wallet screen (unlocked)
        // The AddWalletSheet will be shown automatically if no wallets exist
        else -> {
            when {
                // Show error if WalletKit failed to initialize
                walletKitError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Failed to initialize WalletKit:\n$walletKitError",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                
                // Show loading while WalletKit initializes
                walletKit == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                // Show WalletScreen when walletKit is ready
                else -> {
                WalletScreen(
                    state = state,
                    walletKit = walletKit,
                    nftsViewModel = nftsViewModel,
                    onAddWalletClick = viewModel::openAddWalletSheet,
                    onUrlPromptClick = viewModel::showUrlPrompt,
                    onOpenBrowser = { url -> viewModel.openBrowser(url) },
                    onRefresh = viewModel::refreshAll,
                    onDismissSheet = viewModel::dismissSheet,
                    onWalletDetails = viewModel::showWalletDetails,
                    onSendFromWallet = viewModel::openSendTransactionSheet,
                    onDisconnectSession = viewModel::disconnectSession,
                    onToggleWalletSwitcher = viewModel::toggleWalletSwitcher,
                    onSwitchWallet = viewModel::switchWallet,
                    onRemoveWallet = viewModel::removeWallet,
                    onRenameWallet = viewModel::renameWallet,
                    onImportWallet = viewModel::importWallet,
                    onGenerateWallet = viewModel::generateWallet,
                    onApproveConnect = viewModel::approveConnect,
                    onRejectConnect = viewModel::rejectConnect,
                    onApproveTransaction = viewModel::approveTransaction,
                    onRejectTransaction = viewModel::rejectTransaction,
                    onApproveSignData = viewModel::approveSignData,
                    onRejectSignData = viewModel::rejectSignData,
                    onConfirmSignerApproval = viewModel::confirmSignerApproval,
                    onCancelSignerApproval = viewModel::cancelSignerApproval,
                    onSendTransaction = viewModel::sendLocalTransaction,
                    onRefreshTransactions = viewModel::refreshTransactions,
                    onTransactionClick = viewModel::showTransactionDetail,
                    onHandleUrl = viewModel::handleTonConnectUrl,
                    onDismissUrlPrompt = viewModel::hideUrlPrompt,
                    onShowJettonDetails = viewModel::showJettonDetails,
                    onTransferJetton = viewModel::transferJetton,
                    onShowTransferJetton = viewModel::showTransferJetton,
                    onLoadMoreJettons = viewModel::loadMoreJettons,
                    onRefreshJettons = viewModel::refreshJettons,
                )
                }
            }
        }
    }
}
