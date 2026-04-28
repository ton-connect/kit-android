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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme

@Immutable
data class WalletHomeNFTPreview(
    val id: String,
    val name: String,
    val address: String,
    val imageUrl: String?,
)

private val CardSize = 168.dp

// Horizontal carousel of square NFT cards. Cards are 168x168 with rounded-16 corners
// (continuous via SmoothCornerShape — matches iOS .continuous), title (bodySemibold)
// + truncated #suffix address below.
@Composable
fun WalletHomeNFTsCarousel(
    nfts: List<WalletHomeNFTPreview>,
    onTap: (WalletHomeNFTPreview) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding,
    ) {
        items(items = nfts, key = { it.id }) { nft ->
            Column(
                modifier = Modifier
                    .width(CardSize)
                    .clickable { onTap(nft) },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(CardSize)
                        .clip(SmoothCornerShape(16.dp))
                        .background(TonTheme.colors.bgFillTertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    val imageUrl = nft.imageUrl
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = nft.name,
                            modifier = Modifier.size(CardSize),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        TonIconImage(
                            icon = TonIcon.Doc,
                            size = 28.dp,
                            tint = TonTheme.colors.textTertiary,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TonText(
                        text = nft.name,
                        style = TonTheme.typography.bodySemibold,
                        color = TonTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TonText(
                        text = shortNftAddress(nft.address),
                        style = TonTheme.typography.subheadline2,
                        color = TonTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun shortNftAddress(address: String): String {
    if (address.length <= 8) return address
    return "#${address.takeLast(6)}"
}
