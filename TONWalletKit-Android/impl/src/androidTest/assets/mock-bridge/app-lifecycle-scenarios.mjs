// Mock JavaScript bridge scenarios for app lifecycle
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #90: App backgrounded during operation
export function appBackgrounded() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Simulate app going to background
        setTimeout(() => {
            sendEvent('app_backgrounded', { timestamp: Date.now() });
        }, 100);
        
        // Complete operation anyway
        setTimeout(() => {
            sendRpcResponse(callId, { 
                success: true, 
                completed_in_background: true 
            });
        }, 500);
    };
}

// Scenario #91: App killed and restored
export function appKilledRestore() {
    // Simulate app killed - no ready event initially
    setTimeout(() => {
        // After "restore"
        sendReadyEvent();
        sendEvent('app_restored', { 
            reason: 'killed_by_system',
            state_restored: false 
        });
    }, 1500);
}

// Scenario #92: Low memory warning
export function lowMemoryWarning() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('low_memory_warning', { 
            level: 'critical',
            available_mb: 50 
        });
        
        // Reduce functionality
        sendRpcResponse(callId, { 
            success: true, 
            limited_mode: true,
            message: 'Operating in reduced memory mode'
        });
    };
}

// Scenario #93: Screen orientation change
export function orientationChange() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('orientation_changed', { 
            from: 'portrait',
            to: 'landscape' 
        });
        
        // May cause WebView reload
        setTimeout(() => {
            sendReadyEvent(); // Re-initialize
            sendRpcResponse(callId, { 
                success: true, 
                reloaded: true 
            });
        }, 200);
    };
}

// Scenario #94: App permission revoked
export function permissionRevoked() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('permission_revoked', { 
            permission: 'storage',
            impact: 'high' 
        });
        
        sendRpcResponse(callId, { 
            success: false, 
            error: 'permission_denied',
            required_permission: 'storage'
        });
    };
}
