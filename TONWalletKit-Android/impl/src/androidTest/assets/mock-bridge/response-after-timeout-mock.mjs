/**
 * Scenario #2: RPC response arrives after timeout/cancellation
 */
import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

window.__walletkitCall = function(method, params, callId) {
    // Send response after 30 seconds (way beyond timeout)
    setTimeout(() => {
        sendRpcResponse(callId, { delayed: true, method });
    }, 30000);
};
