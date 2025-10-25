package io.ton.walletkit.presentation.browser.internal

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebViewClient that handles page loading events and injects the TonConnect bridge.
 */
internal class TonConnectWebViewClient(
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val injectBridge: (webView: WebView?) -> Unit,
    private val onTonConnectUrl: ((url: String) -> Unit)? = null,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false

        Log.d(TAG, "shouldOverrideUrlLoading: $url")

        // Intercept tc://, tonkeeper:// deep links AND https://app.tonkeeper.com/ton-connect URLs
        if (url.startsWith(SCHEME_TC) ||
            url.startsWith(SCHEME_TONKEEPER) ||
            url.startsWith(SCHEME_TONKEEPER_SHORT) ||
            url.startsWith("https://app.tonkeeper.com/ton-connect") ||
            url.startsWith("https://tonkeeper.com/ton-connect")
        ) {
            Log.d(TAG, "Intercepted TonConnect URL (preventing navigation, dApp should use injected bridge): $url")
            onTonConnectUrl?.invoke(url)
            return true // Prevent navigation - dApp should use injected bridge (embedded: true)
        }

        return super.shouldOverrideUrlLoading(view, request)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false

        Log.d(TAG, "shouldOverrideUrlLoading (deprecated): $url")

        // Intercept tc://, tonkeeper:// deep links AND https://app.tonkeeper.com/ton-connect URLs
        if (url.startsWith(SCHEME_TC) ||
            url.startsWith(SCHEME_TONKEEPER) ||
            url.startsWith(SCHEME_TONKEEPER_SHORT) ||
            url.startsWith("https://app.tonkeeper.com/ton-connect") ||
            url.startsWith("https://tonkeeper.com/ton-connect")
        ) {
            Log.d(TAG, "Intercepted TonConnect URL (deprecated, preventing navigation, dApp should use injected bridge): $url")
            onTonConnectUrl?.invoke(url)
            return true // Prevent navigation - dApp should use injected bridge (embedded: true)
        }

        return super.shouldOverrideUrlLoading(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        Log.d(TAG, "onPageStarted: $url")

        // Also intercept in onPageStarted as a safety net
        if (url != null && (
                url.startsWith(SCHEME_TC) ||
                    url.startsWith(SCHEME_TONKEEPER) ||
                    url.startsWith(SCHEME_TONKEEPER_SHORT) ||
                    url.startsWith("https://app.tonkeeper.com/ton-connect") ||
                    url.startsWith("https://tonkeeper.com/ton-connect")
                )
        ) {
            Log.d(TAG, "Intercepted TonConnect URL in onPageStarted (stopping load, dApp should use injected bridge): $url")
            onTonConnectUrl?.invoke(url)
            view?.stopLoading()
            return // Don't call callbacks
        }

        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        // Inject TonConnect bridge into main frame and all iframes
        injectBridge(view)

        url?.let { onPageFinished(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: android.webkit.WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        val errorMessage = error?.description?.toString() ?: DEFAULT_ERROR_MESSAGE
        onError(errorMessage)
    }

    companion object {
        private const val TAG = "TonConnectWebViewClient"
        private const val DEFAULT_ERROR_MESSAGE = "Unknown error"

        // URL schemes for TonConnect deep links
        private const val SCHEME_TC = "tc://"
        private const val SCHEME_TONKEEPER = "tonkeeper://"
        private const val SCHEME_TONKEEPER_SHORT = "tonkeeper:"
    }
}
