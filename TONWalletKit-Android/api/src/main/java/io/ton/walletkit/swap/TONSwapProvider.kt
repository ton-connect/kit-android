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
package io.ton.walletkit.swap

import io.ton.walletkit.api.generated.TONDeDustProviderOptions
import io.ton.walletkit.api.generated.TONOmnistonProviderOptions

/**
 * Handle for a registered swap provider. The type parameter [TQuoteOptions] carries
 * the provider-specific options type used in [ITONSwapManager.getQuote].
 *
 * Created via [io.ton.walletkit.ITONWalletKit.omnistonSwapProvider] or
 * [io.ton.walletkit.ITONWalletKit.deDustSwapProvider].
 * Register with [ITONSwapManager.registerProvider] before calling [ITONSwapManager.getQuote].
 */
data class TONSwapProvider<TQuoteOptions>(val providerId: String)

/** Typed handle for the Omniston (STON.fi) swap provider. */
typealias TONOmnistonSwapProvider = TONSwapProvider<TONOmnistonProviderOptions>

/** Typed handle for the DeDust swap provider. */
typealias TONDeDustSwapProvider = TONSwapProvider<TONDeDustProviderOptions>
