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
package io.ton.walletkit.demo.designsystem.components.pin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.designsystem.tokens.TonPrimitiveColors

// PIN dot indicator from figma `40001176:24966`. Always 4 slots (12×12) in a 96×24
// box, 16dp gap between them. Slot rendering:
//   • empty   → light-gray dot (tonBgLightGray)
//   • filled  → brand-blue dot (tonBgBrand)
//   • error   → red dot for all slots
//
// On the rising edge of [isError] the row plays a brief left-right shake — same
// damped sine motion the iOS `ShakeEffect` uses, expressed here as a sequenced
// [Animatable] so the spring/keyframe machinery stays in Compose.

@Composable
fun TonPinDots(
    pin: String,
    modifier: Modifier = Modifier,
    length: Int = 4,
    isError: Boolean = false,
) {
    val filled = pin.length

    var shakeKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(isError) {
        if (isError) shakeKey++
    }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            shakeOffset.snapTo(0f)
            // 6 hops of decreasing magnitude over ~360ms — matches iOS ShakeEffect envelope.
            listOf(10f, -10f, 8f, -8f, 5f, -5f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 60))
            }
        }
    }

    Row(
        modifier = modifier
            .height(24.dp)
            .graphicsLayer { translationX = shakeOffset.value },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(length) { index ->
            val color = when {
                isError -> TonPrimitiveColors.tonRed
                index < filled -> TonTheme.colors.bgBrand
                else -> TonTheme.colors.bgLightGray
            }
            PinSlot(color = color)
        }
    }
}

@Composable
private fun PinSlot(color: Color) {
    Spacer(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Preview(showBackground = true)
@Composable
private fun TonPinDotsPreview() {
    TonTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            TonPinDots(pin = "")
            TonPinDots(pin = "12")
            TonPinDots(pin = "1234")
            TonPinDots(pin = "1234", isError = true)
        }
    }
}
