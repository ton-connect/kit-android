/**
 * Mock scenario: RPC timeout
 * Tests SDK behavior when RPC calls don't receive responses within timeout period.
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

// Send ready event
setTimeout(() => {
    sendReadyEvent('testnet');
}, 100);

// Handle RPC calls - intentionally delay responses beyond timeout
window.__walletkitCall = function(method, params, callId) {
    console.log(`[RPC Timeout Mock] Received call: ${method} (${callId})`);
    
    // Delay response by 10 seconds (way beyond typical timeout)
    setTimeout(() => {
        sendRpcResponse(callId, { delayed: true });
    }, 10000);
};
