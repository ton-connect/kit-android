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
package io.ton.walletkit.demo.designsystem.tokens

import androidx.compose.ui.graphics.Color

// Primitive palette — only [TonColors] semantic tokens should reach this object.
// Mirrors iOS `Tokens/TONColors.swift`.
object TonPrimitiveColors {
    // Text & Icon
    val tonBlack = Color(0xFF000000)
    val tonGray = Color(0xFF93939D)
    val tonDarkGray = Color(0xFF787881)
    val tonAccentBlue = Color(0xFF007AFF)
    val tonGreen = Color(0xFF2ABD4F)
    val tonRed = Color(0xFFFF3B30)
    val tonWhite = Color(0xFFFFFFFF)

    // Background
    val tonBgWhite = Color(0xFFFFFFFF)
    val tonBgLightGray = Color(0xFFEDEDF3)

    // 12% — Apple HIG tertiary fill
    val tonBgTertiaryFill = Color(0x1F747480)

    // 8% — Apple HIG quaternary fill (segmented track)
    val tonBgQuaternaryFill = Color(0x14747480)
    val tonBgBlack = Color(0xFF000000)
    val tonBgSuperLightGray = Color(0xFFF7F8FA)
    val tonBgLightBlue = Color(0xFFECF1FF)
    val tonBgLightBlueSecondary = Color(0xFFD4E5FF)

    // 10% accent blue (action button secondary)
    val tonBgBrandFillSubtle = Color(0x1A007AFF)
}
