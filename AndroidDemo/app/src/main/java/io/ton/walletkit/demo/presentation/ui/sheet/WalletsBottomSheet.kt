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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.model.WalletSummary

// Bottom-sheet content matching iOS WalletsBottomSheetView. Caller wraps this in a
// ModalBottomSheet so we reuse the existing dismiss/drag handle semantics in the
// demo. Tapping a row calls [onSelect]; the long-form delete flow goes through
// AlertDialog (Compose's idiomatic equivalent of iOS's .confirmationDialog).
@Composable
fun WalletsBottomSheet(
    wallets: List<WalletSummary>,
    activeWalletAddress: String?,
    onSelect: (WalletSummary) -> Unit,
    onCopyAddress: (String) -> Unit,
    onAddWallet: () -> Unit,
    onDelete: (WalletSummary) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var actionSheetWallet by remember { mutableStateOf<WalletSummary?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TonTheme.colors.bgPrimary)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TonText(
                text = "Wallets",
                style = TonTheme.typography.title3Bold,
                color = TonTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TonTheme.colors.bgFillTertiary)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                TonIconImage(
                    icon = TonIcon.Close,
                    size = 16.dp,
                    tint = TonTheme.colors.textSecondary,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            wallets.forEachIndexed { index, wallet ->
                WalletsBottomSheetRow(
                    title = walletTitle(wallet, index),
                    truncatedAddress = shortAddress(wallet.address),
                    isActive = wallet.address == activeWalletAddress,
                    onSelect = { onSelect(wallet) },
                    onCopyAddress = { onCopyAddress(wallet.address) },
                    onMore = { actionSheetWallet = wallet },
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(TonTheme.colors.bgBrand)
                .clickable(onClick = onAddWallet)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TonIconImage(
                icon = TonIcon.PlusLarge,
                size = 24.dp,
                tint = TonTheme.colors.textOnBrand,
            )
            TonText(
                text = "Add wallet",
                style = TonTheme.typography.bodySemibold,
                color = TonTheme.colors.textOnBrand,
            )
        }
    }

    actionSheetWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { actionSheetWallet = null },
            title = {
                val title = walletTitle(
                    wallet = wallet,
                    index = wallets.indexOfFirst { it.address == wallet.address },
                )
                TonText(
                    text = title,
                    style = TonTheme.typography.title3Bold,
                    color = TonTheme.colors.textPrimary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(wallet)
                        actionSheetWallet = null
                    },
                ) {
                    TonText(
                        text = "Delete wallet",
                        style = TonTheme.typography.bodySemibold,
                        color = TonTheme.colors.textError,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { actionSheetWallet = null }) {
                    TonText(
                        text = "Cancel",
                        style = TonTheme.typography.bodySemibold,
                        color = TonTheme.colors.textPrimary,
                    )
                }
            },
        )
    }
}

private fun walletTitle(wallet: WalletSummary, index: Int): String = if (index >= 0) "Wallet ${index + 1}" else "Wallet"

private fun shortAddress(address: String): String {
    if (address.length <= 12) return address
    val prefix = address.take(6)
    val suffix = address.takeLast(6)
    return "$prefix...$suffix"
}
