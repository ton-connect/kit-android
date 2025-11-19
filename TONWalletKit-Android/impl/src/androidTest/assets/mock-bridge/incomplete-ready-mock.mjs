/**
 * Scenario #28: Bridge never sends ready event (already covered by no-ready-mock)
 * Scenario #29: Ready event missing network/apiBaseUrl
 */
import { sendEvent } from './base-mock.mjs';

setTimeout(() => {
    // Send ready without required fields
    if (window.WalletKitNative && window.WalletKitNative.onMessage) {
        window.WalletKitNative.onMessage(JSON.stringify({
            type: 'ready'
            // Missing network and api_base_url!
        }));
    }
}, 100);

window.__walletkitCall = function(method, params, callId) {
    console.log('[Incomplete Ready] Call received but bridge improperly initialized');
};
