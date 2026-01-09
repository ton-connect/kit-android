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
package io.ton.walletkit.demo.e2e.dapp

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridge class for evaluating JavaScript in a WebView from instrumented tests.
 *
 * This allows E2E tests to interact with web content using JavaScript evaluation,
 * similar to how Playwright works in web E2E tests.
 *
 * Example usage:
 * ```kotlin
 * val bridge = WebViewJsBridge()
 *
 * // Click a button by CSS selector
 * bridge.evaluateJs("document.querySelector('[data-tc-button]').click()")
 *
 * // Get element text
 * val text = bridge.evaluateJs("document.querySelector('#result').innerText")
 *
 * // Check if element exists
 * val exists = bridge.evaluateJs("document.querySelector('.modal') !== null")
 * ```
 */
class WebViewJsBridge {

    companion object {
        private const val TAG = "WebViewJsBridge"
        private const val DEFAULT_TIMEOUT_MS = 10000L
    }

    private var cachedWebView: WebView? = null

    /**
     * Find the WebView in the current activity's view hierarchy.
     * Caches the result for performance.
     */
    fun findWebView(): WebView? {
        // Return cached if available and still attached
        cachedWebView?.let { webView ->
            if (webView.isAttachedToWindow) {
                return webView
            }
        }

        android.util.Log.d(TAG, "Looking for WebView...")

        // Try to find WebView using multiple strategies
        var webView: WebView? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Strategy 1: Get from current activity
            val activity = getCurrentActivityInternal()
            if (activity != null) {
                val rootView = activity.window?.decorView?.rootView
                webView = rootView?.let { findWebViewInHierarchy(it) }
                if (webView != null) {
                    android.util.Log.d(TAG, "Found WebView via activity root view")
                }
            }

            // Strategy 2: Get from window manager (for dialogs/sheets)
            if (webView == null) {
                try {
                    val windowManager = Class.forName("android.view.WindowManagerGlobal")
                    val getInstanceMethod = windowManager.getMethod("getInstance")
                    val instance = getInstanceMethod.invoke(null)

                    val getViewsMethod = windowManager.getMethod("getViewRootNames")
                    val views = getViewsMethod.invoke(instance) as? Array<*>
                    android.util.Log.d(TAG, "Found ${views?.size ?: 0} windows")

                    val getRootViewMethod = windowManager.getDeclaredMethod("getRootView", String::class.java)
                    getRootViewMethod.isAccessible = true

                    views?.forEach { viewName ->
                        val rootView = getRootViewMethod.invoke(instance, viewName) as? View
                        if (rootView != null) {
                            val foundWebView = findWebViewInHierarchy(rootView)
                            if (foundWebView != null) {
                                webView = foundWebView
                                android.util.Log.d(TAG, "Found WebView in window: $viewName")
                                return@forEach
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Failed to search windows: ${e.message}")
                }
            }

            // Strategy 3: Use UiAutomation to find WebView
            if (webView == null) {
                try {
                    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
                    val windows = uiAutomation.windows
                    android.util.Log.d(TAG, "UiAutomation found ${windows.size} windows")

                    for (window in windows) {
                        try {
                            val rootNode = window.root
                            if (rootNode != null) {
                                android.util.Log.d(TAG, "  Window: ${rootNode.className}, pkg=${rootNode.packageName}")
                            }
                        } catch (e: Exception) {
                            // Skip
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "UiAutomation search failed: ${e.message}")
                }
            }
        }

        if (webView != null) {
            cachedWebView = webView
        } else {
            android.util.Log.w(TAG, "No WebView found!")
        }

        return webView
    }

    /**
     * Get the current activity from ActivityLifecycleMonitorRegistry (internal version for main thread).
     */
    private fun getCurrentActivityInternal(): Activity? {
        val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED)
        return resumedActivities.firstOrNull()
    }

    /**
     * Get the current resumed activity from ActivityLifecycleMonitorRegistry.
     */
    private fun getCurrentActivity(): Activity? {
        var currentActivity: Activity? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currentActivity = getCurrentActivityInternal()
        }

        return currentActivity
    }

    /**
     * Recursively find a WebView in the view hierarchy.
     */
    private fun findWebViewInHierarchy(view: View, depth: Int = 0): WebView? {
        val indent = "  ".repeat(depth)

        if (depth < 5) { // Only log first few levels to avoid spam
            android.util.Log.d(TAG, "$indent- ${view.javaClass.simpleName} (${view.width}x${view.height})")
        }

        if (view is WebView) {
            android.util.Log.d(TAG, "$indent  >>> FOUND WebView!")
            return view
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val webView = findWebViewInHierarchy(child, depth + 1)
                if (webView != null) {
                    return webView
                }
            }
        }

        return null
    }

