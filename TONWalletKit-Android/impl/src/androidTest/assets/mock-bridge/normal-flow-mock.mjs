/**
 * Normal Flow Mock - Simulates expected/happy path behavior
 * 
 * Covers scenarios:
 * - Normal ready event
 * - Successful RPC calls
 * - Normal event emission
 * - Standard connect/disconnect flow
 */

import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Auto-send ready event on load
setTimeout(() => {
  console.log('[NORMAL_FLOW] Sending ready event');
  sendReadyEvent('testnet', 'https://testnet.tonapi.io');
}, 100);

// Handle RPC calls
window.__walletkitMockHandler = function(id, method, params) {
  console.log(`[NORMAL_FLOW] Handling ${method}`);
  
  switch (method) {
    case 'init':
      sendRpcResponse(id, {
        network: 'testnet',
        tonApiUrl: 'https://testnet.tonapi.io'
      });
      break;
      
    case 'setEventsListeners':
      window.__walletkit_mock_state.eventListenersRegistered = true;
      sendRpcResponse(id, { success: true });
      break;
      
    case 'getWallets':
      sendRpcResponse(id, { wallets: [] });
      break;
      
    case 'getSessions':
      sendRpcResponse(id, { sessions: [] });
      break;
      
    case 'disconnect':
      const sessionId = params?.sessionId;
      if (sessionId) {
        // Send disconnect event
        setTimeout(() => {
          sendEvent('disconnect', { sessionId });
        }, 50);
      }
      sendRpcResponse(id, { success: true });
      break;
      
    default:
      sendRpcResponse(id, { success: true });
  }
};

console.log('[NORMAL_FLOW] Mock initialized');
