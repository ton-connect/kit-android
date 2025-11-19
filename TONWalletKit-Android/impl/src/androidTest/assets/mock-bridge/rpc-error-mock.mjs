/**
 * Mock scenario: RPC error responses
 * Tests SDK handling of error responses from JavaScript bridge.
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

// Send ready event
setTimeout(() => {
    sendReadyEvent('testnet');
}, 100);

// Handle RPC calls - always return errors
window.__walletkitCall = function(method, params, callId) {
    console.log(`[RPC Error Mock] Received call: ${method} (${callId})`);
    
    setTimeout(() => {
        if (method === 'getWallets') {
            sendRpcResponse(callId, null, 'NO_WALLETS_FOUND');
        } else if (method === 'disconnect') {
            sendRpcResponse(callId, null, 'DISCONNECT_FAILED');
        } else {
            sendRpcResponse(callId, null, 'UNKNOWN_ERROR');
        }
    }, 50);
};
