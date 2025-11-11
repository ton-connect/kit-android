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
package io.ton.walletkit.internal

import android.content.Context
import io.ton.walletkit.ITONWalletKit
import io.ton.walletkit.config.TONWalletKitConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Internal factory for creating ITONWalletKit instances.
 * Uses reflection to load the implementation without compile-time dependency.
 *
 * @hide This is internal implementation detail and should not be used directly.
 */
@PublishedApi
internal object TONWalletKitFactory {
    @Suppress("UNCHECKED_CAST")
    suspend fun create(
        context: Context,
        config: TONWalletKitConfiguration,
    ): ITONWalletKit {
        // Load the implementation class via reflection to avoid compile-time dependency
        val implClass = Class.forName("io.ton.walletkit.core.TONWalletKit")

        // Get the Companion object
        val companionField = implClass.getDeclaredField("Companion")
        companionField.isAccessible = true
        val companion = companionField.get(null)

        // Get the initialize method from the Companion class
        val companionClass = companion.javaClass
        val method = companionClass.getDeclaredMethod(
            "initialize",
            Context::class.java,
            TONWalletKitConfiguration::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        method.isAccessible = true

        // Call as suspend function
        return suspendCancellableCoroutine { continuation ->
            try {
                method.invoke(companion, context, config, continuation)
            } catch (e: Exception) {
                continuation.resumeWith(Result.failure(e))
            }
        }
    }
}
