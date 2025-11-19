// Mock JavaScript bridge scenarios for data validation
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #70: Invalid wallet address format
export function invalidWalletAddress() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'sendTransaction') {
            sendRpcResponse(callId, { 
                success: false, 
                error: 'invalid_address',
                address: params?.to,
                expected_format: 'EQ...'
            });
        }
    };
}

// Scenario #71: Negative amount value
export function negativeAmountValue() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (params?.amount < 0) {
            sendRpcResponse(callId, { 
                success: false, 
                error: 'invalid_amount',
                value: params.amount,
                message: 'Amount must be positive'
            });
        }
    };
}

// Scenario #72: Null/undefined parameters
export function nullParameters() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (params === null || params === undefined) {
            sendRpcResponse(callId, { 
                success: false, 
                error: 'missing_parameters',
                received: params
            });
        } else {
            sendRpcResponse(callId, { success: true });
        }
    };
}

// Scenario #73: String injection in JSON
export function stringInjection() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Send response with injection attempt
        const maliciousString = '","injected":"value","hack":"';
        sendRpcResponse(callId, { 
            data: maliciousString,
            sanitized: false
        });
    };
}

// Scenario #74: Integer overflow
export function integerOverflow() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, { 
            amount: Number.MAX_SAFE_INTEGER + 1000,
            overflow: true,
            warning: 'Value exceeds safe integer range'
        });
    };
}

// Scenario #75: Unicode edge cases
export function unicodeEdgeCases() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, { 
            message: '👾🚀💎 Test with emojis and \\u0000 null chars',
            rtl: 'مرحبا hello שלום',
            combining: 'e\\u0301', // é with combining accent
            surrogate: '𝕳𝖊𝖑𝖑𝖔'
        });
    };
}
