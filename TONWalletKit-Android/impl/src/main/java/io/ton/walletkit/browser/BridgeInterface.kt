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
package io.ton.walletkit.browser

import android.webkit.JavascriptInterface
import io.ton.walletkit.internal.constants.BrowserConstants
import io.ton.walletkit.internal.util.Logger
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * JavaScript interface for bridge communication.
 * Messages from injected bridge come through this interface.
 *
 * Each message includes a frameId to identify which window/iframe sent it.
 *
 * This interface is automatically available in ALL frames (parent + iframes)
 * thanks to addJavascriptInterface() behavior.
 */
internal class BridgeInterface(
    private val onMessage: (message: JSONObject, type: String) -> Unit,
    private val onError: (error: String) -> Unit,
    private val onResponse: ((message: JSONObject) -> Unit)? = null,
) {
    // Thread-safe storage for responses that iframes can pull
    // Limit size to prevent unbounded growth if responses are never pulled
    private val availableResponses = ConcurrentHashMap<String, ResponseEntry>()

    // Thread-safe storage for events that need to be broadcast to ALL frames
    // Maps event ID to EventBroadcast (event data + consumed frame IDs)
    private val broadcastEvents = ConcurrentHashMap<String, EventBroadcast>()

    private data class ResponseEntry(
        val response: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private data class EventBroadcast(
        val eventData: String,
        val consumedByFrames: MutableSet<String> = mutableSetOf(),
        val timestamp: Long = System.currentTimeMillis(),
    )

    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString(BrowserConstants.KEY_TYPE)

            onMessage(json, type)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to handle bridge message", e)
            onError("Invalid bridge message: ${e.message}")
        }
    }

    /**
     * Store a response that any iframe can pull.
     * Called from Kotlin when a response is ready.
     */
    fun storeResponse(messageId: String, response: String) {
        // Enforce max size limit
        if (availableResponses.size >= MAX_STORED_RESPONSES) {
            // Remove oldest entry
            val oldestEntry = availableResponses.entries.minByOrNull { it.value.timestamp }
            oldestEntry?.let {
                availableResponses.remove(it.key)
                Logger.w(TAG, "Response storage full, removed oldest response: ${it.key}")
            }
        }

        availableResponses[messageId] = ResponseEntry(response)
    }

    /**
     * Pull a response for a specific messageId.
     * Called from JavaScript in any frame (parent or iframe).
     * Returns the response JSON string or null if not available.
     */
    @JavascriptInterface
    fun pullResponse(messageId: String): String? {
        val entry = availableResponses.remove(messageId)
        if (entry != null) {
            return entry.response
        }
        return null
    }

    /**
     * Check if a response is available for a messageId.
     * Called from JavaScript to poll for responses.
     */
    @JavascriptInterface
    fun hasResponse(messageId: String): Boolean {
        return availableResponses.containsKey(messageId)
    }

    /**
     * Send a TonConnect response from the RPC bridge back to the internal browser WebView.
     * This is called by bridge.ts jsBridgeTransport to deliver responses.
     */
    @JavascriptInterface
    fun postResponse(message: String) {
        try {
            val json = JSONObject(message)
            onResponse?.invoke(json) ?: Logger.w(TAG, "No response handler configured")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to handle bridge response", e)
            onError("Invalid bridge response: ${e.message}")
        }
    }

    /**
     * Store an event (like disconnect) that ALL frames can pull.
     * Called from Kotlin when an event needs to be delivered to the dApp.
     * Events are broadcast - each frame can pull independently.
     */
    @JavascriptInterface
    fun storeEvent(event: String) {
        try {
            val json = JSONObject(event)
            val eventId = "${System.currentTimeMillis()}-${event.hashCode()}"

            // Enforce max size limit for broadcast events
            if (broadcastEvents.size >= MAX_BROADCAST_EVENTS) {
                // Remove oldest event
                val oldestEntry = broadcastEvents.entries.minByOrNull { it.value.timestamp }
                oldestEntry?.let {
                    broadcastEvents.remove(it.key)
                    Logger.w(TAG, "Broadcast storage full, removed oldest event: ${it.key}")
                }
            }

            broadcastEvents[eventId] = EventBroadcast(event)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to store event for broadcast", e)
        }
    }

    /**
     * Pull an event for a specific frame.
     * Called from JavaScript in any frame to check for new events.
     * Returns an event JSON string that this frame hasn't seen yet, or null.
     *
     * This implements broadcast: each frame gets each event once.
     */
    @JavascriptInterface
    fun pullEvent(frameId: String): String? {
        // Find first event that this frame hasn't consumed yet
        for ((eventId, broadcast) in broadcastEvents) {
            synchronized(broadcast) {
                if (!broadcast.consumedByFrames.contains(frameId)) {
                    // Mark this frame as having consumed the event
                    broadcast.consumedByFrames.add(frameId)

                    // If all expected frames have consumed this event, or if too many frames have seen it, remove it
                    // We use a generous limit (10 frames) to ensure all legitimate frames get the event
                    if (broadcast.consumedByFrames.size >= MAX_FRAMES_PER_EVENT) {
                        broadcastEvents.remove(eventId)
                    }

                    return broadcast.eventData
                }
            }
        }

        return null
    }

    /**
     * Check if there are any pending events for a specific frame.
     * Called from JavaScript to poll for events.
     */
    @JavascriptInterface
    fun hasEvent(frameId: String): Boolean {
        return broadcastEvents.any { (_, broadcast) ->
            !broadcast.consumedByFrames.contains(frameId)
        }
    }

    companion object {
        private const val TAG = "BridgeInterface"
        private const val MAX_BROADCAST_EVENTS = 50
        private const val MAX_FRAMES_PER_EVENT = 10
        private const val MAX_STORED_RESPONSES = 100
    }
}
