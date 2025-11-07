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
package io.ton.walletkit.demo.presentation.viewmodel

import androidx.annotation.StringRes
import io.ton.walletkit.demo.presentation.state.WalletUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Maintains the rolling event log and temporary status messages.
 */
class WalletEventLogger(
    private val state: MutableStateFlow<WalletUiState>,
    private val scope: CoroutineScope,
    private val maxEvents: Int,
    private val hideDelayMillis: Long,
    private val defaultStatusProvider: () -> String,
    private val stringProvider: (Int, Array<out Any>) -> String,
) {

    fun log(@StringRes resId: Int, vararg args: Any) {
        log(stringProvider(resId, args))
    }

    fun log(message: String) {
        state.update {
            val events = listOf(message) + it.events
            it.copy(events = events.take(maxEvents))
        }
    }

    fun showTemporaryStatus(message: String) {
        state.update { it.copy(status = message, error = null) }
        scope.launch {
            delay(hideDelayMillis)
            state.update { current ->
                if (current.status == message) {
                    current.copy(status = defaultStatusProvider())
                } else {
                    current
                }
            }
        }
    }
}
