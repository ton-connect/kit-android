package io.ton.walletkit.quickjs

import java.util.concurrent.atomic.AtomicBoolean

internal object QuickJsNativeLoader {
    private val loaded = AtomicBoolean(false)

    fun load() {
        if (loaded.compareAndSet(false, true)) {
            System.loadLibrary("walletkitquickjs")
        }
    }
}
