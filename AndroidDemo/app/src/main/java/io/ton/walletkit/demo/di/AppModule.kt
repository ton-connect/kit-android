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
package io.ton.walletkit.demo.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ton.walletkit.demo.core.WalletKitDemoApp
import io.ton.walletkit.demo.data.storage.DemoAppStorage
import io.ton.walletkit.demo.data.storage.SecureDemoAppStorage
import io.ton.walletkit.event.TONWalletKitEvent
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Singleton

/**
 * Hilt module providing app-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplication(
        @ApplicationContext context: Context,
    ): Application = context.applicationContext as Application

    @Provides
    @Singleton
    fun provideWalletKitDemoApp(
        @ApplicationContext context: Context,
    ): WalletKitDemoApp = context.applicationContext as WalletKitDemoApp

    @Provides
    @Singleton
    fun provideDemoAppStorage(
        @ApplicationContext context: Context,
    ): DemoAppStorage = SecureDemoAppStorage(context)

    @Provides
    @Singleton
    fun provideSdkEvents(app: WalletKitDemoApp): @JvmSuppressWildcards SharedFlow<TONWalletKitEvent> = app.sdkEvents

    @Provides
    @Singleton
    fun provideSdkInitialized(app: WalletKitDemoApp): @JvmSuppressWildcards SharedFlow<Boolean> = app.sdkInitialized
}
