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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.presentation.model.BrowserPageState
import io.ton.walletkit.demo.presentation.model.BrowserTab

@Composable
fun BrowserTabBar(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    pageStates: Map<String, BrowserPageState>,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onAddTab: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val pageState = pageStates[tab.id]
            val label = pageState?.title?.takeIf { it.isNotBlank() }
                ?: pageState?.currentUrl?.takeIf { it.isNotBlank() }
                ?: tab.url

            FilterChip(
                selected = tab.id == activeTabId,
                onClick = { onTabSelect(tab.id) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onTabClose(tab.id) },
                        modifier = Modifier.size(TAB_CLOSE_BUTTON_SIZE),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close tab",
                            modifier = Modifier.size(TAB_CLOSE_ICON_SIZE),
                        )
                    }
                },
            )
        }

        IconButton(onClick = onAddTab) {
            Icon(Icons.Default.Add, contentDescription = "New tab")
        }
    }
}

private val TAB_CLOSE_BUTTON_SIZE = 20.dp
private val TAB_CLOSE_ICON_SIZE = 12.dp
