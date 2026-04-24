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
package io.ton.walletkit.demo.presentation.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ton.walletkit.ITONWallet
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.api.generated.TONStakingQuote
import io.ton.walletkit.api.generated.TONStakingQuoteDirection
import io.ton.walletkit.api.generated.TONUnstakeMode
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.ui.components.NetworkBadge
import io.ton.walletkit.demo.presentation.util.TestTags
import io.ton.walletkit.demo.presentation.util.TonFormatter
import io.ton.walletkit.demo.presentation.viewmodel.StakingViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakingSheet(
    wallet: WalletSummary,
    walletKit: ITONWalletKit,
    sheetKey: Long,
    onDismiss: () -> Unit,
) {
    var tonWallet by remember(wallet.address) { mutableStateOf<ITONWallet?>(null) }
    var isLoadingWallet by remember(wallet.address) { mutableStateOf(true) }

    LaunchedEffect(wallet.address) {
        isLoadingWallet = true
        tonWallet = walletKit.getWallets().firstOrNull { it.address.value == wallet.address }
        isLoadingWallet = false
    }

    when {
        isLoadingWallet -> LoadingContent()
        tonWallet == null -> MissingWalletContent(onDismiss = onDismiss)
        else -> StakingContent(
            wallet = wallet,
            tonWallet = tonWallet!!,
            walletKit = walletKit,
            sheetKey = sheetKey,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun LoadingContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MissingWalletContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Wallet not found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "The staking screen could not resolve the active wallet instance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onDismiss) {
            Text("Close")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakingContent(
    wallet: WalletSummary,
    tonWallet: ITONWallet,
    walletKit: ITONWalletKit,
    sheetKey: Long,
    onDismiss: () -> Unit,
) {
    val viewModel: StakingViewModel = viewModel(
        key = "staking:${wallet.address}:$sheetKey",
        factory = StakingViewModel.factory(tonWallet, walletKit, wallet.network),
    )
    val state by viewModel.state.collectAsState()
    val isBusy = state.isLoadingQuote || state.isExecuting
    val availableBalanceNano = state.availableBalanceNano ?: wallet.balanceNano

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Staking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    wallet.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }

        NetworkBadge(wallet.network)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledValue("Available balance", formatTonDisplay(availableBalanceNano, wallet.balance))
                LabeledValue("Staked balance", state.stakedBalance?.let { formatTokenDisplay(it.stakedBalance) } ?: "Loading...")
                LabeledValue("Instant unstake available", state.stakedBalance?.let { formatTokenDisplay(it.instantUnstakeAvailable) } ?: "Loading...")
                LabeledValue("Provider APY", state.formattedAPY ?: "Loading...")
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                TONStakingQuoteDirection.stake to "Stake",
                TONStakingQuoteDirection.unstake to "Unstake",
            )
            options.forEachIndexed { index, (direction, label) ->
                SegmentedButton(
                    selected = state.direction == direction,
                    onClick = { viewModel.setDirection(direction) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    enabled = !isBusy,
                    label = { Text(label) },
                )
            }
        }

        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::setAmount,
            label = { Text("Amount (${state.inputTokenSymbol})") },
            placeholder = { Text("0.00") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.STAKING_AMOUNT_FIELD),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !isBusy,
            isError = state.amount.isNotEmpty() && !isValidAmount(state.amount),
            supportingText = {
                when {
                    state.amount.isNotEmpty() && !isValidAmount(state.amount) -> {
                        Text("Enter a valid positive amount.")
                    }
                    state.direction == TONStakingQuoteDirection.stake && isAmountBelowMinimumStake(state.amount) -> {
                        Text("TonStakers minimum stake is 1 TON.")
                    }
                    state.direction == TONStakingQuoteDirection.stake && isAmountAboveBalance(state.amount, availableBalanceNano) -> {
                        Text("Amount is higher than the available wallet balance.")
                    }
                    else -> {
                        Text("Preview first. Confirm sends the transaction from this wallet.")
                    }
                }
            },
        )

        if (state.direction == TONStakingQuoteDirection.unstake) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Unstake mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.supportedModes.forEach { mode ->
                        FilterChip(
                            selected = state.unstakeMode == mode,
                            onClick = { viewModel.setUnstakeMode(mode) },
                            enabled = !isBusy,
                            label = { Text(mode.toDisplayLabel()) },
                        )
                    }
                }
            }
        }

        state.currentQuote?.let { quote ->
            QuoteCard(quote = quote, receiveTokenSymbol = state.receiveTokenSymbol)
        }

        state.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.currentQuote != null) {
                OutlinedButton(
                    onClick = viewModel::cancelQuote,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.STAKING_CANCEL_BUTTON),
                    enabled = !isBusy,
                ) {
                    Text("Reset")
                }
            }

            Button(
                onClick = viewModel::buttonAction,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.STAKING_ACTION_BUTTON),
                enabled = !isBusy &&
                    state.amount.isNotBlank() &&
                    isValidAmount(state.amount) &&
                    (state.direction != TONStakingQuoteDirection.stake || !isAmountBelowMinimumStake(state.amount)) &&
                    (state.direction != TONStakingQuoteDirection.stake || !isAmountAboveBalance(state.amount, availableBalanceNano)),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(state.buttonTitle)
            }
        }

        Text(
            text = "Use a funded wallet. The first tap previews the quote, and the second tap confirms the blockchain transaction.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuoteCard(
    quote: TONStakingQuote,
    receiveTokenSymbol: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Quote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LabeledValue("You send", "${formatTokenDisplay(quote.amountIn)} ${quote.direction.inputSymbol()}")
            LabeledValue("You receive", "${formatTokenDisplay(quote.amountOut)} $receiveTokenSymbol")
            LabeledValue("Provider", quote.providerId)
            quote.apy?.let { apy ->
                LabeledValue("Quote APY", formatApy(apy))
            }
            quote.unstakeMode?.let { mode ->
                LabeledValue("Unstake mode", mode.toDisplayLabel())
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun TONUnstakeMode.toDisplayLabel(): String = when (this) {
    TONUnstakeMode.instant -> "Instant"
    TONUnstakeMode.whenAvailable -> "When Available"
    TONUnstakeMode.roundEnd -> "Round End"
}

private fun TONStakingQuoteDirection.inputSymbol(): String = when (this) {
    TONStakingQuoteDirection.stake -> "TON"
    TONStakingQuoteDirection.unstake -> "tsTON"
}

private fun isValidAmount(value: String): Boolean = runCatching {
    (value.toBigDecimalOrNull() ?: return false) > BigDecimal.ZERO
}.getOrDefault(false)

private fun isAmountAboveBalance(value: String, balanceNano: String?): Boolean = runCatching {
    if (value.isBlank() || balanceNano.isNullOrBlank()) return false
    val amountNano = TonFormatter.tonToNano(value).toBigDecimal()
    amountNano > balanceNano.toBigDecimal()
}.getOrDefault(false)

private fun isAmountBelowMinimumStake(value: String): Boolean = runCatching {
    if (value.isBlank()) return false
    value.toBigDecimal() < BigDecimal.ONE
}.getOrDefault(false)

private fun formatTonDisplay(balanceNano: String?, fallback: String?): String = when {
    !balanceNano.isNullOrBlank() -> TonFormatter.formatTon(balanceNano)
    !fallback.isNullOrBlank() -> fallback
    else -> "0"
}

private fun formatTokenDisplay(value: String?): String = runCatching {
    if (value.isNullOrBlank()) {
        return "0"
    }
    BigDecimal(value)
        .setScale(4, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
}.getOrDefault(value ?: "0")

private fun formatApy(apyPercent: Number): String = String.format(Locale.US, "%.2f%%", apyPercent.toDouble())
