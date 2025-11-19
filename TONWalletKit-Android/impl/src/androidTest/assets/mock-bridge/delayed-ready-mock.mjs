/**
 * Mock scenario: Delayed ready event
 * Tests SDK handling of slow JavaScript initialization (5 second delay).
 */

import { sendReadyEvent } from './base-mock.mjs';

// Send ready event after significant delay
setTimeout(() => {
    console.log('[Delayed Ready Mock] Sending ready event after 5 seconds');
    sendReadyEvent('testnet');
}, 5000);

window.__walletkitCall = function(method, params, callId) {
    console.log(`[Delayed Ready Mock] Call before ready: ${method}`);
};
