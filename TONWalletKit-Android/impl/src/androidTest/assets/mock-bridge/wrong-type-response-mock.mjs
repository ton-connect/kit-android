/**
 * Scenario #6: RPC response with wrong type (array instead of object)
 */
import { sendReadyEvent } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

window.__walletkitCall = function(method, params, callId) {
    setTimeout(() => {
        if (window.WalletKitNative && window.WalletKitNative.onMessage) {
            // Send array instead of object
            window.WalletKitNative.onMessage(JSON.stringify({
                type: 'rpc-response',
                call_id: callId,
                result: [1, 2, 3] // Array instead of object
            }));
        }
    }, 50);
};
