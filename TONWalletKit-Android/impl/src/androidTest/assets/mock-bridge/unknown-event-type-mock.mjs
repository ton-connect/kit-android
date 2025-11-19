/**
 * Scenario #19: Event type unknown/unsupported
 */
import { sendReadyEvent } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

setTimeout(() => {
    if (window.WalletKitNative && window.WalletKitNative.onMessage) {
        window.WalletKitNative.onMessage(JSON.stringify({
            type: 'event',
            event_type: 'future_event_v2_not_yet_supported',
            data: { some: 'data' }
        }));
    }
}, 200);

window.__walletkitCall = function(method, params, callId) {};
