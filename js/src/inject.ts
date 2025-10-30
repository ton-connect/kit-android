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
    private static readonly MAX_EVENT_REQUEUE_ATTEMPTS = 25;

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
            if (bridge.hasEvent && bridge.pullEvent) {
                while (bridge.hasEvent()) {
                    try {
                        const eventStr = bridge.pullEvent();
                        if (eventStr) {
                            const data = JSON.parse(eventStr) as {
                                type?: string;
                                event?: unknown;
                                __requeueCount?: number;
                            };
                            const currentRequeueCount =
                                typeof data.__requeueCount === 'number' ? data.__requeueCount : 0;

                            const tryRequeue = (reason: string): boolean => {
                                const nextCount = currentRequeueCount + 1;
                                if (nextCount > AndroidWebViewTransport.MAX_EVENT_REQUEUE_ATTEMPTS) {
                                    console.log(
                                        `[AndroidTransport] ⏭️ Requeue limit reached (${reason}), processing in this frame`,
                                    );
                                    delete data.__requeueCount;
                                    return false;
                                }

                                console.log(
                                    `[AndroidTransport] 🔁 Re-queuing event (${reason}), attempt ${nextCount}/${AndroidWebViewTransport.MAX_EVENT_REQUEUE_ATTEMPTS}`,
                                );
                                data.__requeueCount = nextCount;
                                if (bridge.storeEvent) {
                                    bridge.storeEvent(JSON.stringify(data));
                                }
                                return true;
                            };

                            console.log('[AndroidTransport] 🔔 Pulled event from BridgeInterface:', data);
                            console.log('[AndroidTransport] 🔔 Frame ID:', frameId);
                            console.log('[AndroidTransport] 🔔 Window location:', window.location.href);
                            console.log('[AndroidTransport] 🔔 Is top window:', window === window.top);

                            // If this frame does not have any TonConnect listeners but nested frames exist,
                            // re-queue the event so that the frame with active listeners can process it.
                            const hasTonConnectBridge = !!(window as any).tonkeeper?.tonconnect;
                            const bridgeHasListeners =
                                hasTonConnectBridge &&
                                typeof (window as any).tonkeeper.tonconnect.hasListeners === 'function'
                                    ? (window as any).tonkeeper.tonconnect.hasListeners()
                                    : this.eventCallbacks.length > 0;
                            const hasChildFrames =
                                typeof document !== 'undefined' &&
                                typeof document.querySelectorAll === 'function' &&
                                document.querySelectorAll('iframe').length > 0;

                            if (!bridgeHasListeners && hasChildFrames) {
                                if (tryRequeue('no-listeners-with-child-frames')) {
                                    break;
                                }
                            }

                            if (data.type === 'TONCONNECT_BRIDGE_EVENT' && data.event) {
                                console.log('[AndroidTransport] 🔔 Processing event:', data.event);
                                console.log('[AndroidTransport] 🔔 Event callbacks count:', this.eventCallbacks.length);

                                if (this.eventCallbacks.length === 0) {
                                    // No callbacks in this frame - put event back for other frames (within limit)
                                    if (tryRequeue('no-event-callbacks')) {
                                        break; // Stop pulling events in this frame
                                    }
                                    console.log(
                                        '[AndroidTransport] ⚠️ No callbacks available after requeue limit, dropping event in this frame',
                                    );
                                    delete data.__requeueCount;
                                    continue;
                                } else {
                                    console.log('[AndroidTransport] 🔔 Event callback details:', this.eventCallbacks.map((cb, i) => `#${i}: ${typeof cb}`));

                                    delete data.__requeueCount;
                                    this.eventCallbacks.forEach((callback, index) => {
                                        try {
                                            console.log(`[AndroidTransport] 🔔 Calling event callback #${index}`);
                                            callback(data.event);
                                            console.log(`[AndroidTransport] ✅ Event callback #${index} completed`);
                                        } catch (error) {
                                            console.error(`[AndroidTransport] ❌ Event callback #${index} error:`, error);
                                        }
                                    });
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
        
        // Wrap the callback to intercept connect events and fix storage type
        const wrappedCallback = (event: unknown) => {
            console.log('[AndroidTransport] 🔔 Event received in wrapped callback:', event);
            
            // Check if this is a connect event
            if (event && typeof event === 'object' && 'event' in event) {
                const walletEvent = event as { event: string; payload?: any };
                
                if (walletEvent.event === 'connect') {
                    console.log('[AndroidTransport] 🔌 Connect event detected! Fixing storage to use injected provider...');
                    
                    // Use setTimeout to allow the BridgeProvider to store the connection first,
                    // then we modify it to use injected type
                    setTimeout(() => {
                        try {
                            const bridge = (window as any).AndroidTonConnect;
                            if (!bridge || !bridge.storageGet || !bridge.storageSet) {
                                console.error('[AndroidTransport] ❌ Bridge storage methods not available');
                                return;
                            }
                            
                            const storageKey = 'ton-connect-storage_bridge-connection';
                            const storedConnectionStr = bridge.storageGet(storageKey);
                            
                            if (!storedConnectionStr) {
                                console.log('[AndroidTransport] ⚠️ No stored connection found');
                                return;
                            }
                            
                            console.log('[AndroidTransport] 📦 Original stored connection:', storedConnectionStr);
                            
                            const storedConnection = JSON.parse(storedConnectionStr);
                            
                            // Only modify if it's an HTTP connection
                            if (storedConnection.type === 'http') {
                                console.log('[AndroidTransport] 🔧 Converting HTTP connection to injected connection');
                                
                                // Create injected connection object
                                const injectedConnection = {
                                    type: 'injected',
                                    jsBridgeKey: 'tonkeeper',
                                    nextRpcRequestId: storedConnection.nextRpcRequestId || 0
                                };
                                
                                console.log('[AndroidTransport] 💾 Storing injected connection:', injectedConnection);
                                bridge.storageSet(storageKey, JSON.stringify(injectedConnection));
                                
                                console.log('[AndroidTransport] ✅ Storage updated! SDK will now use InjectedProvider');
                            } else {
                                console.log('[AndroidTransport] ℹ️ Connection is already type:', storedConnection.type);
                            }
                        } catch (error) {
                            console.error('[AndroidTransport] ❌ Failed to update storage:', error);
                        }
                    }, 100); // Small delay to ensure BridgeProvider has finished storing
                }
            }
            
            // Call the original callback
            callback(event);
        };
        
        this.eventCallbacks.push(wrappedCallback);
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
    
    // CRITICAL: Fix storage type BEFORE SDK initializes
    // If there's an existing HTTP connection in storage, convert it to injected
    if (isAndroidWebView) {
        try {
            const bridge = (window as any).AndroidTonConnect;
            if (bridge && bridge.storageGet && bridge.storageSet) {
                const storageKey = 'ton-connect-storage_bridge-connection';
                const storedConnectionStr = bridge.storageGet(storageKey);
                
                if (storedConnectionStr) {
                    console.log('[TonConnect] 🔍 Found existing connection in storage');
                    const storedConnection = JSON.parse(storedConnectionStr);
                    
                    if (storedConnection.type === 'http') {
                        console.log('[TonConnect] 🔧 Converting HTTP connection to injected BEFORE SDK initialization');
                        
                        // Create injected connection object
                        const injectedConnection = {
                            type: 'injected',
                            jsBridgeKey: 'tonkeeper',
                            nextRpcRequestId: storedConnection.nextRpcRequestId || 0
                        };
                        
                        console.log('[TonConnect] 💾 Storing injected connection:', injectedConnection);
                        bridge.storageSet(storageKey, JSON.stringify(injectedConnection));
                        console.log('[TonConnect] ✅ Storage updated! SDK will use InjectedProvider on initialization');
                    } else {
                        console.log('[TonConnect] ℹ️ Connection is already type:', storedConnection.type);
                    }
                } else {
                    console.log('[TonConnect] ℹ️ No existing connection in storage');
                }
            }
        } catch (error) {
            console.error('[TonConnect] ❌ Failed to fix storage type:', error);
        }
    }
    
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
