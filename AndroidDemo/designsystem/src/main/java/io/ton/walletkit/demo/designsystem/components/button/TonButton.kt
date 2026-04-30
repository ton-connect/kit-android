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
package io.ton.walletkit.demo.designsystem.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.loader.TonLoader
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.SmoothCornerShape
import io.ton.walletkit.demo.designsystem.theme.TonTheme

private val CornerRadius = 12.dp
private val HorizontalPadding = 24.dp
private val IconLabelSpacing = 8.dp

enum class TonButtonStyle { Primary, Secondary, Tertiary, Text }

enum class TonButtonSize(val height: Dp, val iconSize: Dp) {
    Small(36.dp, 16.dp),
    Medium(44.dp, 20.dp),
    Default(50.dp, 20.dp),
}

@Immutable
data class TonButtonConfig(
    val style: TonButtonStyle = TonButtonStyle.Primary,
    val size: TonButtonSize = TonButtonSize.Default,
    val leftIcon: TonIcon? = null,
    val rightIcon: TonIcon? = null,
    val isLoading: Boolean = false,
) {
    fun small() = copy(size = TonButtonSize.Small)
    fun medium() = copy(size = TonButtonSize.Medium)
    fun default() = copy(size = TonButtonSize.Default)
    fun leftIcon(icon: TonIcon) = copy(leftIcon = icon)
    fun rightIcon(icon: TonIcon) = copy(rightIcon = icon)
    fun isLoading(loading: Boolean) = copy(isLoading = loading)

    companion object {
        val Primary = TonButtonConfig(style = TonButtonStyle.Primary)
        val Secondary = TonButtonConfig(style = TonButtonStyle.Secondary)
        val Tertiary = TonButtonConfig(style = TonButtonStyle.Tertiary)
        val Text = TonButtonConfig(style = TonButtonStyle.Text)
    }
}

@Immutable
private data class TonButtonProperties(
    val iconColor: Color,
    val backgroundColor: Color,
    val labelColor: Color,
    val loaderColor: Color,
)

@Composable
fun TonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: TonButtonConfig = TonButtonConfig.Primary,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val state = when {
        !enabled -> ButtonState.Disabled
        isPressed -> ButtonState.Pressed
        else -> ButtonState.Default
    }
    val properties = config.style.properties(state)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(config.size.height)
            .clip(SmoothCornerShape(CornerRadius))
            .background(properties.backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !config.isLoading,
                onClick = onClick,
            )
            .padding(horizontal = HorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (config.isLoading) {
            TonLoader(size = config.size.iconSize, color = properties.loaderColor)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(IconLabelSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                config.leftIcon?.let {
                    TonIconImage(icon = it, size = config.size.iconSize, tint = properties.iconColor)
                }
                TonText(
                    text = text,
                    style = TonTheme.typography.bodySemibold,
                    color = properties.labelColor,
                )
                config.rightIcon?.let {
                    TonIconImage(icon = it, size = config.size.iconSize, tint = properties.iconColor)
                }
            }
        }
    }
}

private enum class ButtonState { Default, Pressed, Disabled }

@Composable
private fun TonButtonStyle.properties(state: ButtonState): TonButtonProperties {
    val colors = TonTheme.colors
    return when (this) {
        TonButtonStyle.Primary -> when (state) {
            ButtonState.Default -> TonButtonProperties(
                iconColor = colors.textOnBrand,
                backgroundColor = colors.bgBrand,
                labelColor = colors.textOnBrand,
                loaderColor = colors.textOnBrand,
            )
            ButtonState.Pressed -> TonButtonProperties(
                iconColor = colors.textOnBrand,
                backgroundColor = colors.bgBrandActive,
                labelColor = colors.textOnBrand,
                loaderColor = colors.textOnBrand,
            )
            ButtonState.Disabled -> TonButtonProperties(
                iconColor = colors.textOnBrand,
                backgroundColor = colors.bgDisabled,
                labelColor = colors.textTertiary,
                loaderColor = colors.textOnBrand,
            )
        }

        TonButtonStyle.Secondary -> when (state) {
            ButtonState.Default -> TonButtonProperties(
                iconColor = colors.textBrand,
                backgroundColor = colors.bgBrandSubtle,
                labelColor = colors.textBrand,
                loaderColor = colors.textBrand,
            )
            ButtonState.Pressed -> TonButtonProperties(
                iconColor = colors.textBrand,
                backgroundColor = colors.bgBrandActive,
                labelColor = colors.textBrand,
                loaderColor = colors.textBrand,
            )
            ButtonState.Disabled -> TonButtonProperties(
                iconColor = colors.textTertiary,
                backgroundColor = colors.bgDisabled,
                labelColor = colors.textTertiary,
                loaderColor = colors.textBrand,
            )
        }

        TonButtonStyle.Tertiary -> when (state) {
            ButtonState.Default -> TonButtonProperties(
                iconColor = colors.textPrimary,
                backgroundColor = colors.bgSecondary,
                labelColor = colors.textPrimary,
                loaderColor = colors.textBrand,
            )
            ButtonState.Pressed -> TonButtonProperties(
                iconColor = colors.textBrand,
                backgroundColor = colors.bgSecondary,
                labelColor = colors.textBrand,
                loaderColor = colors.textBrand,
            )
            ButtonState.Disabled -> TonButtonProperties(
                iconColor = colors.textTertiary,
                backgroundColor = colors.bgDisabled,
                labelColor = colors.textTertiary,
                loaderColor = colors.textBrand,
            )
        }

        TonButtonStyle.Text -> when (state) {
            ButtonState.Default -> TonButtonProperties(
                iconColor = colors.textBrand,
                backgroundColor = Color.Transparent,
                labelColor = colors.textBrand,
                loaderColor = colors.textBrand,
            )
            ButtonState.Pressed -> TonButtonProperties(
                iconColor = colors.textBrand.copy(alpha = 0.6f),
                backgroundColor = Color.Transparent,
                labelColor = colors.textBrand.copy(alpha = 0.6f),
                loaderColor = colors.textBrand,
            )
            ButtonState.Disabled -> TonButtonProperties(
                iconColor = colors.textTertiary,
                backgroundColor = Color.Transparent,
                labelColor = colors.textTertiary,
                loaderColor = colors.textBrand,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TonButtonAllSizesPreview() {
    TonTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TonButton(text = "Default 50", onClick = {}, config = TonButtonConfig.Primary)
            TonButton(text = "Medium 44", onClick = {}, config = TonButtonConfig.Primary.medium())
            TonButton(text = "Small 36", onClick = {}, config = TonButtonConfig.Primary.small())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TonButtonAllStylesPreview() {
    TonTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            TonButton(text = "Primary", onClick = {}, config = TonButtonConfig.Primary)
            TonButton(text = "Secondary", onClick = {}, config = TonButtonConfig.Secondary)
            TonButton(text = "Tertiary", onClick = {}, config = TonButtonConfig.Tertiary)
            TonButton(text = "Text", onClick = {}, config = TonButtonConfig.Text)
            TonButton(text = "Disabled", onClick = {}, enabled = false)
            TonButton(text = "Loading", onClick = {}, config = TonButtonConfig.Primary.isLoading(true))
        }
    }
}
