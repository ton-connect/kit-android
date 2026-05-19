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
package io.ton.walletkit.demo.designsystem.components.toggle

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.designsystem.theme.iosSpring

// Track 64x28, knob is a rounded pill 39x24 with 2px inset.
// On = accent blue, off = gray (#93939d).
private val TrackWidth = 64.dp
private val TrackHeight = 28.dp
private val KnobInset = 2.dp
private val KnobWidth = 39.dp
private val KnobHeight = 24.dp

@Composable
fun TonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetOffset = if (checked) TrackWidth - KnobWidth - KnobInset else KnobInset
    val knobOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = iosSpring(response = 0.25f, dampingFraction = 0.85f),
        label = "TonSwitchKnob",
    )

    Box(
        modifier = modifier
            .size(width = TrackWidth, height = TrackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) TonTheme.colors.bgBrand else TonTheme.colors.gray)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = KnobInset)
                .size(width = KnobWidth, height = KnobHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(TonTheme.colors.white),
        )
    }
}

// Mirrors iOS `Toggle("label", isOn: $on).toggleStyle(.tonSwitch)` — a row with the
// label on the leading edge and the switch on the trailing edge.
@Composable
fun TonLabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TonText(text = label, style = TonTheme.typography.body)
        TonSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun TonSwitchPreview() {
    TonTheme {
        var on by remember { mutableStateOf(true) }
        var off by remember { mutableStateOf(false) }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TonLabeledSwitch(label = "Enabled", checked = on, onCheckedChange = { on = it })
            TonLabeledSwitch(label = "Disabled", checked = off, onCheckedChange = { off = it })
        }
    }
}
