// Mock JavaScript bridge - Bridge Ready State Edge Cases
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #24: Ready event with conflicting network config
export function readyConflictingNetwork() {
    // Send first ready with testnet
    setTimeout(() => {
        if (window.tonwallet && window.tonwallet.send) {
            window.tonwallet.send(JSON.stringify({
                type: 'event',
                event: {
                    id: 'ready_1',
                    event: 'ready',
                    payload: {
                        network: 'testnet',
                        apiBaseUrl: 'https://testnet.tonapi.io'
                    }
                }
            }));
        }
    }, 100);
    
    // Send second ready with mainnet (conflict!)
    setTimeout(() => {
        if (window.tonwallet && window.tonwallet.send) {
            window.tonwallet.send(JSON.stringify({
                type: 'event',
                event: {
                    id: 'ready_2',
                    event: 'ready',
                    payload: {
                        network: 'mainnet',
                        apiBaseUrl: 'https://tonapi.io'
                    }
                }
            }));
        }
    }, 200);
}

// Scenario #25: JS context lost and recovered
export function jsContextLostRecovered() {
    sendReadyEvent();
    
    // Simulate context loss
    setTimeout(() => {
        sendEvent('context_lost', { reason: 'webview_reload' });
    }, 100);
    
    // Simulate recovery
    setTimeout(() => {
        sendReadyEvent(); // Send ready again after recovery
        sendEvent('context_recovered', { success: true });
    }, 500);
}

// Scenario #26: Init called multiple times concurrently
export function initCalledConcurrently() {
    // Don't send ready immediately
    // Simulate multiple concurrent init attempts
    setTimeout(() => {
        sendEvent('init_attempt', { attemptId: 1 });
        sendEvent('init_attempt', { attemptId: 2 });
        sendEvent('init_attempt', { attemptId: 3 });
        
        // Finally send ready
        sendReadyEvent();
    }, 200);
}

// Scenario #27: Init called after destroy
export function initAfterDestroy() {
    sendReadyEvent();
    
    // Signal destroy
    setTimeout(() => {
        sendEvent('destroy_started', { timestamp: Date.now() });
    }, 100);
    
    // Attempt init after destroy
    setTimeout(() => {
        sendEvent('init_after_destroy', { shouldFail: true });
        // Don't send another ready - this should be rejected
    }, 300);
}

// Scenario #30: Init with invalid configuration
export function initInvalidConfig() {
    // Send ready with invalid/missing config
    setTimeout(() => {
        if (window.tonwallet && window.tonwallet.send) {
            window.tonwallet.send(JSON.stringify({
                type: 'event',
                event: {
                    id: 'ready_invalid',
                    event: 'ready',
                    payload: {
                        network: null,  // Invalid
                        apiBaseUrl: '',  // Empty
                        version: undefined
                    }
                }
            }));
        }
    }, 100);
}
