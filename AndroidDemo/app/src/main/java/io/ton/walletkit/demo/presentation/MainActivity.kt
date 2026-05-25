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
package io.ton.walletkit.demo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.demo.core.TONWalletKitHelper
import io.ton.walletkit.demo.core.WalletKitDemoApp
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.actions.WalletActionsImpl
import io.ton.walletkit.demo.presentation.dev.DevPreferences
import io.ton.walletkit.demo.presentation.state.CreateWalletFlow
import io.ton.walletkit.demo.presentation.state.SheetState
import io.ton.walletkit.demo.presentation.ui.screen.CreatePinScreen
import io.ton.walletkit.demo.presentation.ui.screen.LegacyWalletScreen
import io.ton.walletkit.demo.presentation.ui.screen.UnlockPinScreen
import io.ton.walletkit.demo.presentation.ui.screen.WalletScreen
import io.ton.walletkit.demo.presentation.ui.screen.onboarding.CreateWalletOnboardingScreen
import io.ton.walletkit.demo.presentation.ui.screen.onboarding.ImportWalletScreen
import io.ton.walletkit.demo.presentation.ui.screen.onboarding.RevealSeedPhraseScreen
import io.ton.walletkit.demo.presentation.viewmodel.WalletKitViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WalletKitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DevPreferences.ensureLoaded(applicationContext)
        setContent {
            TonTheme {
                Surface(color = TonTheme.colors.bgPrimary) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(
    viewModel: WalletKitViewModel,
) {
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val state by viewModel.state.collectAsState()
    val nftsViewModel by viewModel.nftsViewModel.collectAsState()
    val swapViewModel by viewModel.swapViewModel.collectAsState()
    val hasWallet = state.wallets.isNotEmpty()

    val context = LocalContext.current
    val walletKit = remember { mutableStateOf<ITONWalletKit?>(null) }

    LaunchedEffect(Unit) {
        val app = context.applicationContext as WalletKitDemoApp
        walletKit.value = TONWalletKitHelper.mainnet(app)
    }

    val walletActions = remember(viewModel) { WalletActionsImpl(viewModel) }

    when {
        !isPasswordSet -> {
            CreatePinScreen(
                onPinSet = { pin -> viewModel.setupPassword(pin) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        !isUnlocked -> {
            UnlockPinScreen(
                onUnlock = { pin -> viewModel.unlockWallet(pin) },
                onUnlockBiometric = { viewModel.unlockWithBiometric() },
                onReset = { viewModel.resetWallet() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        else -> {
            walletKit.value?.let { kit ->
                val useLegacyMainScreen by DevPreferences.useLegacyMainScreen.collectAsState()
                val createFlow by viewModel.createWalletFlow.collectAsState()
                val walletsBootstrapped = state.walletsBootstrapped

                LaunchedEffect(hasWallet, useLegacyMainScreen, walletsBootstrapped) {
                    if (!walletsBootstrapped) return@LaunchedEffect
                    if (hasWallet) return@LaunchedEffect
                    if (useLegacyMainScreen) {
                        if (state.sheetState !is SheetState.AddWallet) {
                            viewModel.openAddWalletSheet()
                        }
                    } else if (createFlow is CreateWalletFlow.Idle) {
                        viewModel.showCreateWalletOnboarding()
                    }
                }

                CreateWalletFlowHost(
                    flow = createFlow,
                    viewModel = viewModel,
                    fallback = {
                        if (useLegacyMainScreen) {
                            LegacyWalletScreen(
                                state = state,
                                walletKit = kit,
                                nftsViewModel = nftsViewModel,
                                swapViewModel = swapViewModel,
                                actions = walletActions,
                            )
                        } else {
                            WalletScreen(
                                state = state,
                                walletKit = kit,
                                nftsViewModel = nftsViewModel,
                                swapViewModel = swapViewModel,
                                actions = walletActions,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CreateWalletFlowHost(
    flow: CreateWalletFlow,
    viewModel: WalletKitViewModel,
    fallback: @Composable () -> Unit,
) {
    when (flow) {
        is CreateWalletFlow.Idle -> fallback()

        is CreateWalletFlow.Onboarding -> {
            // Only allow exit-via-back when the user already has a wallet to fall
            // back to — otherwise stay put rather than dropping onto an empty home.
            BackHandler(enabled = viewModel.state.value.wallets.isNotEmpty()) {
                viewModel.cancelCreateWalletFlow()
            }
            CreateWalletOnboardingScreen(
                onCreate = { viewModel.startCreateWallet() },
                onImport = { viewModel.startImportWalletFlow() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        is CreateWalletFlow.Reveal -> {
            BackHandler { viewModel.showCreateWalletOnboarding() }
            RevealSeedPhraseScreen(
                words = flow.words,
                onBack = { viewModel.showCreateWalletOnboarding() },
                onContinue = { viewModel.confirmRevealAndCreate() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        is CreateWalletFlow.ImportEntry -> {
            BackHandler { viewModel.showCreateWalletOnboarding() }
            ImportWalletScreen(
                wordCount = flow.wordCount,
                words = flow.words,
                canContinue = flow.isComplete,
                onWordCountChange = { viewModel.setImportWordCount(it) },
                onWordChange = { idx, value -> viewModel.setImportWord(idx, value) },
                onBack = { viewModel.showCreateWalletOnboarding() },
                onContinue = { viewModel.confirmImportWallet() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
