// Bridge injection for Android internal browser
import { Buffer } from 'buffer';

window.Buffer = Buffer;
if (globalThis && !globalThis.Buffer) {
    globalThis.Buffer = Buffer;
}

import { injectBridgeCode } from '@ton/walletkit/bridge';

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
    about_url: 'https://example.com/about', // SDK expects about_url not aboutUrl  
    image: 'https://example.com/image.png', // SDK expects image not imageUrl
    platforms: ['ios' as const, 'android' as const, 'macos' as const, 'windows' as const, 'linux' as const, 'chrome' as const, 'firefox' as const, 'safari' as const], // supported platforms
    jsBridgeKey: 'tonkeeper', // window key for wallet bridge
    injected: true, // wallet is injected into the page (via injectBridgeCode)
    embedded: false, // dApp is not embedded in wallet (wallet's internal browser)
    tondns: 'tonkeeper.ton', // tondns for wallet
    bridgeUrl: 'https://bridge.tonapi.io/bridge', // url for wallet bridge
    universalLink: 'https://example.com/universal-link', // universal link for wallet
    deepLink: 'https://example.com/deep-link', // deep link for wallet
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

// Inject wallet with proper configuration matching demo wallet extension
injectBridgeCode(window, {
    deviceInfo,
    walletInfo
});

// Patch the injected bridge to forward to Android
setTimeout(() => {
    const bridge = (window as any).tonkeeper?.tonconnect;
    
    if (!bridge) {
        console.warn('[TonConnect] Bridge not found after injection');
        return;
    }
    
    console.log('[TonConnect] Patching bridge for Android WebView');
    
    const original = bridge._sendToExtension;
    
    bridge._sendToExtension = function(message: any) {
        console.log('[TonConnect] Intercepting:', message?.method);
        
        if (!isAndroidWebView) {
            return original?.call(this, message);
        }
        
        const messageId = `msg-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
        const method = message?.method || 'unknown';
        const params = message?.params || {};
        
        const request = {
            type: 'TONCONNECT_BRIDGE_REQUEST',
            messageId,
            method,
            params,
            frameId
        };
        
        (window as any).AndroidTonConnect.postMessage(JSON.stringify(request));
        
        return new Promise((resolve, reject) => {
            const timeout = setTimeout(() => reject(new Error('Timeout')), 30000);
            bridge._pendingRequests = bridge._pendingRequests || {};
            bridge._pendingRequests[messageId] = { resolve, reject, timeout };
        });
    };
    
    // Test function
    (window as any).__testTonConnect = async () => {
        console.log('[TonConnect TEST] Testing...');
        try {
            const result = await bridge._sendToExtension({ method: 'connect', params: {} });
            console.log('[TonConnect TEST] Success:', result);
            return result;
        } catch (error) {
            console.error('[TonConnect TEST] Failed:', error);
            throw error;
        }
    };
    
    console.log('[TonConnect] Bridge patched, test: window.__testTonConnect()');
}, 100);

// Handle responses from Android
window.addEventListener('message', (event: MessageEvent) => {
    if (event.source !== window) return;
    
    const data = event.data;
    if (!data || typeof data !== 'object') return;
    
    const bridge = (window as any).tonkeeper?.tonconnect;
    if (!bridge) return;
    
    if (data.type === 'TONCONNECT_BRIDGE_RESPONSE' && data.messageId) {
        console.log(`[TonConnect] Response: ${data.messageId}`);
        const pending = bridge._pendingRequests?.[data.messageId];
        if (pending) {
            clearTimeout(pending.timeout);
            delete bridge._pendingRequests[data.messageId];
            
            if (data.error) {
                pending.reject(new Error(data.error.message || 'Failed'));
            } else {
                pending.resolve(data.result);
            }
        }
    }
});

console.log(`[TonConnect] Bridge ready for frame: ${frameId}`);
