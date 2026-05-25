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
package io.ton.walletkit.demo.presentation.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.button.TonButton
import io.ton.walletkit.demo.designsystem.components.button.TonButtonConfig
import io.ton.walletkit.demo.designsystem.components.navbarbutton.TonBackButton
import io.ton.walletkit.demo.designsystem.components.seedphrase.SeedWordField
import io.ton.walletkit.demo.designsystem.components.segmentedcontrol.TonSegmentedControl
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.util.TestTags

@Composable
fun ImportWalletScreen(
    wordCount: Int,
    words: Map<Int, String>,
    canContinue: Boolean,
    onWordCountChange: (Int) -> Unit,
    onWordChange: (index: Int, value: String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowCount = wordCount / 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TonTheme.colors.bgPrimary)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            TonBackButton(onClick = onBack)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            TonText(
                text = "Import a wallet",
                style = TonTheme.typography.title2,
                color = TonTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TonText(
                text = "Enter your secret recovery phrase",
                style = TonTheme.typography.body,
                color = TonTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            TonSegmentedControl(
                selection = wordCount,
                items = listOf(12, 24),
                title = { it.toString() },
                onSelect = onWordCountChange,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            ImportWordGrid(
                wordCount = wordCount,
                rowCount = rowCount,
                words = words,
                onWordChange = onWordChange,
            )
        }

        TonButton(
            text = "Continue",
            onClick = onContinue,
            config = TonButtonConfig.Primary,
            enabled = canContinue,
            modifier = Modifier.testTag(TestTags.IMPORT_WALLET_CONTINUE_BUTTON),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ImportWordGrid(
    wordCount: Int,
    rowCount: Int,
    words: Map<Int, String>,
    onWordChange: (index: Int, value: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rowCount) { rowIndex ->
            val leftIdx = rowIndex
            val rightIdx = rowIndex + rowCount
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val leftFieldModifier = if (leftIdx == 0) {
                    Modifier.testTag(TestTags.IMPORT_WALLET_WORD_FIELD)
                } else {
                    Modifier
                }
                SeedWordField(
                    index = leftIdx + 1,
                    value = words[leftIdx].orEmpty(),
                    onValueChange = { onWordChange(leftIdx, it) },
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                    fieldModifier = leftFieldModifier,
                )
                if (rightIdx < wordCount) {
                    SeedWordField(
                        index = rightIdx + 1,
                        value = words[rightIdx].orEmpty(),
                        onValueChange = { onWordChange(rightIdx, it) },
                        imeAction = if (rightIdx == wordCount - 1) ImeAction.Done else ImeAction.Next,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportWalletScreenPreview() {
    TonTheme {
        ImportWalletScreen(
            wordCount = 12,
            words = mapOf(0 to "cactus"),
            canContinue = false,
            onWordCountChange = {},
            onWordChange = { _, _ -> },
            onBack = {},
            onContinue = {},
        )
    }
}
