// Mock JavaScript bridge - Request/Response Flow Edge Cases
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #39: Approve called twice on same request
export function approveTwiceOnSameRequest() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'connect') {
            // Send connect request
            sendEvent('connect_request', {
                id: 'req_123',
                dAppInfo: { name: 'Test dApp', url: 'https://test.com' }
            });
            
            // Simulate double approval (should be rejected/ignored)
            setTimeout(() => {
                sendEvent('approve_attempt_1', { requestId: 'req_123' });
                sendEvent('approve_attempt_2', { requestId: 'req_123' });
            }, 100);
        }
    };
}

// Scenario #40: Reject after approve
export function rejectAfterApprove() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'connect') {
            sendEvent('connect_request', { id: 'req_456' });
            
            setTimeout(() => {
                sendEvent('approved', { requestId: 'req_456' });
                // Try to reject after approval
                sendEvent('rejected', { requestId: 'req_456', error: 'should_be_ignored' });
            }, 100);
        }
    };
}

// Scenario #41: Approve with invalid wallet address
export function approveWithInvalidAddress() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'approveConnection') {
            // Respond with success but invalid address format
            sendRpcResponse(callId, {
                success: true,
                address: 'invalid_address_format',
                publicKey: '0x1234'
            });
        }
    };
}

// Scenario #42: Transaction request with missing BOC
export function transactionMissingBoc() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            sendEvent('transaction_request', {
                id: 'tx_789',
                // Missing BOC/transaction data
                from: 'EQ...',
                to: 'EQ...',
                amount: '1000000000'
            });
        }
    };
}

// Scenario #43: SignData request with invalid data type
export function signDataInvalidType() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'signData') {
            sendEvent('sign_data_request', {
                id: 'sign_999',
                data: 12345,  // Should be string, not number
                type: null    // Invalid type
            });
        }
    };
}

// Scenario #44: Request timeout without response
export function requestTimeoutNoResponse() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Receive request but never respond
        console.log('Request received but will timeout:', method, callId);
        // No response sent - should timeout
    };
}

// Scenario #45: JS rejects request Android already approved (race)
export function jsRejectsAndroidApproved() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'connect') {
            sendEvent('connect_request', { id: 'req_race' });
            
            // Simulate race: both approve and reject sent nearly simultaneously
            setTimeout(() => {
                sendEvent('approved', { requestId: 'req_race' });
            }, 100);
            
            setTimeout(() => {
                sendEvent('rejected', { requestId: 'req_race', error: 'user_cancelled' });
            }, 105); // Very close timing
        }
    };
}

// Scenario #46: Request with extremely long payload
export function requestExtremelyLongPayload() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            // Create very long comment/payload
            const longComment = 'A'.repeat(100000); // 100KB comment
            const longMessages = [];
            for (let i = 0; i < 500; i++) {
                longMessages.push({
                    to: `EQAddress${i}`,
                    amount: '1000000',
                    comment: longComment
                });
            }
            
            sendEvent('transaction_request', {
                id: 'tx_huge',
                messages: longMessages
            });
        }
    };
}

// Scenario #47: Request callback throws exception
export function requestCallbackThrows() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'approveConnection') {
            // Send response that might cause callback to throw
            sendRpcResponse(callId, {
                success: true,
                causeException: true,
                malformedData: { /* intentionally problematic */ }
            });
        }
    };
}
