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
package io.ton.walletkit.demo.presentation.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.R
import io.ton.walletkit.demo.presentation.model.WalletSummary
import io.ton.walletkit.demo.presentation.ui.components.EmptyStateCard
import io.ton.walletkit.demo.presentation.ui.components.WalletCard
import io.ton.walletkit.demo.presentation.ui.preview.PreviewData

@Composable
fun WalletsSection(
    activeWallet: WalletSummary?,
    totalWallets: Int,
    onWalletSelected: (String) -> Unit,
    onSendFromWallet: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(WALLETS_SECTION_SPACING)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.wallets_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(WALLETS_TITLE_SPACING))
            Text(
                pluralStringResource(R.plurals.wallets_count, totalWallets, totalWallets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (activeWallet == null) {
            EmptyStateCard(
                title = stringResource(R.string.wallets_empty_title),
                description = stringResource(R.string.wallets_empty_description),
            )
        } else {
            WalletCard(
                wallet = activeWallet,
                onDetails = { onWalletSelected(activeWallet.address) },
                onSend = { onSendFromWallet(activeWallet.address) },
            )
            if (totalWallets > 1) {
                Text(
                    text = stringResource(R.string.wallets_switcher_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val WALLETS_SECTION_SPACING = 12.dp
private val WALLETS_TITLE_SPACING = 8.dp

@Preview(showBackground = true)
@Composable
private fun WalletsSectionPreview() {
    WalletsSection(
        activeWallet = PreviewData.wallet,
        totalWallets = 3,
        onWalletSelected = {},
        onSendFromWallet = {},
    )
}
