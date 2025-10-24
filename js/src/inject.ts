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

// Generate unique frame ID
const frameId = window === window.top 
    ? 'main' 
    : `frame-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

(window as any).__tonconnect_frameId = frameId;

const isAndroidWebView = typeof (window as any).AndroidTonConnect !== 'undefined';

console.log(`[TonConnect] Frame ${frameId} initializing (Android: ${isAndroidWebView})`);

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
    private eventCallbacks: Array<(event: unknown) => void> = [];

    constructor() {
        // Listen for responses from Android
        window.addEventListener('message', (event: MessageEvent) => {
            if (event.source !== window) return;
            
            const data = event.data;
            if (!data || typeof data !== 'object') return;
            
            if (data.type === 'TONCONNECT_BRIDGE_RESPONSE' && data.messageId) {
                console.log(`[AndroidTransport] Response: ${data.messageId}`);
                const pending = this.pendingRequests.get(data.messageId);
                if (pending) {
                    clearTimeout(pending.timeout);
                    this.pendingRequests.delete(data.messageId);
                    
                    if (data.error) {
                        pending.reject(new Error(data.error.message || 'Failed'));
                    } else {
                        pending.resolve(data.payload);
                    }
                }
            } else if (data.type === 'TONCONNECT_BRIDGE_EVENT') {
                // Handle events from Android wallet
                console.log(`[AndroidTransport] Event:`, data.event);
                this.eventCallbacks.forEach(callback => {
                    try {
                        callback(data.event);
                    } catch (error) {
                        console.error('[AndroidTransport] Event callback error:', error);
                    }
                });
            }
        });
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
        this.eventCallbacks.push(callback);
    }

    isAvailable(): boolean {
        return isAndroidWebView;
    }

    requestContentScriptInjection(): void {
        // Not needed for Android WebView - already injected
    }

    destroy(): void {
        // Clear pending requests
        this.pendingRequests.forEach(({ timeout, reject }) => {
            clearTimeout(timeout);
            reject(new Error('Transport destroyed'));
        });
        this.pendingRequests.clear();
        this.eventCallbacks = [];
    }
}

// Create custom transport for Android or undefined for default behavior
const transport: Transport | undefined = isAndroidWebView ? new AndroidWebViewTransport() : undefined;

// Inject wallet with proper configuration and custom transport
injectBridgeCode(window, {
    deviceInfo,
    walletInfo,
    isWalletBrowser: true  // CRITICAL: tells SDK this is wallet's internal browser
}, transport);

console.log(`[TonConnect] Bridge ready for frame: ${frameId} (transport: ${transport ? 'Android' : 'default'})`);
console.log('[TonConnect] Wallet Info:', JSON.stringify(walletInfo, null, 2));
console.log('[TonConnect] isWalletBrowser check:', (window as any).tonkeeper?.tonconnect?.isWalletBrowser);

