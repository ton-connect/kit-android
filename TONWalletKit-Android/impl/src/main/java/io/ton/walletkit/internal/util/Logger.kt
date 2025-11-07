package io.ton.walletkit.internal.util

import android.util.Log
import io.ton.walletkit.bridge.BuildConfig

/**
 * Internal logger for TON WalletKit SDK.
 * Logs are automatically disabled in release builds.
 */
internal object Logger {
    
    private val ENABLE_LOGGING = BuildConfig.ENABLE_LOGGING
    
    fun v(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.v(tag, message)
        }
    }
    
    fun d(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.d(tag, message)
        }
    }
    
    fun i(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.i(tag, message)
        }
    }
    
    fun w(tag: String, message: String) {
        if (ENABLE_LOGGING) {
            Log.w(tag, message)
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable) {
        if (ENABLE_LOGGING) {
            Log.w(tag, message, throwable)
        }
    }
    
    // Keep errors always - critical for debugging production issues
    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}
