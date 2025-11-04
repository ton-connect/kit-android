/**
 * Android WalletKit Bridge
 * Main entry point for the layered bridge architecture
 * Thin wrapper that delegates to @ton/walletkit core
 */

import type { WalletKitBridgeEvent, WalletKitBridgeInitConfig } from '../types';

// Core
import { init as initWalletKit } from './core/initialization';
import { setEventsListeners, removeEventListeners } from './core/eventForwarding';

// API Operations
import * as walletOps from './api/walletOperations';
import * as transactionOps from './api/transactionOperations';
import * as connectionOps from './api/connectionOperations';
import * as jettonOps from './api/jettonOperations';
import * as sessionOps from './api/sessionOperations';

// RPC
import { handleCall, postToNative } from './rpc/handler';

/**
 * Emit event to Android
 */
function emit(type: WalletKitBridgeEvent['type'], data?: WalletKitBridgeEvent['data']) {
  postToNative({
    kind: 'event',
    event: { type, data } as WalletKitBridgeEvent,
  });
}

/**
 * API methods exposed to Android
 * Following the iOS pattern: thin delegation to core
 */
const api = {
  // Initialization
  async init(config?: WalletKitBridgeInitConfig) {
    console.log('[bridge] Initializing WalletKit');
    const result = await initWalletKit(config, (readyDetails) => {
      emit('ready', readyDetails);
      postToNative({ kind: 'ready', ...readyDetails });
    });
    return result;
  },

  // Event Management
  async setEventsListeners(args?: { callback?: (type: string, event: any) => void }) {
    const callback = args?.callback || ((type: string, event: any) => {
      emit(type as any, event);
    });
    return setEventsListeners(callback);
  },

  async removeEventListeners() {
    return removeEventListeners();
  },

  // Wallet Operations
  async createTonMnemonic(args?: { count?: number }) {
    return walletOps.createTonMnemonic(args);
  },

  async derivePublicKeyFromMnemonic(args: { mnemonic: string[] }) {
    return walletOps.derivePublicKeyFromMnemonic(args);
  },

  async signDataWithMnemonic(args: { words: string[]; data: number[]; mnemonicType?: 'ton' | 'bip39' }) {
    return walletOps.signDataWithMnemonic(args);
  },

  async createV4R2WalletUsingMnemonic(args: { mnemonic: string[]; network?: string }) {
    return walletOps.createV4R2WalletUsingMnemonic(args);
  },

  async createV4R2WalletUsingSecretKey(args: { secretKey: string; network?: string }) {
    return walletOps.createV4R2WalletUsingSecretKey(args);
  },

  async createV5R1WalletUsingMnemonic(args: { mnemonic: string[]; network?: string }) {
    return walletOps.createV5R1WalletUsingMnemonic(args);
  },

  async createV5R1WalletUsingSecretKey(args: { secretKey: string; network?: string }) {
    return walletOps.createV5R1WalletUsingSecretKey(args);
  },

  async addWallet(args: { wallet: any }) {
    return walletOps.addWallet(args);
  },

  async getWallets() {
    return walletOps.getWallets();
  },

  async getWalletBalance(args: { address: string }) {
    return walletOps.getWalletBalance(args);
  },

  async removeWallet(args: { address: string }) {
    return walletOps.removeWallet(args);
  },

  // Transaction Operations
  async sendLocalTransaction(args: {
    walletAddress: string;
    toAddress: string;
    amount: string;
    comment?: string;
  }) {
    return transactionOps.sendLocalTransaction(args);
  },

  async sendTransaction(args: { walletAddress: string; transactionContent: string }) {
    return transactionOps.sendTransaction(args);
  },

  async getRecentTransactions(args: { address: string; limit?: number; offset?: number }) {
    return transactionOps.getRecentTransactions(args);
  },

  async approveTransactionRequest(args: { event: any }) {
    return transactionOps.approveTransactionRequest(args);
  },

  async rejectTransactionRequest(args: { event: any; reason?: string }) {
    return transactionOps.rejectTransactionRequest(args);
  },

  // Connection Operations
  async handleTonConnectUrl(args: unknown) {
    return connectionOps.handleTonConnectUrl(args);
  },

  async approveConnectRequest(args: { event: any; walletAddress: string }) {
    return connectionOps.approveConnectRequest(args);
  },

  async rejectConnectRequest(args: { event: any; reason?: string }) {
    return connectionOps.rejectConnectRequest(args);
  },

  async approveSignDataRequest(args: { event: any }) {
    return connectionOps.approveSignDataRequest(args);
  },

  async rejectSignDataRequest(args: { event: any; reason?: string }) {
    return connectionOps.rejectSignDataRequest(args);
  },

  async processInternalBrowserRequest(args: { 
    messageId: string; 
    method: string; 
    params?: unknown; 
    url?: string; 
    manifestUrl?: string 
  }) {
    return connectionOps.processInternalBrowserRequest(args);
  },

  // Session Operations
  async listSessions() {
    return sessionOps.listSessions();
  },

  async disconnectSession(args?: { sessionId?: string }) {
    return sessionOps.disconnectSession(args);
  },

  // Jetton Operations
  async getJettons(args: { address: string; limit?: number; offset?: number }) {
    return jettonOps.getJettons(args);
  },

  async getJetton(args: { jettonAddress: string }) {
    return jettonOps.getJetton(args);
  },

  async createTransferJettonTransaction(args: {
    address: string;
    jettonAddress: string;
    amount: string;
    toAddress: string;
    comment?: string;
  }) {
    return jettonOps.createTransferJettonTransaction(args);
  },

  async getJettonBalance(args: { address: string; jettonAddress: string }) {
    return jettonOps.getJettonBalance(args);
  },

  async getJettonWalletAddress(args: { address: string; jettonAddress: string }) {
    return jettonOps.getJettonWalletAddress(args);
  },

  // NFT Operations
  async getNfts(args: { address: string; limit?: number; offset?: number }) {
    return jettonOps.getNfts(args);
  },

  async getNft(args: { address: string }) {
    return jettonOps.getNft(args);
  },

  async createTransferNftTransaction(args: {
    address: string;
    nftAddress: string;
    transferAmount: string;
    toAddress: string;
    comment?: string;
  }) {
    return jettonOps.createTransferNftTransaction(args);
  },

  async createTransferNftRawTransaction(args: {
    address: string;
    nftAddress: string;
    transferAmount: string;
    transferMessage: any;
  }) {
    return jettonOps.createTransferNftRawTransaction(args);
  },

  // Browser Events
  async emitBrowserBridgeRequest(args: { messageId: string; method: string; request: string }) {
    emit('browserBridgeRequest', {
      messageId: args.messageId,
      method: args.method,
      request: args.request,
    });
    return { success: true };
  },

  async emitBrowserPageStarted(args: { url: string }) {
    emit('browserPageStarted', { url: args.url });
    return { success: true };
  },

  async emitBrowserPageFinished(args: { url: string }) {
    emit('browserPageFinished', { url: args.url });
    return { success: true };
  },

  async emitBrowserError(args: { message: string }) {
    emit('browserError', { message: args.message });
    return { success: true };
  },
};

