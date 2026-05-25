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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.button.TonButton
import io.ton.walletkit.demo.designsystem.components.button.TonButtonConfig
import io.ton.walletkit.demo.designsystem.components.navbarbutton.TonBackButton
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.theme.TonTheme

@Composable
fun RevealSeedPhraseScreen(
    words: List<String>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rowCount = (words.size + 1) / 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TonTheme.colors.bgPrimary)
            .statusBarsPadding()
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
                text = "Recovery phrase",
                style = TonTheme.typography.title3Bold,
                color = TonTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TonText(
                text = "This is the only way you will be able to recover your account. Please store it somewhere safe!",
                style = TonTheme.typography.body,
                color = TonTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            SeedPhraseDisplay(words = words, rowCount = rowCount)

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TonButton(
                    text = "Copy phrase",
                    onClick = { copyPhrase(context, words) },
                    config = TonButtonConfig.Tertiary.small(),
                    stretch = false,
                )
            }
        }

        TonButton(
            text = "Continue",
            onClick = onContinue,
            config = TonButtonConfig.Primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SeedPhraseDisplay(words: List<String>, rowCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SeedPhraseColumn(
            words = words,
            indices = (0 until rowCount).toList(),
            modifier = Modifier.weight(1f),
        )
        SeedPhraseColumn(
            words = words,
            indices = (rowCount until words.size).toList(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SeedPhraseColumn(
    words: List<String>,
    indices: List<Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        indices.forEach { idx ->
            SeedWordRow(index = idx + 1, word = words[idx])
        }
    }
}

@Composable
private fun SeedWordRow(index: Int, word: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // Figma models the word in a chip with `gap 8 + chip-left-padding 12` = 20dp
        // from the index. The chip itself is white-on-white (invisible), but the
        // padding still shifts the word.
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TonText(
            text = index.toString(),
            style = TonTheme.typography.body,
            color = TonTheme.colors.textTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(20.dp),
        )
        TonText(
            text = word,
            style = TonTheme.typography.bodySemibold,
            color = TonTheme.colors.textPrimary,
        )
    }
}

private fun copyPhrase(context: Context, words: List<String>) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Recovery phrase", words.joinToString(" ")))
    Toast.makeText(context, "Phrase copied", Toast.LENGTH_SHORT).show()
}

@Preview(showBackground = true)
@Composable
private fun RevealSeedPhraseScreenPreview() {
    TonTheme {
        RevealSeedPhraseScreen(
            words = listOf(
                "cactus", "velvet", "echo", "pigeon", "compass", "glacier",
                "echo", "hiss", "sigh", "shout", "yell", "clamor",
                "orbit", "lantern", "quartz", "drizzle", "apricot", "whisper",
                "murmur", "buzz", "breath", "call", "roar", "peep",
            ),
            onBack = {},
            onContinue = {},
        )
    }
}
