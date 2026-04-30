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
package io.ton.walletkit.demo.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import io.ton.walletkit.demo.designsystem.tokens.TonColors
import io.ton.walletkit.demo.designsystem.tokens.TonTypography
import io.ton.walletkit.demo.designsystem.tokens.darkTonColors
import io.ton.walletkit.demo.designsystem.tokens.defaultTonTypography
import io.ton.walletkit.demo.designsystem.tokens.lightTonColors

private val LocalTonColors = staticCompositionLocalOf<TonColors> {
    error("TonColors not provided. Wrap your UI in TonTheme { ... }")
}

private val LocalTonTypography = staticCompositionLocalOf<TonTypography> {
    error("TonTypography not provided. Wrap your UI in TonTheme { ... }")
}

@Composable
fun TonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: TonColors = if (darkTheme) darkTonColors() else lightTonColors(),
    typography: TonTypography = defaultTonTypography(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTonColors provides colors,
        LocalTonTypography provides typography,
    ) {
        // Compose call sites that haven't migrated yet still see a sane MaterialTheme.
        MaterialTheme(content = content)
    }
}

// Mirrors iOS access pattern (`Color.tonTextPrimary`) by exposing tokens through the theme:
//   TonTheme.colors.textPrimary
//   TonTheme.typography.bodySemibold
object TonTheme {
    val colors: TonColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTonColors.current

    val typography: TonTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTonTypography.current
}
