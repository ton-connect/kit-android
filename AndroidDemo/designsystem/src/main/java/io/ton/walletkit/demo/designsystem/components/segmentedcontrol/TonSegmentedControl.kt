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
package io.ton.walletkit.demo.designsystem.components.segmentedcontrol

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.designsystem.theme.iosSpring

private val Height = 36.dp
private val TrackVerticalPadding = 2.dp
private val TrackHorizontalPadding = 3.dp
private val TrackCornerRadius = 18.dp
private val TabCornerRadius = 16.dp
private val TabHeight = 32.dp

@Composable
fun <Item> TonSegmentedControl(
    selection: Item,
    items: List<Item>,
    title: (Item) -> String,
    onSelect: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOf(selection).coerceAtLeast(0)
    val targetFraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = iosSpring(response = 0.3f, dampingFraction = 0.8f),
        label = "TonSegmentedControlPill",
    )

    BoxWithConstraints(
        modifier = modifier
            .height(Height)
            .clip(SmoothCornerShape(TrackCornerRadius))
            .background(TonTheme.colors.bgFillQuaternary)
            .padding(horizontal = TrackHorizontalPadding, vertical = TrackVerticalPadding),
    ) {
        val density = LocalDensity.current
        val itemWidthPx = remember(maxWidth, items.size) {
            with(density) { maxWidth.toPx() / items.size.coerceAtLeast(1) }
        }

        // Animated pill that slides under the selected item.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .height(TabHeight)
                .then(
                    if (items.isNotEmpty()) {
                        Modifier.fillMaxWidth(1f / items.size)
                    } else {
                        Modifier
                    },
                )
                .offsetX(itemWidthPx * targetFraction)
                .clip(SmoothCornerShape(TabCornerRadius))
                .background(TonTheme.colors.bgPrimary),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    TonText(
                        text = title(item),
                        style = TonTheme.typography.subheadline2Semibold,
                        color = TonTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}

// Inline graphics-layer offset by px on the X axis. Uses a layout-block so the pill
// participates in normal layout (BoxWithConstraints constrains its size).
private fun Modifier.offsetX(px: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(px.toInt(), 0)
        }
    },
)

@Preview(showBackground = true)
@Composable
private fun TonSegmentedControlPreview() {
    TonTheme {
        var selection by remember { mutableStateOf("1D") }
        TonSegmentedControl(
            selection = selection,
            items = listOf("1H", "1D", "1W", "1M", "ALL"),
            title = { it },
            onSelect = { selection = it },
            modifier = Modifier.padding(16.dp),
        )
    }
}
