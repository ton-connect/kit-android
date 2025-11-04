/**
 * Event Forwarding Module  
 * Manages event listeners for WalletKit events
 */

import { getWalletKit } from '../core/initialization';

// Event listener references (stored at module level like iOS pattern)
let onConnectListener: ((event: any) => void) | null = null;
let onTransactionListener: ((event: any) => void) | null = null;
let onSignDataListener: ((event: any) => void) | null = null;
let onDisconnectListener: ((event: any) => void) | null = null;

/**
 * Set up event listeners
 */
export function setEventsListeners(callback: (type: string, event: any) => void) {
  const walletKit = getWalletKit();
  console.log('[eventForwarding] Setting up event listeners');
  
  // Remove old listeners if they exist
  if (onConnectListener) {
    walletKit.removeConnectRequestCallback();
  }
  
  onConnectListener = (event: any) => {
    console.log('[eventForwarding] Connect request received');
    callback('connectRequest', event);
  };
  
  walletKit.onConnectRequest(onConnectListener);
  
  // Transaction listener
  if (onTransactionListener) {
    walletKit.removeTransactionRequestCallback();
  }
  
  onTransactionListener = (event: any) => {
    console.log('[eventForwarding] Transaction request received');
    callback('transactionRequest', event);
  };
  
  walletKit.onTransactionRequest(onTransactionListener);
  
  // Sign data listener
  if (onSignDataListener) {
    walletKit.removeSignDataRequestCallback();
  }
  
  onSignDataListener = (event: any) => {
    console.log('[eventForwarding] Sign data request received');
    callback('signDataRequest', event);
  };
  
  walletKit.onSignDataRequest(onSignDataListener);
  
  // Disconnect listener
  if (onDisconnectListener) {
    walletKit.removeDisconnectCallback();
  }
  
  onDisconnectListener = (event: any) => {
    console.log('[eventForwarding] Disconnect event received');
    callback('disconnect', event);
  };
  
  walletKit.onDisconnect(onDisconnectListener);
  
  console.log('[eventForwarding] Event listeners set up successfully');
  return { ok: true };
}

/**
 * Remove all event listeners
 */
export function removeEventListeners() {
  const walletKit = getWalletKit();
  console.log('[eventForwarding] Removing all event listeners');
  
  if (onConnectListener) {
    walletKit.removeConnectRequestCallback();
    onConnectListener = null;
  }
  
  if (onTransactionListener) {
    walletKit.removeTransactionRequestCallback();
    onTransactionListener = null;
  }
  
  if (onSignDataListener) {
    walletKit.removeSignDataRequestCallback();
    onSignDataListener = null;
  }
  
  if (onDisconnectListener) {
    walletKit.removeDisconnectCallback();
    onDisconnectListener = null;
  }
  
  console.log('[eventForwarding] All event listeners removed');
  return { ok: true };
}
