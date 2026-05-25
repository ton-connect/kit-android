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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ton.walletkit.demo.designsystem.components.button.TonButton
import io.ton.walletkit.demo.designsystem.components.button.TonButtonConfig
import io.ton.walletkit.demo.designsystem.components.pagination.CompactPaginationDots
import io.ton.walletkit.demo.designsystem.components.text.TonText
import io.ton.walletkit.demo.designsystem.icons.TonIcon
import io.ton.walletkit.demo.designsystem.icons.TonIconImage
import io.ton.walletkit.demo.designsystem.theme.TonTheme
import io.ton.walletkit.demo.presentation.util.TestTags

// Icon40 vectors stand in for placeholder illustration art until final assets land.
private data class OnboardingPage(
    val icon: TonIcon,
    val title: String,
    val description: String,
)

private val DefaultPages = listOf(
    OnboardingPage(
        icon = TonIcon.Toncoin40,
        title = "Welcome to TON",
        description = "Hold and manage your Toncoin and tokens in one place.",
    ),
    OnboardingPage(
        icon = TonIcon.Hand40,
        title = "Self-custody",
        description = "You alone control your recovery phrase, keys, and funds.",
    ),
    OnboardingPage(
        icon = TonIcon.Reward40,
        title = "Built for dApps",
        description = "TON Connect-ready for staking, swaps, and games.",
    ),
)

@Composable
fun CreateWalletOnboardingScreen(
    onCreate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = DefaultPages
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TonTheme.colors.bgPrimary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        Spacer(modifier = Modifier.height(24.dp))

        CompactPaginationDots(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.weight(1f))

        TonButton(
            text = "Create a new wallet",
            onClick = onCreate,
            config = TonButtonConfig.Primary,
            modifier = Modifier.testTag(TestTags.ONBOARDING_CREATE_WALLET_BUTTON),
        )
        Spacer(modifier = Modifier.height(8.dp))
        TonButton(
            text = "Add an existing wallet",
            onClick = onImport,
            config = TonButtonConfig.Secondary,
            modifier = Modifier.testTag(TestTags.ONBOARDING_IMPORT_WALLET_BUTTON),
        )
        Spacer(modifier = Modifier.height(12.dp))
        TonText(
            text = "By continuing, you agree to the Terms and Privacy Policy",
            style = TonTheme.typography.footnote,
            color = TonTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TonIconImage(icon = page.icon, size = 128.dp)
        }
        TonText(
            text = page.title,
            style = TonTheme.typography.title1,
            color = TonTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TonText(
            text = page.description,
            style = TonTheme.typography.body,
            color = TonTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateWalletOnboardingScreenPreview() {
    TonTheme {
        CreateWalletOnboardingScreen(onCreate = {}, onImport = {})
    }
}
