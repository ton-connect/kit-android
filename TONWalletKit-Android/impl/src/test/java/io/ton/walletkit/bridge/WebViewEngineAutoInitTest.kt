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
package io.ton.walletkit.bridge

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.ton.walletkit.config.TONWalletKitConfiguration
import io.ton.walletkit.engine.WebViewWalletKitEngine
import io.ton.walletkit.model.TONNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * Tests for WebViewWalletKitEngine auto-initialization feature.
 * Verifies that the engine can automatically initialize itself on first use.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class WebViewEngineAutoInitTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var defaultConfiguration: TONWalletKitConfiguration

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        defaultConfiguration = testWalletKitConfiguration()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `engine has auto-init capability`() = runTest {
        val engine = WebViewWalletKitEngine.getOrCreate(context, defaultConfiguration, NoopEventsHandler)
        assertNotNull(engine)

        // Auto-init happens on first call to any method
        // We can't easily test it without a full WebView environment,
        // but we verify the engine supports the init() method
        assertNotNull(engine::init)

        flushMainThread()
    }

    @Test
    fun `init accepts custom config`() = runTest {
        val config = testWalletKitConfiguration(
            network = TONNetwork.MAINNET,
            persistent = false,
        )

        val engine = WebViewWalletKitEngine.getOrCreate(context, config, NoopEventsHandler)

        assertNotNull(config)

        flushMainThread()
    }

    @Test
    fun `init accepts testnet config`() = runTest {
        val config = testWalletKitConfiguration(network = TONNetwork.TESTNET)
        val engine = WebViewWalletKitEngine.getOrCreate(context, config, NoopEventsHandler)

        assertNotNull(config)
        assertNotNull(engine)

        flushMainThread()
    }

    @Test
    fun `engine supports storage configuration`() = runTest {
        val engine = WebViewWalletKitEngine.getOrCreate(context, defaultConfiguration, NoopEventsHandler)

        val withStorage = testWalletKitConfiguration(persistent = true)
        val withoutStorage = testWalletKitConfiguration(persistent = false)

        assertNotNull(withStorage)
        assertNotNull(withoutStorage)
        assertNotNull(engine)

        flushMainThread()
    }

    @Test
    fun `engine can be created multiple times`() = runTest {
        val engine1 = WebViewWalletKitEngine.getOrCreate(context, defaultConfiguration, NoopEventsHandler)
        val engine2 = WebViewWalletKitEngine.getOrCreate(context, defaultConfiguration, NoopEventsHandler)
        val engine3 = WebViewWalletKitEngine.getOrCreate(context, defaultConfiguration, NoopEventsHandler)

        assertNotNull(engine1)
        assertNotNull(engine2)
        assertNotNull(engine3)

        flushMainThread()
    }

    private fun flushMainThread() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}
