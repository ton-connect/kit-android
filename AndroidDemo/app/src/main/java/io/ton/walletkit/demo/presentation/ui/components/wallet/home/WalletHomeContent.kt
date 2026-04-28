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
package io.ton.walletkit.demo.presentation.ui.components.wallet.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme

// Body content of the new home screen — mirrors iOS `WalletHomeView.body`. Stateless;
// renders [balance, actions, assets, NFTs] in a vertical scroll.
//
// MAX_ASSETS = 3 visible (TON + 2 jettons), `hasMoreAssets` shows the "Show All" link.
// MAX_NFTS = 5 carousel cards.
@Composable
fun WalletHomeContent(
    totalBalanceInteger: String,
    totalBalanceFraction: String,
    assets: List<WalletHomeAssetItem>,
    nfts: List<WalletHomeNFTPreview>,
    hasMoreAssets: Boolean,
    hasMoreNFTs: Boolean,
    onDeposit: () -> Unit,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onShowAllAssets: () -> Unit,
    onShowAllNFTs: () -> Unit,
    onNFTTap: (WalletHomeNFTPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        WalletHomeBalance(
            totalBalanceInteger = totalBalanceInteger,
            totalBalanceFraction = totalBalanceFraction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 8.dp),
        )

        WalletHomeActionsRow(
            onDeposit = onDeposit,
            onSend = onSend,
            onReceive = onReceive,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        AssetsSection(
            assets = assets,
            hasMore = hasMoreAssets,
            onShowAll = onShowAllAssets,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        if (nfts.isNotEmpty()) {
            NFTsSection(
                nfts = nfts,
                hasMore = hasMoreNFTs,
                onShowAll = onShowAllNFTs,
                onNFTTap = onNFTTap,
            )
        }

        Box(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun AssetsSection(
    assets: List<WalletHomeAssetItem>,
    hasMore: Boolean,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(title = "My assets", showAll = hasMore, onShowAll = onShowAll)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            assets.forEach { asset ->
                WalletHomeAssetRow(item = asset)
            }
        }
    }
}

@Composable
private fun NFTsSection(
    nfts: List<WalletHomeNFTPreview>,
    hasMore: Boolean,
    onShowAll: () -> Unit,
    onNFTTap: (WalletHomeNFTPreview) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(
            title = "NFTs",
            showAll = hasMore,
            onShowAll = onShowAll,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        // iOS extends the carousel edge-to-edge with `padding(.horizontal, -16)
        // .padding(.leading, 16)` — Compose equivalent: zero horizontal padding here,
        // then [LazyRow.contentPadding] gives the leading 16dp inset for the first card.
        WalletHomeNFTsCarousel(
            nfts = nfts,
            onTap = onNFTTap,
            contentPadding = PaddingValues(horizontal = 16.dp),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    showAll: Boolean,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TonText(
            text = title,
            style = TonTheme.typography.title3Bold,
            color = TonTheme.colors.textPrimary,
        )
        Box(modifier = Modifier.weight(1f))
        if (showAll) {
            TonText(
                text = "Show All",
                style = TonTheme.typography.body,
                color = TonTheme.colors.textBrand,
                modifier = Modifier.clickable(onClick = onShowAll),
            )
        }
    }
}
