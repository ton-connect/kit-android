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
package io.ton.walletkit.demo.designsystem.components.quickfilters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.tab.TonTab
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.theme.TonTheme

@Immutable
data class TonQuickFilter<Item>(
    val item: Item,
    val title: String,
    val icon: TonIcon? = null,
)

// Quick filters row = horizontal scroll of TonTab pills (active=black, inactive=light-gray).
@Composable
fun <Item> TonQuickFilters(
    selection: Item,
    items: List<TonQuickFilter<Item>>,
    onSelect: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(items = items, key = { it.item as Any }) { filter ->
            TonTab(
                title = filter.title,
                icon = filter.icon,
                isActive = filter.item == selection,
                onClick = { onSelect(filter.item) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TonQuickFiltersPreview() {
    TonTheme {
        var selected by remember { mutableStateOf("Trending") }
        TonQuickFilters(
            selection = selected,
            items = listOf(
                TonQuickFilter("Trending", title = "Trending", icon = TonIcon.Trend),
                TonQuickFilter("Top", title = "Top", icon = TonIcon.Hot),
                TonQuickFilter("Active", title = "Active"),
                TonQuickFilter("New", title = "New", icon = TonIcon.New),
            ),
            onSelect = { selected = it },
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}
