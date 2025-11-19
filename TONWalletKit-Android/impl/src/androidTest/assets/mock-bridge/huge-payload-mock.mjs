/**
 * Scenario #7: RPC response payload exceeds size limits
 */
import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

window.__walletkitCall = function(method, params, callId) {
    // Create 10MB+ response
    const hugeString = 'x'.repeat(10 * 1024 * 1024);
    setTimeout(() => {
        sendRpcResponse(callId, { data: hugeString });
    }, 100);
};
