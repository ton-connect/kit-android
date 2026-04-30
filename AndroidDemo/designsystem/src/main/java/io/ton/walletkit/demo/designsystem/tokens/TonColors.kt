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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Semantic color tokens — every UI surface in the demo MUST go through these.
// Mirrors iOS `Tokens/TONColors+Semantic.swift`.
//
// Each property maps to the same primitive in light and dark today (matching iOS),
// but the data-class shape lets us swap dark-mode values later without touching call sites.
@Immutable
data class TonColors(
    // Text & Icon
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textBrand: Color,
    val textSuccess: Color,
    val textError: Color,
    val textOnBrand: Color,
    // Background
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgBrand: Color,
    val bgBrandSubtle: Color,
    val bgBrandActive: Color,
    val bgDisabled: Color,
    val bgOverlay: Color,
    val bgFillTertiary: Color,
    val bgFillQuaternary: Color,
    // Direct primitives intentionally exposed by the iOS DS for components that
    // bypass the semantic layer (e.g. TONTab uses `.tonBlack`/`.tonWhite` directly,
    // TONSwitchToggleStyle uses `.tonGray`/`.tonWhite`, TONBadge uses `.tonBgLightGray`).
    val black: Color,
    val white: Color,
    val gray: Color,
    val bgLightGray: Color,
    val bgBrandFillSubtle: Color,
)

fun lightTonColors() = TonColors(
    textPrimary = TonPrimitiveColors.tonBlack,
    textSecondary = TonPrimitiveColors.tonDarkGray,
    textTertiary = TonPrimitiveColors.tonGray,
    textBrand = TonPrimitiveColors.tonAccentBlue,
    textSuccess = TonPrimitiveColors.tonGreen,
    textError = TonPrimitiveColors.tonRed,
    textOnBrand = TonPrimitiveColors.tonWhite,
    bgPrimary = TonPrimitiveColors.tonBgWhite,
    bgSecondary = TonPrimitiveColors.tonBgSuperLightGray,
    bgBrand = TonPrimitiveColors.tonAccentBlue,
    bgBrandSubtle = TonPrimitiveColors.tonBgLightBlue,
    bgBrandActive = TonPrimitiveColors.tonBgLightBlueSecondary,
    bgDisabled = TonPrimitiveColors.tonBgLightGray,
    bgOverlay = TonPrimitiveColors.tonBgTertiaryFill,
    bgFillTertiary = TonPrimitiveColors.tonBgTertiaryFill,
    bgFillQuaternary = TonPrimitiveColors.tonBgQuaternaryFill,
    black = TonPrimitiveColors.tonBlack,
    white = TonPrimitiveColors.tonWhite,
    gray = TonPrimitiveColors.tonGray,
    bgLightGray = TonPrimitiveColors.tonBgLightGray,
    bgBrandFillSubtle = TonPrimitiveColors.tonBgBrandFillSubtle,
)

// iOS hardcodes the same primitive for both UI styles today. Kept identical here.
fun darkTonColors() = lightTonColors()
