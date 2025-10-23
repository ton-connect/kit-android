package io.ton.walletkit.presentation.browser.internal

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ton.walletkit.domain.constants.JsonConstants
import org.json.JSONObject

/**
 * WebViewClient that handles page loading events and injects the TonConnect bridge.
 */
internal class TonConnectWebViewClient(
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val injectBridge: (webView: WebView?) -> Unit,
    private val onTonConnectRequest: ((method: String, params: JSONObject) -> Unit)? = null,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false

        Log.d(TAG, "shouldOverrideUrlLoading: $url")

        // Intercept tc:// deep links and convert to bridge request
        if (url.startsWith(SCHEME_TC) || url.startsWith(SCHEME_TONKEEPER) || url.startsWith(SCHEME_TONKEEPER_SHORT)) {
            Log.d(TAG, "Intercepted TonConnect deep link: $url")
            parseTonConnectDeepLink(url)
            return true // Prevent navigation
        }

        return super.shouldOverrideUrlLoading(view, request)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false

        Log.d(TAG, "shouldOverrideUrlLoading (deprecated): $url")

        // Intercept tc:// deep links and convert to bridge request
        if (url.startsWith(SCHEME_TC) || url.startsWith(SCHEME_TONKEEPER) || url.startsWith(SCHEME_TONKEEPER_SHORT)) {
            Log.d(TAG, "Intercepted TonConnect deep link (deprecated): $url")
            parseTonConnectDeepLink(url)
            return true // Prevent navigation
        }

        return super.shouldOverrideUrlLoading(view, url)
    }

    private fun parseTonConnectDeepLink(url: String) {
        try {
            Log.d(TAG, "Intercepted TonConnect deep link: $url")

            // Instead of parsing and reconstructing, pass the full URL to the SDK
            // The URL contains all necessary info including session keys
            val params = JSONObject().apply {
                put(JsonConstants.KEY_URL, url)
            }

            // Forward the complete URL to be handled by handleTonConnectUrl
            onTonConnectRequest?.invoke(METHOD_HANDLE_URL, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse TonConnect deep link", e)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        Log.d(TAG, "onPageStarted: $url")

        // Intercept tc:// URLs even in onPageStarted
        if (url != null && (url.startsWith(SCHEME_TC) || url.startsWith(SCHEME_TONKEEPER) || url.startsWith(SCHEME_TONKEEPER_SHORT))) {
            Log.d(TAG, "Intercepting TonConnect URL in onPageStarted: $url")
            parseTonConnectDeepLink(url)
            view?.stopLoading() // Stop the navigation
            return // Don't call super or onPageStarted callback
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

        // URL schemes for TonConnect deep links
        private const val SCHEME_TC = "tc://"
        private const val SCHEME_TONKEEPER = "tonkeeper://"
        private const val SCHEME_TONKEEPER_SHORT = "tonkeeper:"

        // Bridge method names
        private const val METHOD_HANDLE_URL = "handleUrl"

        // Error messages
        private const val DEFAULT_ERROR_MESSAGE = "Unknown error"
    }
}
