/**
 * Edge Case: Malformed JSON in RPC Response
 * 
 * Scenario #5: JS sends response that cannot be parsed as valid JSON
 */

import { sendReadyEvent } from './base-mock.mjs';

setTimeout(() => {
  sendReadyEvent('testnet', 'https://testnet.tonapi.io');
}, 100);

window.__walletkitMockHandler = function(id, method, params) {
  console.log('[MALFORMED] Sending malformed JSON');
  
  // Send invalid JSON directly
  if (window.WalletKitNative && window.WalletKitNative.onMessage) {
    // Send string that's NOT valid JSON
    window.WalletKitNative.onMessage('{invalid json, missing quotes: true}');
    
    // Also send truncated JSON
    setTimeout(() => {
      window.WalletKitNative.onMessage('{"kind":"response","id":"' + id + '",');
    }, 50);
  }
};

console.log('[MALFORMED] Mock initialized');
