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
package io.ton.walletkit.demo.presentation.dev

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Hidden developer toggles. Persisted in plain SharedPreferences (no secrets) and
// exposed as a StateFlow so Compose can re-render when the user flips them via the
// secret tap-counter on the main screen.
object DevPreferences {
    private const val PREFS_NAME = "dev_preferences"
    private const val KEY_USE_LEGACY_MAIN_SCREEN = "use_legacy_main_screen"

    private val _useLegacyMainScreen = MutableStateFlow(false)
    val useLegacyMainScreen: StateFlow<Boolean> = _useLegacyMainScreen.asStateFlow()

    @Volatile
    private var loaded = false

    // Pulls the current value off disk on first call. Subsequent calls are cheap.
    // Safe to call from MainActivity.onCreate before the first composition.
    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _useLegacyMainScreen.value = prefs.getBoolean(KEY_USE_LEGACY_MAIN_SCREEN, false)
            loaded = true
        }
    }

    // Flip the flag and persist. Returns the new value so callers can render
    // confirmation UI (e.g. Toast).
    fun toggleLegacyMainScreen(context: Context): Boolean {
        ensureLoaded(context)
        val next = !_useLegacyMainScreen.value
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_LEGACY_MAIN_SCREEN, next)
            .apply()
        _useLegacyMainScreen.value = next
        return next
    }

    /**
     * Wipe both the persisted file and the cached in-process state. Used by the e2e test
     * harness so a test class can't inherit a legacy-toggle flip from a previous class —
     * otherwise [useLegacyMainScreen] would stay `true` even after the prefs file is
     * deleted, because this is a process-wide singleton whose [StateFlow] outlives the
     * activity recreate between tests.
     */
    fun reset(context: Context) {
        synchronized(this) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            _useLegacyMainScreen.value = false
            loaded = false
        }
    }
}
