/**
 * Mock scenario: Rapid RPC calls
 * 
 * Tests SDK handling of many concurrent RPC requests.
 * The mock queues all incoming RPC calls and then responds to ALL of them
 * at once after a delay. This tests that the SDK correctly matches responses
 * to their corresponding requests without mixing them up.
 * 
 * Real-world scenario: Network batching, slow backend that processes requests
 * in bulk, or race conditions where responses arrive out of order.
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

const pendingCalls = [];
let callCounter = 0;

// Send ready event immediately
setTimeout(() => {
    sendReadyEvent('testnet');
}, 50);

// Handle RPC calls - queue them and respond later in batch
window.__walletkitCall = function(id, method, params) {
    console.log(`[Rapid RPC Mock] Queuing call #${callCounter++}: ${method} (id=${id})`);
    pendingCalls.push({ id, method, params });
    
    // After collecting calls for 300ms, respond to all at once
    if (pendingCalls.length === 1) {
        setTimeout(() => {
            console.log(`[Rapid RPC Mock] Responding to ${pendingCalls.length} calls simultaneously`);
            
            // Respond to all calls at the same time to test concurrent handling
            pendingCalls.forEach((call, index) => {
                if (call.method === 'createTonMnemonic') {
                    // Generate unique but valid mnemonic for each call
                    const words = [
                        'abandon', 'ability', 'able', 'about', 'above', 'absent',
                        'absorb', 'abstract', 'absurd', 'abuse', 'access', 'accident',
                        'account', 'accuse', 'achieve', 'acid', 'acoustic', 'acquire',
                        'across', 'act', 'action', 'actor', 'actress', `word${index}`
                    ];
                    sendRpcResponse(call.id, { words });
                } else {
                    // Other methods get generic success
                    sendRpcResponse(call.id, { success: true });
                }
            });
            
            pendingCalls.length = 0; // Clear queue
        }, 300);
    }
};
