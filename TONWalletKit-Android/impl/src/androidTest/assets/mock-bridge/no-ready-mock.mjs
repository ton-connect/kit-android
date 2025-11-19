/**
 * Mock scenario: No ready event
 * Tests SDK behavior when JavaScript bridge never sends ready event.
 */

// Don't send ready event at all
// Just set up the RPC handler
window.__walletkitCall = function(method, params, callId) {
    console.log(`[No Ready Mock] Received call before ready: ${method} (${callId})`);
    // Don't respond - bridge not ready
};

console.log('[No Ready Mock] Bridge loaded but not sending ready event');
