/**
 * Edge Case: Multiple Rapid Ready Events
 * 
 * Scenario #23: Multiple "ready" events from JS (Bridge ready event fires multiple times)
 * Tests SDK's ability to handle JS context reload/recovery
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

// Send first ready event
setTimeout(() => {
  console.log('[MULTI_READY] Sending first ready event (testnet)');
  sendReadyEvent('testnet', 'https://testnet.tonapi.io');
}, 100);

// Send second ready event with DIFFERENT network
setTimeout(() => {
  console.log('[MULTI_READY] Sending second ready event (mainnet)');
  sendReadyEvent('mainnet', 'https://tonapi.io');
}, 500);

// Send third ready event (back to testnet)
setTimeout(() => {
  console.log('[MULTI_READY] Sending third ready event (testnet again)');
  sendReadyEvent('testnet', 'https://testnet.tonapi.io');
}, 1000);

// Handle RPC calls normally
window.__walletkitMockHandler = function(id, method, params) {
  sendRpcResponse(id, { success: true });
};

console.log('[MULTI_READY] Mock initialized');
