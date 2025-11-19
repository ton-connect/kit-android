/**
 * Scenario #17: Event with missing required fields
 */
import { sendReadyEvent } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

setTimeout(() => {
    // Send malformed event
    if (window.WalletKitNative && window.WalletKitNative.onMessage) {
        window.WalletKitNative.onMessage(JSON.stringify({
            type: 'event',
            event_type: 'connect_request'
            // Missing data field!
        }));
    }
}, 200);

window.__walletkitCall = function(method, params, callId) {};
