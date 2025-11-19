// Mock JavaScript bridge scenarios for concurrency issues
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #60: Race condition in parallel calls
export function raceConditionParallelCalls() {
    sendReadyEvent();
    
    const callQueue = [];
    window.dispatchRpc = function(method, params, callId) {
        callQueue.push({ method, params, callId });
        
        // Process all queued calls simultaneously
        if (callQueue.length > 1) {
            // Respond in reverse order to simulate race
            for (let i = callQueue.length - 1; i >= 0; i--) {
                const call = callQueue[i];
                sendRpcResponse(call.callId, { 
                    method: call.method,
                    order: i,
                    reversed: true
                });
            }
            callQueue.length = 0;
        }
    };
}

// Scenario #61: Concurrent wallet operations
export function concurrentWalletOperations() {
    sendReadyEvent();
    
    let activeOperations = 0;
    window.dispatchRpc = function(method, params, callId) {
        activeOperations++;
        
        if (activeOperations > 3) {
            sendRpcResponse(callId, { 
                success: false, 
                error: 'too_many_concurrent_operations',
                active: activeOperations
            });
        } else {
            setTimeout(() => {
                sendRpcResponse(callId, { 
                    success: true, 
                    concurrent_count: activeOperations 
                });
                activeOperations--;
            }, 100);
        }
    };
}

// Scenario #62: Deadlock detection
export function deadlockScenario() {
    sendReadyEvent();
    
    const locks = new Set();
    window.dispatchRpc = function(method, params, callId) {
        const resource = params?.resource || 'default';
        
        if (locks.has(resource)) {
            // Deadlock detected
            sendEvent('deadlock_detected', { resource, callId });
            sendRpcResponse(callId, { 
                success: false, 
                error: 'resource_locked',
                resource
            });
        } else {
            locks.add(resource);
            setTimeout(() => {
                locks.delete(resource);
                sendRpcResponse(callId, { success: true });
            }, 500);
        }
    };
}

// Scenario #63: Thread safety violation
export function threadSafetyViolation() {
    sendReadyEvent();
    
    let sharedState = 0;
    window.dispatchRpc = function(method, params, callId) {
        // Simulate non-atomic operations
        const temp = sharedState;
        setTimeout(() => {
            sharedState = temp + 1;
            sendRpcResponse(callId, { 
                state: sharedState,
                potential_race: true
            });
        }, Math.random() * 50);
    };
}

// Scenario #64: Event handler recursion
export function eventHandlerRecursion() {
    sendReadyEvent();
    
    let recursionDepth = 0;
    window.dispatchRpc = function(method, params, callId) {
        recursionDepth++;
        
        if (recursionDepth < 5) {
            // Trigger recursive event
            sendEvent('recursive_event', { depth: recursionDepth });
        } else {
            sendEvent('recursion_limit_reached', { maxDepth: recursionDepth });
        }
        
        sendRpcResponse(callId, { depth: recursionDepth });
        recursionDepth = 0;
    };
}
