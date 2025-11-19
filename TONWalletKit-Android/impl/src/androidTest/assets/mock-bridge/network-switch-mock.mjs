/**
 * Mock scenario: Network switching during operation
 * Tests SDK behavior when network changes mid-operation.
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

let currentNetwork = 'testnet';

setTimeout(() => {
    sendReadyEvent(currentNetwork);
}, 100);

window.__walletkitCall = function(method, params, callId) {
    // Respond normally
    setTimeout(() => {
        sendRpcResponse(callId, { network: currentNetwork, success: true });
    }, 50);
    
    // But then switch network after response
    if (method === 'getWallets') {
        setTimeout(() => {
            currentNetwork = 'mainnet';
            sendReadyEvent(currentNetwork);
        }, 150);
    }
};
