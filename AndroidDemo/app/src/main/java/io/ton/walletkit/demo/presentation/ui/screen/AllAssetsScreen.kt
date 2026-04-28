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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeAssetItem
import io.ton.walletkit.demo.presentation.ui.components.wallet.home.WalletHomeAssetRow

// Full-screen list of every asset in the active wallet. Mirrors iOS's
// `WalletHomeView.allAssetsScreen` push destination — title bar with a back
// button, secondary background, vertical list of [WalletHomeAssetRow]s.
@Composable
fun AllAssetsScreen(
    assets: List<WalletHomeAssetItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TonTheme.colors.bgSecondary),
    ) {
        SubScreenTopBar(title = "My assets", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items = assets, key = { it.id }) { asset ->
                WalletHomeAssetRow(item = asset)
            }
        }
    }
}

// Lightweight back-arrow + title bar shared with [AllNFTsScreen]. Not extracted to
// the design system — iOS uses native NavigationBarTitleDisplayMode.inline which
// has its own toolbar-tied behavior; the demo's top bar is just a 56dp row.
@Composable
internal fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TonTheme.colors.bgPrimary)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(
                icon = TonIcon.ChevronBackSmall,
                size = 20.dp,
                tint = TonTheme.colors.textBrand,
                contentDescription = "Back",
            )
        }
        TonText(
            text = title,
            style = TonTheme.typography.bodySemibold,
            color = TonTheme.colors.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
