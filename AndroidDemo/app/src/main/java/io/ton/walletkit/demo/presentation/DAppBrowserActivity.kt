package io.ton.walletkit.demo.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import io.ton.walletkit.demo.core.WalletKitDemoApp
import io.ton.walletkit.demo.presentation.state.SheetState
import io.ton.walletkit.demo.presentation.ui.sheet.ConnectRequestSheet
import io.ton.walletkit.demo.presentation.ui.sheet.SignDataSheet
import io.ton.walletkit.demo.presentation.ui.sheet.TransactionRequestSheet
import io.ton.walletkit.demo.presentation.viewmodel.BrowserViewModel
import io.ton.walletkit.demo.presentation.viewmodel.WalletKitViewModel
import io.ton.walletkit.presentation.TONWalletKit
import io.ton.walletkit.presentation.browser.injectTonConnect
import io.ton.walletkit.presentation.event.BrowserEvent
import io.ton.walletkit.presentation.event.TONWalletKitEvent
import kotlinx.coroutines.launch

/**
 * Activity for testing dApp browser with TonConnect support using the simplified extension API.
 *
 * This demonstrates:
 * - Creating a standard WebView
 * - Injecting TonConnect support with just one line: webView.injectTonConnect(TONWalletKit)
 * - TONWalletKit handles all TonConnect events automatically
 * - SDK handles cleanup automatically
 *
 * **How it works:**
 * 1. Create a WebView and call webView.injectTonConnect(TONWalletKit) - that's it!
 * 2. When the dApp calls window.wallet.tonconnect.connect(), the SDK automatically:
 *    - Captures the request in inject.mjs
 *    - Forwards it to TONWalletKit's internal engine
 *    - TONWalletKit emits events through its configured event handler
 * 3. Your app shows approval UI (handled in WalletKitViewModel via TONWalletKit events)
 * 4. User approves/rejects → response automatically sent back to the dApp
 * 5. When WebView is destroyed → SDK automatically cleans up resources
 */
class DAppBrowserActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels { BrowserViewModel.factory() }
    private val walletKitViewModel: WalletKitViewModel by viewModels {
        val app = application as WalletKitDemoApp
        WalletKitViewModel.factory(app, app.storage, app.sdkEvents, app.sdkInitialized)
    }
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dappUrl = intent.getStringExtra(EXTRA_DAPP_URL) ?: DEFAULT_DAPP_URL

        // Create WebView
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        // Inject TonConnect support - SDK handles everything automatically!
        webView?.injectTonConnect(TONWalletKit)

        // Load the dApp
        webView?.loadUrl(dappUrl)

        // Observe SDK events and convert browser events to BrowserViewModel
        lifecycleScope.launch {
            (application as? WalletKitDemoApp)?.sdkEvents?.collect { event ->
                when (event) {
                    is TONWalletKitEvent.BrowserPageStarted -> {
                        browserViewModel.handleBrowserEvent(BrowserEvent.PageStarted(event.url))
                    }
                    is TONWalletKitEvent.BrowserPageFinished -> {
                        browserViewModel.handleBrowserEvent(BrowserEvent.PageFinished(event.url))
                    }
                    is TONWalletKitEvent.BrowserError -> {
                        browserViewModel.handleBrowserEvent(BrowserEvent.Error(event.message))
                    }
                    is TONWalletKitEvent.BrowserBridgeRequest -> {
                        browserViewModel.handleBrowserEvent(
                            BrowserEvent.BridgeRequest(
                                messageId = event.messageId,
                                method = event.method,
                                request = event.request
                            )
                        )
                    }
                    is TONWalletKitEvent.ConnectRequest -> {
                        // Forward to WalletKitViewModel which has the approval UI logic
                        Log.d("DAppBrowserActivity", "ConnectRequest received, showing approval dialog")
                        // The WalletKitViewModel will handle showing the sheet
                    }
                    else -> {
                        // Other events handled by WalletKitViewModel automatically
                    }
                }
            }
        }

        setContent {
            MaterialTheme {
                @OptIn(ExperimentalMaterial3Api::class)
                @Composable
                fun BrowserWithApprovalSheets() {
                    // Observe WalletKit state for showing approval dialogs
                    val walletKitState by walletKitViewModel.state.collectAsState()
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Browser content
                        BrowserScreen(
                            viewModel = browserViewModel,
                            webView = webView,
                            onBack = { finish() }
                        )
                        
                        // Approval sheets overlay (when needed)
                        val sheet = walletKitState.sheet
                        if (sheet != null) {
                            ModalBottomSheet(
                                onDismissRequest = walletKitViewModel::dismissSheet,
                                dragHandle = null,
                            ) {
                                when (sheet) {
                                    is SheetState.Connect -> ConnectRequestSheet(
                                        request = sheet.request,
                                        wallets = walletKitState.wallets,
                                        onApprove = { req, wallet -> walletKitViewModel.approveConnect(req, wallet) },
                                        onReject = { req -> walletKitViewModel.rejectConnect(req) },
                                    )

                                    is SheetState.Transaction -> TransactionRequestSheet(
                                        request = sheet.request,
                                        onApprove = { walletKitViewModel.approveTransaction(sheet.request) },
                                        onReject = { walletKitViewModel.rejectTransaction(sheet.request) },
                                    )

                                    is SheetState.SignData -> SignDataSheet(
                                        request = sheet.request,
                                        onApprove = { walletKitViewModel.approveSignData(sheet.request) },
                                        onReject = { walletKitViewModel.rejectSignData(sheet.request) },
                                    )
                                    
                                    else -> {
                                        // Other sheets (AddWallet, WalletDetails, etc.) not needed in browser
                                    }
                                }
                            }
                        }
                    }
                }
                
                BrowserWithApprovalSheets()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // SDK handles cleanup automatically when WebView is detached
        webView?.destroy()
        webView = null
    }

    companion object {
        private const val EXTRA_DAPP_URL = "dapp_url"
        const val DEFAULT_DAPP_URL = "https://tonconnect-demo-dapp-with-react-ui.vercel.app/"

        /**
         * Start the dApp browser with the specified URL.
         */
        fun start(context: Context, dappUrl: String = DEFAULT_DAPP_URL) {
            val intent = Intent(context, DAppBrowserActivity::class.java).apply {
                putExtra(EXTRA_DAPP_URL, dappUrl)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserScreen(
    viewModel: BrowserViewModel,
    webView: WebView?,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("dApp Browser") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Status Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        text = state.currentUrl.ifEmpty { "Loading..." },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append("Requests: ${state.requestCount}")
                            if (state.lastRequest != null) {
                                append(" • ${state.lastRequest}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Loading indicator
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (webView != null) {
                    AndroidView(
                        factory = {
                            webView.apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Error Snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(error)
                }
            }
        }
    }
}