/**
 * Set up RPC handler on window
 */
if (typeof window !== 'undefined') {
  (window as any).__walletkitCall = async (id: string, method: string, paramsJson?: string | null) => {
    // Parse JSON params if provided (comes as JSON string from Kotlin via atob)
    let params: unknown = undefined;
    if (paramsJson && paramsJson !== 'null') {
      try {
        // First parse: paramsJson may itself be a JSON-encoded string (Android
        // sends JSONObject.toString() which may be quoted). After JSON.parse we
        // might get either the actual params object or a JSON string. If it's a
        // string that looks like JSON, attempt a secondary parse.
        let parsed = JSON.parse(paramsJson as string);

        if (typeof parsed === 'string') {
          const trimmed = parsed.trim();
          if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
            try {
              parsed = JSON.parse(parsed);
            } catch (err2) {
              console.warn('[bridge] Second parse of params string failed:', err2);
            }
          }
        }

        params = parsed;
      } catch (err) {
        console.error('[bridge] Failed to parse params JSON:', err);
        postToNative({
          kind: 'response',
          id,
          error: { message: 'Invalid params JSON' },
        });
        return;
      }
    }

    // Diagnostic logs to help ensure mnemonic arrays arrive as proper JS arrays
    try {
      console.log('[bridge] Parsed params type:', typeof params);
      console.log('[bridge] Parsed params preview:', JSON.stringify(params, null, 0));
    } catch (e) {
      // ignore logging errors
    }

    await handleCall(id, method, params, api);
  };
}

// Export API for testing
export default api;
