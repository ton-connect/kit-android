/**
 * Scenario #1: Multiple rapid RPC responses for same call ID
 * JS sends multiple responses with identical call ID before Android processes first one
 */
import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

setTimeout(() => sendReadyEvent('testnet'), 100);

window.__walletkitCall = function(method, params, callId) {
    // Send 5 rapid responses with same call ID
    for (let i = 0; i < 5; i++) {
        setTimeout(() => {
            sendRpcResponse(callId, { attempt: i + 1 });
        }, i * 10);
    }
};
