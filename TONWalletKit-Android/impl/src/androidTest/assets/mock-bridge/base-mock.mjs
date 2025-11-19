/**
 * Base Mock Bridge - Provides common utilities for all mock scenarios
 * 
 * This simulates the minimal WalletKit JavaScript bridge behavior needed for testing.
 * Each test scenario can extend this base to implement specific behaviors.
 */

// Global state
window.__walletkit_mock_state = {
  ready: false,
  network: 'testnet',
  tonApiUrl: 'https://testnet.tonapi.io',
  eventListenersRegistered: false,
  pendingCalls: new Map(),
  sessionStorage: new Map()
};

/**
 * Send message to Android via the JavascriptInterface
 */
function sendToAndroid(payload) {
  const message = JSON.stringify(payload);
  console.log('[MOCK] Sending to Android:', message);
  
  // The SDK exposes WalletKitNative.postMessage() as the bridge
  if (window.WalletKitNative && window.WalletKitNative.postMessage) {
    window.WalletKitNative.postMessage(message);
  } else {
    console.error('[MOCK] WalletKitNative.postMessage not available!');
  }
}

/**
 * Send ready event
 */
export function sendReadyEvent(network = 'testnet', tonApiUrl = 'https://testnet.tonapi.io') {
  window.__walletkit_mock_state.ready = true;
  window.__walletkit_mock_state.network = network;
  window.__walletkit_mock_state.tonApiUrl = tonApiUrl;
  
  sendToAndroid({
    kind: 'ready',
    network: network,
    tonApiUrl: tonApiUrl
  });
}

/**
 * Send RPC response
 */
export function sendRpcResponse(id, result = {}, error = null) {
  const payload = {
    kind: 'response',
    id: id
  };
  
  if (error) {
    payload.error = error;
  } else {
    payload.result = result;
  }
  
  sendToAndroid(payload);
}

/**
 * Send event
 */
export function sendEvent(eventType, eventData) {
  sendToAndroid({
    kind: 'event',
    event: {
      type: eventType,
      data: eventData,
      id: `event-${Date.now()}-${Math.random()}`
    }
  });
}

/**
 * Main bridge function called by Android
 */
window.__walletkitCall = function(id, method, params) {
  console.log(`[MOCK] __walletkitCall: id=${id}, method=${method}`, params);
  
  window.__walletkit_mock_state.pendingCalls.set(id, { method, params, timestamp: Date.now() });
  
  // Let the specific mock implementation handle the method
  if (window.__walletkitMockHandler) {
    window.__walletkitMockHandler(id, method, params);
  } else {
    // Default: just echo success
    sendRpcResponse(id, { success: true });
  }
};

/**
 * Storage adapter (called by Android for persistence)
 */
window.WalletKitNative = window.WalletKitNative || {
  getItem: function(key) {
    const value = window.__walletkit_mock_state.sessionStorage.get(key);
    console.log(`[MOCK] Storage.getItem: ${key} = ${value}`);
    return value || null;
  },
  
  setItem: function(key, value) {
    console.log(`[MOCK] Storage.setItem: ${key} = ${value}`);
    window.__walletkit_mock_state.sessionStorage.set(key, value);
  },
  
  removeItem: function(key) {
    console.log(`[MOCK] Storage.removeItem: ${key}`);
    window.__walletkit_mock_state.sessionStorage.delete(key);
  },
  
  onMessage: function(message) {
    console.log(`[MOCK] Android received: ${message}`);
  }
};

console.log('[MOCK] Base bridge initialized');
