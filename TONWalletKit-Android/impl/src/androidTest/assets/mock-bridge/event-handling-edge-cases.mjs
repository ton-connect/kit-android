// Mock JavaScript bridge - Event Handling Edge Cases
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #11: Duplicate event with same event ID
export function duplicateEventSameId() {
    sendReadyEvent();
    
    const eventId = 'evt_duplicate_123';
    const eventData = { type: 'test', message: 'duplicate event' };
    
    // Send same event twice with identical ID
    sendEvent('test_event', eventData, eventId);
    setTimeout(() => {
        sendEvent('test_event', eventData, eventId);
    }, 100);
}

// Scenario #15: Handler throws exception during event processing
export function handlerThrowsException() {
    sendReadyEvent();
    
    // Send event that might cause handler to throw
    sendEvent('error_inducing_event', { 
        shouldThrow: true,
        errorMessage: 'Simulated handler exception'
    });
}

// Scenario #16: Multiple handlers with one throwing exception
export function multipleHandlersOneFails() {
    sendReadyEvent();
    
    // Send event to multiple handlers (one will fail)
    sendEvent('multi_handler_event', {
        testData: 'value',
        handlerCount: 3
    });
}

// Scenario #18: Event with wrong payload type
export function eventWrongPayloadType() {
    sendReadyEvent();
    
    // Send event where payload is wrong type (string instead of object)
    setTimeout(() => {
        if (window.tonwallet && window.tonwallet.send) {
            window.tonwallet.send(JSON.stringify({
                type: 'event',
                event: {
                    id: 'evt_wrong_type',
                    event: 'wallet_changed',
                    payload: "This should be an object, not a string"
                }
            }));
        }
    }, 100);
}

// Scenario #20: Event arrives after SDK destroyed
export function eventAfterSdkDestroyed() {
    sendReadyEvent();
    
    // Simulate SDK destroy signal
    setTimeout(() => {
        sendEvent('sdk_destroying', { reason: 'user_initiated' });
    }, 100);
    
    // Send event after destroy
    setTimeout(() => {
        sendEvent('post_destroy_event', { shouldBeIgnored: true });
    }, 500);
}

// Scenario #21: Same event handler added multiple times
export function sameHandlerAddedMultipleTimes() {
    sendReadyEvent();
    
    // Send event that would trigger duplicate handler registrations
    sendEvent('handler_registration_test', { 
        duplicateRegistration: true,
        expectedCount: 1
    });
}

// Scenario #22: Remove handler that was never added
export function removeUnregisteredHandler() {
    sendReadyEvent();
    
    // Attempt to remove non-existent handler
    sendEvent('handler_removal_test', {
        handlersToRemove: ['non_existent_handler_id']
    });
}
