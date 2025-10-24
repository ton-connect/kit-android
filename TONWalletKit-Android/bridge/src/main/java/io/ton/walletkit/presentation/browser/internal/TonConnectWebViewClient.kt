package io.ton.walletkit.presentation.browser.internal

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ton.walletkit.domain.constants.BrowserConstants
import io.ton.walletkit.domain.constants.MiscConstants
import java.io.ByteArrayInputStream

/**
 * WebViewClient that handles page loading events and injects the TonConnect bridge.
 */
internal class TonConnectWebViewClient(
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val injectBridge: (webView: WebView?) -> Unit,
    private val onTonConnectUrl: ((url: String) -> Unit)? = null,
    // Function to get the injection script
    private val getInjectionScript: () -> String,
) : WebViewClient() {

    // Track the original URL before HTML interception changes it to appassets.androidplatform.net
    private var originalUrl: String? = null

    private fun isTonConnectUrl(url: String): Boolean =
        url.startsWith(SCHEME_TC) ||
            url.startsWith(SCHEME_TONKEEPER) ||
            url.startsWith(SCHEME_TONKEEPER_SHORT) ||
            url.startsWith(BrowserConstants.URL_TONKEEPER_APP_TON_CONNECT) ||
            url.startsWith(BrowserConstants.URL_TONKEEPER_TON_CONNECT)

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false

        Log.d(TAG, "shouldOverrideUrlLoading: $url")

        // Intercept tc://, tonkeeper:// deep links AND https://app.tonkeeper.com/ton-connect URLs
        if (isTonConnectUrl(url)) {
            Log.d(TAG, "Intercepted TonConnect URL (preventing navigation, dApp should use injected bridge): $url")
            onTonConnectUrl?.invoke(url)
            return true // Prevent navigation - dApp should use injected bridge (embedded: true)
        }

        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString()

        // Store the original URL before interception
        if (url != null) {
            originalUrl = url
        }

        // Intercept HTML documents to inject bridge script before any JavaScript runs
        // This is CRITICAL for iframe support - we need to inject BEFORE dApp's JS executes
        if (url != null && request.method == BrowserConstants.HTTP_METHOD_GET) {
            val acceptHeader = request.requestHeaders?.get(BrowserConstants.HEADER_ACCEPT) ?: MiscConstants.EMPTY_STRING

            // Only intercept HTML documents, not scripts, images, etc.
            if (acceptHeader.contains(BrowserConstants.MIME_TYPE_HTML) ||
                url.endsWith(BrowserConstants.HTML_EXTENSION) ||
                url.endsWith(BrowserConstants.ROOT_PATH_SUFFIX)
            ) {
                try {
                    Log.d(TAG, "Intercepting HTML request to inject bridge: $url")

                    // Fetch the original HTML
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    request.requestHeaders?.forEach { (key, value) ->
                        connection.setRequestProperty(key, value)
                    }

                    if (connection.responseCode == 200) {
                        val originalHtml = connection.inputStream.bufferedReader().use { it.readText() }
                        val injectionScript = getInjectionScript()

                        // Inject our script at the very beginning of <head> or <html>
                        val injectionMarkup = BrowserConstants.HTML_SCRIPT_OPEN + injectionScript + BrowserConstants.HTML_SCRIPT_CLOSE
                        val modifiedHtml =
                            when {
                                originalHtml.contains(BrowserConstants.HTML_TAG_HEAD, ignoreCase = true) -> {
                                    originalHtml.replaceFirst(
                                        BrowserConstants.HTML_TAG_HEAD,
                                        BrowserConstants.HTML_TAG_HEAD + injectionMarkup,
                                        ignoreCase = true,
                                    )
                                }
                                originalHtml.contains(BrowserConstants.HTML_TAG_HTML, ignoreCase = true) -> {
                                    originalHtml.replaceFirst(
                                        BrowserConstants.HTML_TAG_HTML,
                                        BrowserConstants.HTML_TAG_HTML + injectionMarkup,
                                        ignoreCase = true,
                                    )
                                }
                                else -> {
                                    // No proper HTML structure, prepend script
                                    buildString {
                                        append(injectionMarkup)
                                        appendLine()
                                        append(originalHtml)
                                    }
                                }
                            }

                        Log.d(TAG, "Successfully injected bridge into HTML (${modifiedHtml.length} bytes)")

                        return WebResourceResponse(
                            BrowserConstants.MIME_TYPE_HTML,
                            BrowserConstants.CHARSET_UTF8,
                            ByteArrayInputStream(modifiedHtml.toByteArray(Charsets.UTF_8)),
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to inject bridge into HTML: ${e.message}", e)
                    // Fall through to default behavior
                }
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false

        Log.d(TAG, "shouldOverrideUrlLoading (deprecated): $url")

        // Intercept tc://, tonkeeper:// deep links AND https://app.tonkeeper.com/ton-connect URLs
        if (isTonConnectUrl(url)) {
            Log.d(TAG, "Intercepted TonConnect URL (deprecated, preventing navigation, dApp should use injected bridge): $url")
            onTonConnectUrl?.invoke(url)
            return true // Prevent navigation - dApp should use injected bridge (embedded: true)
        }

        return super.shouldOverrideUrlLoading(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        Log.d(TAG, "onPageStarted: $url")

        // Also intercept in onPageStarted as a safety net
        if (url != null && isTonConnectUrl(url)) {
            Log.d(TAG, "Intercepted TonConnect URL in onPageStarted (stopping load, dApp should use injected bridge): $url")
            onTonConnectUrl?.invoke(url)
            view?.stopLoading()
            return // Don't call callbacks
        }

        super.onPageStarted(view, url, favicon)

        // CRITICAL: Inject bridge BEFORE page JavaScript executes
        // This ensures iframe TonConnect SDKs detect our bridge instead of falling back to deep links
        injectBridge(view)

        // Use original URL (before HTML interception) if available, otherwise use the current URL
        val effectiveUrl = originalUrl ?: url
        effectiveUrl?.let {
            Log.d(TAG, "onPageStarted - reporting URL: $it (original: $originalUrl, current: $url)")
            onPageStarted(it)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        // Re-inject bridge after page finishes loading as a safety net
        // (in case any dynamic iframes were added after onPageStarted)
        injectBridge(view)

        // Use original URL (before HTML interception) if available, otherwise use the current URL
        val effectiveUrl = originalUrl ?: url
        effectiveUrl?.let {
            Log.d(TAG, "onPageFinished - reporting URL: $it (original: $originalUrl, current: $url)")
            onPageFinished(it)
        }
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
