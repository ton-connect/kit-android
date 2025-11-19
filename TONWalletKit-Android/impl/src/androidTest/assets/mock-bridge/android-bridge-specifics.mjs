// Mock JavaScript bridge - Android Bridge Specifics Edge Cases
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #116: __walletkitCall invoked before bridge ready
export function walletkitCallBeforeReady() {
    // Don't send ready yet
    // Simulate Android calling __walletkitCall before JS is ready
    setTimeout(() => {
        sendEvent('premature_call_detected', { 
            error: 'Bridge not ready',
            timestamp: Date.now() 
        });
        // Finally send ready
        sendReadyEvent();
    }, 200);
}

// Scenario #117: JavaScript exception during method call
export function jsExceptionDuringCall() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Throw JavaScript exception
        throw new Error('Simulated JavaScript exception in dispatchRpc');
    };
}

// Scenario #119: Response missing kind field
export function responseMissingKind() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Send malformed response without 'kind' field
        if (window.tonwallet && window.tonwallet.send) {
            window.tonwallet.send(JSON.stringify({
                type: 'response',
                // Missing 'kind' field
                id: callId,
                result: { data: 'some data' }
            }));
        }
    };
}

// Scenario #120: Response kind is unknown value
export function responseUnknownKind() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (window.tonwallet && window.tonwallet.send) {
            window.tonwallet.send(JSON.stringify({
                type: 'response',
                kind: 'future_unknown_kind_from_newer_version',
                id: callId,
                result: { data: 'unknown kind response' }
            }));
        }
    };
}

// Scenario #121: Event listener setup fails in JS
export function eventListenerSetupFails() {
    sendReadyEvent();
    
    // Simulate listener setup failure
    window.setEventsListeners = function() {
        throw new Error('Failed to set up event listeners');
    };
}

// Scenario #123: Storage adapter bridge not available
export function storageAdapterNotAvailable() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'getStorageValue') {
            sendRpcResponse(callId, {
                error: 'WalletKitNative not available',
                success: false
            });
        }
    };
}

// Scenario #124: Storage get returns null unexpectedly
export function storageGetReturnsNull() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'getStorageValue') {
            sendRpcResponse(callId, {
                value: null,
                found: false
            });
        }
    };
}

// Scenario #125: Storage set throws exception
export function storageSetThrows() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'setStorageValue') {
            sendRpcResponse(callId, {
                success: false,
                error: 'Storage write failed',
                exception: 'SecurityException: Permission denied'
            });
        }
    };
}
