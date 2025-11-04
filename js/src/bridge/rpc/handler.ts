/**
 * RPC Handler
 * Handles RPC protocol communication with Android native
 */

import type { BridgePayload, WalletKitApiMethod } from './types';
import { resolveGlobalScope, resolveNativeBridge, bigIntReplacer } from '../utils/helpers';

/**
 * Post message to native Android
 */
export function postToNative(payload: BridgePayload) {
  try {
    const scope = resolveGlobalScope();
    const bridge = resolveNativeBridge(scope);
    const message = JSON.stringify(payload, bigIntReplacer);
    bridge.postMessage(message);
  } catch (error) {
    console.error('[rpcHandler] Failed to post to native:', error);
  }
}

/**
 * Send RPC response
 */
export function respond(id: string, result?: unknown, error?: { message: string }) {
  postToNative({
    kind: 'response',
    id,
    result,
    error,
  });
}

/**
 * Emit diagnostic/checkpoint message
 */
export function emitDiagnostic(
  id: string,
  method: WalletKitApiMethod,
  stage: 'start' | 'checkpoint' | 'success' | 'error',
  message?: string
) {
  postToNative({
    kind: 'diagnostic-call',
    id,
    method,
    stage,
    timestamp: Date.now(),
    message,
  });
}

/**
 * Handle RPC method call
 */
export async function handleCall(
  id: string,
  method: WalletKitApiMethod,
  params: unknown,
  apiMethods: Record<string, (args?: any) => Promise<any>>
) {
  console.log(`[rpcHandler] Calling ${method}`);
  emitDiagnostic(id, method, 'start');

  try {
    const fn = apiMethods[method];
    if (!fn) {
      throw new Error(`Unknown method: ${method}`);
    }

    const result = await fn(params);
    emitDiagnostic(id, method, 'success');
    respond(id, result);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`[rpcHandler] Error in ${method}:`, message);
    emitDiagnostic(id, method, 'error', message);
    respond(id, undefined, { message });
  }
}
