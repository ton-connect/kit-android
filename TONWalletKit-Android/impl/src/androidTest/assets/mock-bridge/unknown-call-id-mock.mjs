/**
 * Edge Case: RPC Response for Unknown Call ID
 * 
 * Scenario #4: JS sends response with call ID that has no pending deferred in Android
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

setTimeout(() => {
  sendReadyEvent('testnet', 'https://testnet.tonapi.io');
  
  // Send response for non-existent call ID
  setTimeout(() => {
    console.log('[UNKNOWN_ID] Sending response for unknown call ID');
    sendRpcResponse('non-existent-call-id-12345', { data: 'surprise!' });
  }, 200);
}, 100);

window.__walletkitMockHandler = function(id, method, params) {
  // Also send valid response
  sendRpcResponse(id, { success: true });
  
  // But then send ANOTHER response with wrong ID
  setTimeout(() => {
    sendRpcResponse(id + '-wrong', { error: 'duplicate' });
  }, 100);
};

console.log('[UNKNOWN_ID] Mock initialized');
