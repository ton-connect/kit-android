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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// All 23 styles from the iOS `TONTypography.swift` (Figma "Typography" section).
//
// iOS uses SF Pro / SF Pro Rounded. Android falls back to the platform default sans-serif —
// when product wants exact parity we'll bundle SF Pro and swap [defaultFontFamily] /
// [roundedFontFamily]. Sizes, weights, line heights and tracking are 1:1 with iOS.
@Immutable
data class TonTextStyleSpec(
    val name: String,
    val style: TextStyle,
    val uppercase: Boolean = false,
)

@Immutable
data class TonTypography(
    val price64: TonTextStyleSpec,
    val price44: TonTextStyleSpec,
    val price40: TonTextStyleSpec,
    val title1: TonTextStyleSpec,
    val title2: TonTextStyleSpec,
    val title3Bold: TonTextStyleSpec,
    val title3Semibold: TonTextStyleSpec,
    val title3RoundedRegular: TonTextStyleSpec,
    val body: TonTextStyleSpec,
    val bodyMedium: TonTextStyleSpec,
    val bodySemibold: TonTextStyleSpec,
    val bodyRoundedSemibold: TonTextStyleSpec,
    val callout: TonTextStyleSpec,
    val calloutMedium: TonTextStyleSpec,
    val subheadline1: TonTextStyleSpec,
    val subheadline2: TonTextStyleSpec,
    val subheadline2Medium: TonTextStyleSpec,
    val subheadline2Semibold: TonTextStyleSpec,
    val footnote: TonTextStyleSpec,
    val footnoteSemibold: TonTextStyleSpec,
    val footnoteCaps: TonTextStyleSpec,
    val caption1: TonTextStyleSpec,
    val caption2Medium: TonTextStyleSpec,
    val caption2Semibold: TonTextStyleSpec,
    val caption2MediumCaps: TonTextStyleSpec,
) {
    val allStyles: List<TonTextStyleSpec>
        get() = listOf(
            price64, price44, price40,
            title1, title2, title3Bold, title3Semibold, title3RoundedRegular,
            body, bodyMedium, bodySemibold, bodyRoundedSemibold,
            callout, calloutMedium,
            subheadline1,
            subheadline2, subheadline2Medium, subheadline2Semibold,
            footnote, footnoteSemibold, footnoteCaps,
            caption1,
            caption2Medium, caption2Semibold, caption2MediumCaps,
        )
}

// SF Pro substitute. Inter is the Figma stand-in our designers use; downloaded
// at runtime via the Google Play Services Font Provider. See [TonFontFamilies].
//
// letterSpacing is left in `.sp` so it scales with the user's font-size accessibility
// preference together with the base size — same proportion at any setting. iOS does
// not scale system(size:) text with Dynamic Type, so on a phone with cranked-up text
// size Android will track-and-grow while iOS will hold; that's a deliberate Android
// behaviour, not a parity miss.
private val defaultFontFamily: FontFamily = TonDefaultFontFamily
private val roundedFontFamily: FontFamily = TonRoundedFontFamily

private fun style(
    name: String,
    sizeSp: Float,
    weight: FontWeight,
    lineHeightSp: Float,
    letterSpacingSp: Float,
    family: FontFamily = defaultFontFamily,
    uppercase: Boolean = false,
): TonTextStyleSpec = TonTextStyleSpec(
    name = name,
    style = TextStyle(
        fontFamily = family,
        fontSize = sizeSp.sp,
        fontWeight = weight,
        lineHeight = lineHeightSp.sp,
        // iOS UIKit `.kerning` is in points (~ sp on Android). Compose `letterSpacing`
        // accepts both em and sp; sp keeps the absolute tracking iOS uses.
        letterSpacing = letterSpacingSp.sp,
    ),
    uppercase = uppercase,
)

fun defaultTonTypography(): TonTypography = TonTypography(
    // Price (SF Pro Rounded Bold)
    price64 = style("Price 64", 64f, FontWeight.Bold, 82f, -0.64f, family = roundedFontFamily),
    price44 = style("Price 44", 44f, FontWeight.Bold, 46f, -1.32f, family = roundedFontFamily),
    price40 = style("Price 40", 40f, FontWeight.Bold, 46f, -0.5f, family = roundedFontFamily),
    // Titles
    title1 = style("Title 1", 28f, FontWeight.Bold, 34f, 0.38f),
    title2 = style("Title 2", 22f, FontWeight.SemiBold, 28f, -0.264f),
    title3Bold = style("Title 3 Bold", 20f, FontWeight.Bold, 24f, -0.45f),
    title3Semibold = style("Title 3 Semibold", 20f, FontWeight.SemiBold, 24f, -0.45f),
    title3RoundedRegular = style("Title 3 Rounded Regular", 20f, FontWeight.Normal, 24f, -0.45f, family = roundedFontFamily),
    // Body (17)
    body = style("Body", 17f, FontWeight.Normal, 22f, -0.43f),
    bodyMedium = style("Body Medium", 17f, FontWeight.Medium, 22f, -0.43f),
    bodySemibold = style("Body Semibold", 17f, FontWeight.SemiBold, 22f, -0.43f),
    bodyRoundedSemibold = style("Body Rounded Semibold", 17f, FontWeight.SemiBold, 22f, -0.12f, family = roundedFontFamily),
    // Callout (16)
    callout = style("Callout", 16f, FontWeight.Normal, 22f, -0.31f),
    calloutMedium = style("Callout Medium", 16f, FontWeight.Medium, 22f, -0.31f),
    // Subheadline 1 (15 Rounded)
    subheadline1 = style("Subheadline 1", 15f, FontWeight.SemiBold, 20f, 0.44f, family = roundedFontFamily),
    // Subheadline 2 (14)
    subheadline2 = style("Subheadline 2", 14f, FontWeight.Normal, 18f, -0.154f),
    subheadline2Medium = style("Subheadline 2 Medium", 14f, FontWeight.Medium, 18f, -0.154f),
    subheadline2Semibold = style("Subheadline 2 Semibold", 14f, FontWeight.SemiBold, 18f, -0.154f),
    // Footnote (13)
    footnote = style("Footnote", 13f, FontWeight.Normal, 18f, -0.078f),
    footnoteSemibold = style("Footnote Semibold", 13f, FontWeight.SemiBold, 18f, -0.08f),
    footnoteCaps = style("Footnote Caps", 13f, FontWeight.Normal, 18f, -0.078f, uppercase = true),
    // Caption 1 (12)
    caption1 = style("Caption 1", 12f, FontWeight.Normal, 16f, 0f),
    // Caption 2 (11)
    caption2Medium = style("Caption 2 Medium", 11f, FontWeight.Medium, 13f, 0.06f),
    caption2Semibold = style("Caption 2 Semibold", 11f, FontWeight.SemiBold, 12f, -0.11f),
    caption2MediumCaps = style("Caption 2 Medium Caps", 11f, FontWeight.Medium, 13f, 0.06f, uppercase = true),
)
