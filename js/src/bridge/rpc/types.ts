import type { WalletKitBridgeEvent } from '../../types';

/**
 * RPC Protocol Type Definitions
 * Defines the message structure for communication between Android native and JS bridge
 */

export type WalletKitApiMethod = string;

export interface CallContext {
  id: string;
  method: WalletKitApiMethod;
}

export type BridgePayload =
  | { kind: 'response'; id: string; result?: unknown; error?: { message: string } }
  | { kind: 'event'; event: WalletKitBridgeEvent }
  | {
      kind: 'ready';
      network?: string;
      tonApiUrl?: string;
      tonClientEndpoint?: string;
      source?: string;
      timestamp?: number;
    }
  | {
      kind: 'diagnostic-call';
      id: string;
      method: WalletKitApiMethod;
      stage: 'start' | 'checkpoint' | 'success' | 'error';
      timestamp: number;
      message?: string;
    }
  | { kind: 'jsBridgeEvent'; sessionId: string; event: any };

export interface RPCRequest {
  id: string;
  method: WalletKitApiMethod;
  params?: unknown;
}

export interface RPCResponse {
  id: string;
  result?: unknown;
  error?: { message: string };
}
