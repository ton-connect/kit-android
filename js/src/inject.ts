// Bridge injection for Android internal browser
import { Buffer } from 'buffer';

window.Buffer = Buffer;
if (globalThis && !globalThis.Buffer) {
    globalThis.Buffer = Buffer;
}

import { injectBridgeCode } from '@ton/walletkit/bridge';
import type { InjectedToExtensionBridgeRequestPayload } from '@ton/walletkit';

// Import Transport type - it's available as internal export
interface Transport {
    send(request: Omit<InjectedToExtensionBridgeRequestPayload, 'id'>): Promise<unknown>;
    onEvent(callback: (event: unknown) => void): void;
    isAvailable(): boolean;
    requestContentScriptInjection(): void;
    destroy(): void;
}

// Polyfill Buffer
if (typeof window !== 'undefined') {
    (window as any).Buffer = Buffer;
}
if (typeof globalThis !== 'undefined' && !globalThis.Buffer) {
    (globalThis as any).Buffer = Buffer;
}

// Generate unique frame ID (preserve existing one if already set to prevent re-injection issues)
const frameId = (window as any).__tonconnect_frameId || (window === window.top 
    ? 'main' 
    : `frame-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);

// Store the frameId (only set if not already set)
if (!(window as any).__tonconnect_frameId) {
    (window as any).__tonconnect_frameId = frameId;
}

const isAndroidWebView = typeof (window as any).AndroidTonConnect !== 'undefined';

console.log(`[TonConnect] ===== INJECTION STARTING =====`);
console.log(`[TonConnect] Frame ID: ${frameId}`);
console.log(`[TonConnect] Is top window: ${window === window.top}`);
console.log(`[TonConnect] Is iframe: ${window !== window.parent}`);
console.log(`[TonConnect] Android WebView: ${isAndroidWebView}`);
console.log(`[TonConnect] Current URL: ${window.location.href}`);
console.log(`[TonConnect] ===== STARTING BRIDGE SETUP =====`);

// Device info matching demo wallet extension format
const deviceInfo = {
    platform: 'android' as const,
    appName: 'Tonkeeper',
    appVersion: '1.0.0',
    maxProtocolVersion: 2,
    features: [
        'SendTransaction',
        {
            name: 'SendTransaction',
            maxMessages: 4
        },
        {
            name: 'SignData',
            types: ['text', 'binary', 'cell']
        }
    ] as any
};

// Wallet info matching demo wallet extension format
// NOTE: TonConnect SDK expects snake_case properties (app_name, about_url, image)
// even though the TypeScript types use camelCase
const walletInfo = {
    name: 'tonkeeper', // key for wallet
    app_name: 'Tonkeeper', // SDK expects app_name not appName
    about_url: 'https://tonkeeper.com', // SDK expects about_url not aboutUrl  
    image: 'https://tonkeeper.com/assets/tonconnect-icon.png', // SDK expects image not imageUrl
    platforms: ['ios' as const, 'android' as const, 'macos' as const, 'windows' as const, 'linux' as const, 'chrome' as const, 'firefox' as const, 'safari' as const], // supported platforms
    jsBridgeKey: 'tonkeeper', // window key for wallet bridge
    injected: true, // wallet is injected into the page (via injectBridgeCode)
    embedded: true, // dApp IS embedded in wallet (wallet's internal browser) - tells dApp to prefer injected bridge
    tondns: 'tonkeeper.ton', // tondns for wallet
    bridgeUrl: 'https://bridge.tonapi.io/bridge', // url for wallet bridge
    features: [
        'SendTransaction',
        {
            name: 'SendTransaction',
            maxMessages: 4
        },
        {
            name: 'SignData',
            types: ['text', 'binary', 'cell']
        }
    ] as any
} as any; // Cast to any to bypass TypeScript type checking

/**
 * Android WebView Transport Implementation
 * Custom transport that communicates with Android native code instead of extension
 */
class AndroidWebViewTransport implements Transport {
    private pendingRequests = new Map<string, { resolve: (value: unknown) => void; reject: (error: Error) => void; timeout: NodeJS.Timeout }>();
    private pollingInterval: ReturnType<typeof setInterval> | null = null;
    private eventCallbacks: Array<(event: unknown) => void> = [];

    constructor() {
        // Start polling for responses and events
        // This works in ALL frames (main + iframes) via @JavascriptInterface
        this.startPolling();
    }


    private tryPullResponse(messageId: string): void {
        const pending = this.pendingRequests.get(messageId);
        if (!pending) return;

        try {
            const bridge = (window as any).AndroidTonConnect;
            if (!bridge || !bridge.pullResponse) return;

            const responseStr = bridge.pullResponse(messageId);
            if (responseStr) {
                const response = JSON.parse(responseStr);
                console.log(`[AndroidTransport] Pulled response for: ${messageId}`);
                
                clearTimeout(pending.timeout);
                this.pendingRequests.delete(messageId);
                
                if (response.error) {
                    pending.reject(new Error(response.error.message || "Failed"));
                } else {
                    pending.resolve(response.payload);
                }
            }
        } catch (error) {
            console.error("[AndroidTransport] Failed to pull response:", error);
        }
    }

    private startPolling(): void {
        // Poll every 100ms to check for responses AND events
        this.pollingInterval = setInterval(() => {
            const bridge = (window as any).AndroidTonConnect;
            if (!bridge) return;
            
            // Check for pending responses
            if (this.pendingRequests.size > 0 && bridge.hasResponse) {
                this.pendingRequests.forEach((pending, messageId) => {
                    if (bridge.hasResponse(messageId)) {
                        console.log(`[AndroidTransport] Polling detected response for: ${messageId}`);
                        this.tryPullResponse(messageId);
                    }
                });
            }
            
            // Check for pending events (like disconnect)
            // Kotlin side now broadcasts events to ALL frames - each frame gets each event once
            if (bridge.hasEvent && bridge.pullEvent) {
                // Check if there are events for this specific frame (Kotlin tracks which frames consumed which events)
                while (bridge.hasEvent(frameId)) {
                    try {
                        // Pull event for this frame - Kotlin will mark it as consumed by this frame
                        const eventStr = bridge.pullEvent(frameId);
                        if (eventStr) {
                            const data = JSON.parse(eventStr) as {
                                type?: string;
                                event?: unknown;
                            };

                            console.log('[AndroidTransport] 🔔 Pulled event from BridgeInterface (broadcast)');
                            console.log('[AndroidTransport] 🔔 Frame ID:', frameId);
                            console.log('[AndroidTransport] 🔔 Window location:', window.location.href);
                            console.log('[AndroidTransport] 🔔 Is top window:', window === window.top);
                            console.log('[AndroidTransport] 🔔 Event data:', JSON.stringify(data).substring(0, 200));

                            if (data.type === 'TONCONNECT_BRIDGE_EVENT' && data.event) {
                                console.log('[AndroidTransport] 🔔 Processing event:', data.event);
                                console.log('[AndroidTransport] 🔔 Event callbacks count:', this.eventCallbacks.length);

                                // Process event if we have callbacks
                                if (this.eventCallbacks.length > 0) {
                                    console.log('[AndroidTransport] 🔔 Event callback details:', this.eventCallbacks.map((cb, i) => `#${i}: ${typeof cb}`));

                                    this.eventCallbacks.forEach((callback, index) => {
                                        try {
                                            console.log(`[AndroidTransport] 🔔 Calling event callback #${index}`);
                                            callback(data.event);
                                            console.log(`[AndroidTransport] ✅ Event callback #${index} completed`);
                                        } catch (error) {
                                            console.error(`[AndroidTransport] ❌ Event callback #${index} error:`, error);
                                        }
                                    });
                                } else {
                                    console.log('[AndroidTransport] ⏭️ No callbacks in this frame');
                                }
                            }
                        }
                    } catch (error) {
                        console.error('[AndroidTransport] Failed to pull/process event:', error);
                        break; // Stop pulling if there's an error
                    }
                }
            }
        }, 100);
    }

    private stopPolling(): void {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }

    async send(request: Omit<InjectedToExtensionBridgeRequestPayload, 'id'>): Promise<unknown> {
        console.log('[AndroidTransport] Sending:', request.method);
        
        const messageId = `msg-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
        const method = request.method || 'unknown';
        const params = request.params || {};
        
        const payload = {
            type: 'TONCONNECT_BRIDGE_REQUEST',
            messageId,
            method,
            params,
            frameId
        };
        
        (window as any).AndroidTonConnect.postMessage(JSON.stringify(payload));
        
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                this.pendingRequests.delete(messageId);
                reject(new Error('Request timeout'));
            }, 30000);
            
            this.pendingRequests.set(messageId, { resolve, reject, timeout });
        });
    }

    onEvent(callback: (event: unknown) => void): void {
        console.log('[AndroidTransport] 📝 Registering event callback');
        console.log('[AndroidTransport] 📝 Frame ID:', frameId);
        console.log('[AndroidTransport] 📝 Window location:', window.location.href);
        console.log('[AndroidTransport] 📝 Is top window:', window === window.top);
        console.log('[AndroidTransport] 📝 Callbacks before:', this.eventCallbacks.length);
        
        this.eventCallbacks.push(callback);
        console.log('[AndroidTransport] 📝 Callbacks after:', this.eventCallbacks.length);
    }

    isAvailable(): boolean {
        return isAndroidWebView;
    }

    requestContentScriptInjection(): void {
        console.log('[TonConnect] ⚠️ requestContentScriptInjection CALLED - IframeWatcher detected iframe!');
        
        // For Android WebView, we need to inject the bridge into iframes
        if (typeof document !== 'undefined') {
            const iframes = document.querySelectorAll('iframe');
            console.log(`[TonConnect] Found ${iframes.length} iframes in DOM`);
            
            iframes.forEach((iframe, index) => {
                console.log(`[TonConnect] Processing iframe ${index}:`, iframe.src || iframe.getAttribute('src') || '(no src)');
                
                try {
                    // Try to access iframe's window (will fail for cross-origin)
                    const iframeWindow = iframe.contentWindow;
                    
                    if (!iframeWindow) {
                        console.log(`[TonConnect] iframe ${index}: contentWindow is null`);
                        return;
                    }
                    
                    if (iframeWindow === window) {
                        console.log(`[TonConnect] iframe ${index}: contentWindow === window (skipping self)`);
                        return;
                    }
                    
                    // Check if bridge already exists in this iframe
                    const hasExtension = !!(iframeWindow as any).tonkeeper?.tonconnect;
                    console.log(`[TonConnect] iframe ${index}: Bridge exists? ${hasExtension}`);
                    
                    if (!hasExtension) {
                        console.log(`[TonConnect] ✅ Injecting bridge into same-origin iframe ${index}`);
                        // Re-run injection in the iframe context
                        injectBridgeCode(iframeWindow, {
                            deviceInfo,
                            walletInfo,
                            isWalletBrowser: true
                        }, new AndroidWebViewTransport());
                        console.log(`[TonConnect] ✅ Bridge injection complete for iframe ${index}`);
                    }
                } catch (e) {
                    // Cross-origin iframe, can't access
                    console.log(`[TonConnect] iframe ${index}: Cross-origin - will use postMessage bridge (${(e as Error).message})`);
                }
            });
        } else {
            console.log('[TonConnect] document is undefined, cannot query iframes');
        }
    }

    destroy(): void {
        this.stopPolling();
        this.pendingRequests.clear();
        this.eventCallbacks = [];
        // Clear pending requests
        this.pendingRequests.forEach(({ timeout, reject }) => {
            clearTimeout(timeout);
            reject(new Error('Transport destroyed'));
        });
    }
}

/**
 * Iframe Bridge Support - NOT NEEDED FOR ANDROID WEBVIEW
 * 
 * Android WebView automatically injects JavaScript into ALL frames (main + iframes)
 * This is different from browser extensions which need postMessage bridges for cross-origin iframes.
 * 
 * Each iframe gets its own direct injection of window.tonkeeper.tonconnect via injectBridgeCode(),
 * so there's no need for a postMessage relay between parent and iframe.
 * 
 * See BridgeInjector.kt:
 * - Uses webView.evaluateJavascript() which runs in all frames
 * - Android WebView documentation: "JavaScript runs in the context of the current page,
 *   including all iframes within that page"
 */
console.log('[TonConnect] Android WebView injects bridge into all frames automatically');

// Create custom transport for Android or undefined for default behavior
const transport: Transport | undefined = isAndroidWebView ? new AndroidWebViewTransport() : undefined;

// Function to inject the bridge
const performInjection = () => {
    console.log('[TonConnect] Injecting bridge code...');
    console.log('[TonConnect] document.body exists?', !!document.body);
    console.log('[TonConnect] Current iframes in DOM:', document.querySelectorAll('iframe').length);
    
    // Inject wallet with proper configuration and custom transport
    injectBridgeCode(window, {
        deviceInfo,
        walletInfo,
        isWalletBrowser: true  // CRITICAL: tells SDK this is wallet's internal browser
    }, transport);

    console.log(`[TonConnect] Bridge ready for frame: ${frameId} (transport: ${transport ? 'Android' : 'default'})`);
    console.log('[TonConnect] Wallet Info:', JSON.stringify(walletInfo, null, 2));
    console.log('[TonConnect] isWalletBrowser check:', (window as any).tonkeeper?.tonconnect?.isWalletBrowser);
    
    // After injection, manually check for existing iframes
    setTimeout(() => {
        const iframes = document.querySelectorAll('iframe');
        console.log(`[TonConnect] Post-injection check: ${iframes.length} iframes found`);
        if (iframes.length > 0) {
            console.log('[TonConnect] ⚠️ Iframes exist but IframeWatcher may not have triggered yet');
            console.log('[TonConnect] Manually triggering iframe injection...');
            if (transport && 'requestContentScriptInjection' in transport) {
                (transport as any).requestContentScriptInjection();
            }
        }
    }, 100);
};

// Wait for document.body to exist before injecting
// This is critical because IframeWatcher needs document.body to observe for iframes
if (!document.body) {
    console.log('[TonConnect] Waiting for document.body before injecting bridge...');
    
    // Use DOMContentLoaded if DOM is still loading
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            console.log('[TonConnect] DOMContentLoaded fired, injecting bridge');
            performInjection();
        }, { once: true });
    } else {
        // DOM is interactive/complete but body doesn't exist yet (rare) - use timer
        const checkBody = () => {
            if (document.body) {
                console.log('[TonConnect] document.body now available, injecting bridge');
                performInjection();
            } else {
                setTimeout(checkBody, 10);
            }
        };
        checkBody();
    }
} else {
    // Body already exists, inject immediately
    console.log('[TonConnect] document.body exists, injecting bridge immediately');
    performInjection();
}
