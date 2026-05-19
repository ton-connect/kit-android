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
package io.ton.walletkit.demo.designsystem.components.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.designsystem.tokens.TonTextStyleSpec

// Replaces iOS `.textStyle(.bodySemibold)` modifier. Applies the spec's TextStyle
// and uppercase transform when the spec asks for caps.
@Composable
fun TonText(
    text: String,
    style: TonTextStyleSpec,
    modifier: Modifier = Modifier,
    color: Color = TonTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = if (style.uppercase) text.uppercase() else text,
        modifier = modifier,
        color = color,
        style = style.style.copy(
            // Tighten the leading distribution so glyphs sit closer to their baseline
            // — without this, mixing 64sp + 40sp Text in a Row with [alignByBaseline]
            // still leaves the smaller glyph visually lower because its lineHeight
            // padding pushes it below the baseline. With Trim.Both the lineHeight
            // pads symmetrically around the baseline and price64 + price40 sit on
            // the same line, matching iOS's `.lastTextBaseline` HStack.
            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
            ),
        ),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}
