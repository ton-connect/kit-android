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
package io.ton.walletkit.demo.presentation.state

// Mnemonic state is held only in memory — a process kill drops it and the user
// re-rolls, which is the right trade-off for unconfirmed seed material.
sealed interface CreateWalletFlow {
    data object Idle : CreateWalletFlow
    data object Onboarding : CreateWalletFlow
    data class Reveal(val words: List<String>) : CreateWalletFlow

    data class ImportEntry(
        val wordCount: Int = 24,
        val words: Map<Int, String> = emptyMap(),
    ) : CreateWalletFlow {
        val isComplete: Boolean
            get() = (0 until wordCount).all { idx -> !words[idx].isNullOrBlank() }

        fun asPhrase(): List<String> = (0 until wordCount).map { idx ->
            words[idx].orEmpty().trim().lowercase()
        }
    }
}
