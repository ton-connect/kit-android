/**
 * Edge Case: Event Flooding
 * 
 * Scenario #12: JS sends 100+ events within milliseconds
 * Tests SDK's ability to handle rapid event succession
 */

import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

setTimeout(() => {
  sendReadyEvent('testnet', 'https://testnet.tonapi.io');
  
  // After ready, flood with events
  setTimeout(() => {
    console.log('[FLOODING] Starting event flood...');
    
    for (let i = 0; i < 150; i++) {
      sendEvent('testEvent', {
        index: i,
        timestamp: Date.now(),
        data: `Event ${i}`
      });
    }
    
    console.log('[FLOODING] Sent 150 events');
  }, 200);
}, 100);

window.__walletkitMockHandler = function(id, method, params) {
  sendRpcResponse(id, { success: true });
};

console.log('[FLOODING] Mock initialized');
