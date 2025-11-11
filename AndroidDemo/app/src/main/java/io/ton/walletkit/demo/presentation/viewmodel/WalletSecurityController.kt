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

import io.ton.walletkit.demo.data.storage.DemoAppStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles password and unlock state, providing reactive signals for the UI layer.
 */
class WalletSecurityController(
    private val storage: DemoAppStorage,
) {

    private val _isPasswordSet = MutableStateFlow(storage.isPasswordSet())
    val isPasswordSet: StateFlow<Boolean> = _isPasswordSet.asStateFlow()

    private val _isUnlocked = MutableStateFlow(storage.isUnlocked())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun setPassword(password: String) {
        storage.setPassword(password)
        storage.setUnlocked(true)
        _isPasswordSet.value = true
        _isUnlocked.value = true
    }

    fun verifyPassword(password: String): Boolean {
        val verified = storage.verifyPassword(password)
        if (verified) {
            storage.setUnlocked(true)
            _isUnlocked.value = true
        }
        return verified
    }

    fun lock() {
        _isUnlocked.value = false
        storage.setUnlocked(false)
    }

    fun reset() {
        _isPasswordSet.value = false
        _isUnlocked.value = false
    }
}
