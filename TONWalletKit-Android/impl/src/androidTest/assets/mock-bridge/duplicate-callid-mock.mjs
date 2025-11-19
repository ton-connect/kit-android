/**
 * Mock scenario: Duplicate call IDs
 * Tests SDK handling of responses with duplicate call IDs.
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

// Send ready event
setTimeout(() => {
    sendReadyEvent('testnet');
}, 100);

// Handle RPC calls - send duplicate responses
window.__walletkitCall = function(method, params, callId) {
    console.log(`[Duplicate ID Mock] Received call: ${method} (${callId})`);
    
    // Send first response
    setTimeout(() => {
        sendRpcResponse(callId, { attempt: 1 });
    }, 50);
    
    // Send duplicate response with same call ID
    setTimeout(() => {
        console.log(`[Duplicate ID Mock] Sending duplicate for: ${callId}`);
        sendRpcResponse(callId, { attempt: 2 });
    }, 100);
};