    /**
     * Clear the cached WebView reference.
     * Call this when the WebView is destroyed or the browser is closed.
     */
    fun clearCache() {
        cachedWebView = null
    }

    /**
     * Wait for the WebView to be available.
     * @param timeoutMs Maximum time to wait
     * @param pollIntervalMs How often to check
     * @return The WebView if found, null if timeout
     */
    fun waitForWebView(timeoutMs: Long = 10000, pollIntervalMs: Long = 500): WebView? {
        android.util.Log.d(TAG, "Waiting for WebView to appear...")
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val webView = findWebView()
            if (webView != null) {
                android.util.Log.d(TAG, "WebView found after ${System.currentTimeMillis() - startTime}ms")
                return webView
            }
            Thread.sleep(pollIntervalMs)
        }

        android.util.Log.e(TAG, "WebView not found within ${timeoutMs}ms")
        return null
    }

    /**
     * Evaluate JavaScript in the WebView and return the result.
     *
     * This is a synchronous call that blocks until the JavaScript execution completes.
     *
     * @param script The JavaScript code to execute
     * @param timeoutMs Maximum time to wait for the result
     * @return The result of the JavaScript evaluation as a string, or null if failed
     */
    fun evaluateJs(script: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): String? {
        val webView = findWebView()
        if (webView == null) {
            android.util.Log.e(TAG, "WebView not found!")
            return null
        }

        val resultHolder = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)

        // JavaScript must be evaluated on the UI thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            webView.evaluateJavascript(
                script,
                ValueCallback { result ->
                    resultHolder.set(result)
                    latch.countDown()
                },
            )
        }

        val completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS)

        if (!completed) {
            android.util.Log.w(TAG, "JavaScript evaluation timed out: ${script.take(100)}...")
            return null
        }

        val result = resultHolder.get()
        android.util.Log.d(TAG, "JS result: $result (for: ${script.take(50)}...)")
        return result
    }

    /**
     * Evaluate JavaScript and wait for a specific condition to become true.
     *
     * @param condition JavaScript expression that returns a boolean
     * @param timeoutMs Maximum time to wait
     * @param pollIntervalMs How often to check the condition
     * @return true if condition became true within timeout, false otherwise
     */
    fun waitForCondition(
        condition: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        pollIntervalMs: Long = 200,
    ): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val result = evaluateJs(condition, timeoutMs = 1000)
            if (result == "true") {
                return true
            }
            Thread.sleep(pollIntervalMs)
        }

        android.util.Log.w(TAG, "Condition did not become true within ${timeoutMs}ms: $condition")
        return false
    }

    /**
     * Click an element identified by a CSS selector.
     *
     * @param selector CSS selector for the element
     * @return true if click was successful (element found), false otherwise
     */
    fun clickElement(selector: String): Boolean {
        val script = """
            (function() {
                var element = document.querySelector('$selector');
                if (element) {
                    element.click();
                    return true;
                }
                return false;
            })()
        """.trimIndent()

        val result = evaluateJs(script)
        return result == "true"
    }

    /**
     * Wait for an element to appear and then click it.
     *
     * @param selector CSS selector for the element
     * @param timeoutMs Maximum time to wait for element to appear
     * @return true if element appeared and was clicked, false otherwise
     */
    fun waitAndClickElement(selector: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val appeared = waitForCondition(
            "document.querySelector('$selector') !== null",
            timeoutMs = timeoutMs,
        )

        if (!appeared) {
            android.util.Log.w(TAG, "Element not found: $selector")
            return false
        }

        return clickElement(selector)
    }

    /**
     * Get the inner text of an element.
     *
     * @param selector CSS selector for the element
     * @return The text content, or null if element not found
     */
    fun getElementText(selector: String): String? {
        // Escape single quotes in selector for JavaScript string interpolation
        val escapedSelector = selector.replace("'", "\\'")
        val script = """
            (function() {
                var element = document.querySelector('$escapedSelector');
                return element ? element.innerText : null;
            })()
        """.trimIndent()

        val result = evaluateJs(script)
        // Remove quotes from JSON string result
        return result?.trim('"')?.let {
            if (it == "null") null else it
        }
    }

    /**
     * Wait for an element to contain specific text.
     *
     * @param selector CSS selector for the element
     * @param text Expected text (substring match)
     * @param timeoutMs Maximum time to wait
     * @return true if text appeared within timeout, false otherwise
     */
    fun waitForElementText(
        selector: String,
        text: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): Boolean {
        // Escape single quotes in selector and text for JavaScript string interpolation
        val escapedSelector = selector.replace("'", "\\'")
        val escapedText = text.replace("'", "\\'")
        val condition = """
            (function() {
                var element = document.querySelector('$escapedSelector');
                return element && element.innerText.includes('$escapedText');
            })()
        """.trimIndent()

        return waitForCondition(condition, timeoutMs)
    }

    /**
     * Check if an element exists.
     *
     * @param selector CSS selector for the element
     * @return true if element exists, false otherwise
     */
    fun elementExists(selector: String): Boolean {
        val escapedSelector = selector.replace("'", "\\'")
        val result = evaluateJs("document.querySelector('$escapedSelector') !== null")
        return result == "true"
    }

    /**
     * Wait for an element to appear.
     *
     * @param selector CSS selector for the element
     * @param timeoutMs Maximum time to wait
     * @return true if element appeared, false otherwise
     */
    fun waitForElement(selector: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val escapedSelector = selector.replace("'", "\\'")
        return waitForCondition(
            "document.querySelector('$escapedSelector') !== null",
            timeoutMs = timeoutMs,
        )
    }

    /**
     * Scroll an element into view.
     *
     * @param selector CSS selector for the element
     * @return true if element was found and scrolled into view
     */
    fun scrollIntoView(selector: String): Boolean {
        val script = """
            (function() {
                var element = document.querySelector('$selector');
                if (element) {
                    element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    return true;
                }
                return false;
            })()
        """.trimIndent()

        val result = evaluateJs(script)
        return result == "true"
    }

    /**
     * Fill a text input field.
     *
     * @param selector CSS selector for the input element
     * @param value The text to fill
     * @return true if successful
     */
    fun fillInput(selector: String, value: String): Boolean {
        val escapedSelector = selector.replace("'", "\\'")
        // Escape special characters for JavaScript string (using backticks, so escape backticks and ${)
        val escapedValue = value
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\${", "\\\${")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\t", "\\t")

        // This script works with React by using the native value setter trick
        // React overrides the value property, so we need to use the native setter
        // to properly trigger React's state update
        // Using backticks (template literals) to avoid escaping double quotes in JSON
        val script = """
            (function() {
                var element = document.querySelector('$escapedSelector');
                if (!element) {
                    console.error('Element not found: $escapedSelector');
                    return 'element_not_found';
                }
                
                var newValue = `$escapedValue`;
                
                // Focus the element first
                element.focus();
                
                // Get the native value setter (React overrides this)
                var nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                    element.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype,
                    'value'
                ).set;
                
                // Use native setter to set value
                nativeInputValueSetter.call(element, newValue);
                
                // Dispatch input event - React listens for this
                var inputEvent = new Event('input', { bubbles: true, cancelable: true });
                element.dispatchEvent(inputEvent);
                
                // Also dispatch change event
                var changeEvent = new Event('change', { bubbles: true, cancelable: true });
                element.dispatchEvent(changeEvent);
                
                // Blur to ensure any onBlur handlers fire
                element.blur();
                
                console.log('fillInput: Set value for $escapedSelector, new length: ' + element.value.length);
                return 'success:' + element.value.length;
            })()
        """.trimIndent()

        android.util.Log.d(TAG, "fillInput selector: $escapedSelector, value length: ${value.length}")
        val result = evaluateJs(script)
        android.util.Log.d(TAG, "fillInput result: $result")
        return result?.startsWith("success") == true
    }

    /**
     * Get the value of an input/textarea element.
     *
     * @param selector CSS selector for the input element
     * @return The value, or null if element not found
     */
    fun getInputValue(selector: String): String? {
        val escapedSelector = selector.replace("'", "\\'")
        val script = """
            (function() {
                var element = document.querySelector('$escapedSelector');
                return element ? element.value : null;
            })()
        """.trimIndent()

        val result = evaluateJs(script)
        return if (result == "null" || result.isNullOrEmpty()) null else result
    }

    /**
     * Read clipboard content via JavaScript.
     * Note: This requires clipboard permission and may not work in all contexts.
     * For E2E tests, it's often easier to use the Android ClipboardManager directly.
     */
    fun readClipboard(): String? {
        // Note: navigator.clipboard.readText() is async and requires user gesture
        // For testing, we'll need to use a different approach
        android.util.Log.w(TAG, "JavaScript clipboard read not implemented - use Android ClipboardManager instead")
        return null
    }

    /**
     * Log all elements matching a selector for debugging.
     */
    fun debugLogElements(selector: String) {
        val script = """
            (function() {
                var elements = document.querySelectorAll('$selector');
                var results = [];
                elements.forEach(function(el, index) {
                    results.push({
                        index: index,
                        tagName: el.tagName,
                        text: el.innerText.substring(0, 50),
                        className: el.className,
                        id: el.id
                    });
                });
                return JSON.stringify(results);
            })()
        """.trimIndent()

        val result = evaluateJs(script)
        android.util.Log.d(TAG, "Elements matching '$selector': $result")
    }
}
