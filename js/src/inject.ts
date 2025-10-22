// Bridge injection for Android internal browser
// This file is the entry point for injecting TonConnect bridge into WebViews
import { Buffer } from 'buffer';

window.Buffer = Buffer;
if (globalThis && !globalThis.Buffer) {
    globalThis.Buffer = Buffer;
}

import { injectBridgeCode } from '@ton/walletkit/bridge';

function injectTonConnectBridge() {
    try {
        // Inject the simplified bridge that forwards to Android WebView
        injectBridgeCode(window, {
            // Configuration can be passed from Android side if needed
            // For now, using defaults which will use the extension transport
            // Android SDK should provide custom transport via JavascriptInterface
        });

        console.log('TonConnect bridge injected for Android internal browser');
    } catch (error) {
        console.error('Failed to inject TonConnect bridge:', error);
    }
}

injectTonConnectBridge();
