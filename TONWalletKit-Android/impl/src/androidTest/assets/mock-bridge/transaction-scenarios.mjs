// Mock JavaScript bridge scenarios for transaction handling
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #50: Transaction confirmation timeout
export function transactionTimeout() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            // Send pending event
            sendEvent('transaction_pending', { txId: 'tx123' });
            // Never send confirmation - timeout scenario
        }
    };
}

// Scenario #51: Transaction failed after pending
export function transactionFailedAfterPending() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            sendEvent('transaction_pending', { txId: 'tx456' });
            setTimeout(() => {
                sendEvent('transaction_failed', { 
                    txId: 'tx456', 
                    error: 'insufficient_funds' 
                });
                sendRpcResponse(callId, { 
                    success: false, 
                    error: 'insufficient_funds' 
                });
            }, 500);
        }
    };
}

// Scenario #52: Multiple transactions queued
export function multipleTransactionsQueued() {
    sendReadyEvent();
    
    let txCount = 0;
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            txCount++;
            const txId = `tx_${txCount}`;
            sendEvent('transaction_queued', { txId, position: txCount });
            
            // Send confirmations in sequence
            setTimeout(() => {
                sendEvent('transaction_confirmed', { txId });
                sendRpcResponse(callId, { success: true, txId });
            }, txCount * 200);
        }
    };
}

// Scenario #53: Transaction signing cancelled
export function transactionSigningCancelled() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            sendEvent('signing_requested', { txId: 'tx789' });
            setTimeout(() => {
                sendEvent('signing_cancelled', { txId: 'tx789', reason: 'user_cancelled' });
                sendRpcResponse(callId, { 
                    success: false, 
                    error: 'user_cancelled' 
                });
            }, 300);
        }
    };
}

// Scenario #54: Transaction amount validation error
export function transactionAmountError() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            sendRpcResponse(callId, { 
                success: false, 
                error: 'invalid_amount',
                details: 'Amount exceeds maximum allowed value'
            });
        }
    };
}
