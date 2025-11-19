/**
 * Scenario #3: RPC response with missing call ID
 */
import { sendReadyEvent } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

window.__walletkitCall = function(method, params, callId) {
    setTimeout(() => {
        // Send response without call_id field
        if (window.WalletKitNative && window.WalletKitNative.onMessage) {
            window.WalletKitNative.onMessage(JSON.stringify({
                type: 'rpc-response',
                result: { success: true }
                // Missing call_id!
            }));
        }
    }, 50);
};
