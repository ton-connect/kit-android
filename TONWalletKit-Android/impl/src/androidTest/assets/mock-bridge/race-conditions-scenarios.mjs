// Mock JavaScript bridge - Race Conditions
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #126: Event arrives before listeners set up
export function eventBeforeListenersSetup() {
    // Send events IMMEDIATELY before ready
    sendEvent('premature_event_1', { timestamp: Date.now() });
    sendEvent('premature_event_2', { timestamp: Date.now() });
    
    // Then send ready
    setTimeout(() => {
        sendReadyEvent();
    }, 100);
}

// Scenario #127: Listener removed during event dispatch
export function listenerRemovedDuringDispatch() {
    sendReadyEvent();
    
    // Send event that triggers listener removal
    setTimeout(() => {
        sendEvent('trigger_removal', { removeListener: true });
        // Send another event immediately - listener might be partially removed
        sendEvent('after_removal', { shouldBeIgnored: false });
    }, 100);
}

// Scenario #128: Init completes after destroy started
export function initCompletesAfterDestroy() {
    // Start init but delay ready
    setTimeout(() => {
        sendEvent('init_started', { timestamp: Date.now() });
    }, 100);
    
    // Signal destroy before init completes
    setTimeout(() => {
        sendEvent('destroy_started', { timestamp: Date.now() });
    }, 200);
    
    // Init completes after destroy
    setTimeout(() => {
        sendReadyEvent();
        sendEvent('init_completed_late', { afterDestroy: true });
    }, 300);
}

// Scenario #129: Multiple ready events race with init
export function readyRacesWithInit() {
    // Send multiple ready events in rapid succession during init
    setTimeout(() => {
        sendReadyEvent();
    }, 50);
    
    setTimeout(() => {
        sendReadyEvent();
    }, 60);
    
    setTimeout(() => {
        sendReadyEvent();
    }, 70);
}

// Scenario #130: Session list modified during iteration
export function sessionModifiedDuringRead() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'getSessions') {
            // Start responding with session list
            const sessions = [
                { id: 'session1', dApp: 'app1' },
                { id: 'session2', dApp: 'app2' },
                { id: 'session3', dApp: 'app3' }
            ];
            
            sendRpcResponse(callId, { sessions });
            
            // Immediately modify sessions (simulate concurrent modification)
            setTimeout(() => {
                sendEvent('session_added', { id: 'session4' });
                sendEvent('session_removed', { id: 'session2' });
            }, 10);
        }
    };
}
