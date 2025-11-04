/**
 * Connection Operations API
 * Handles TonConnect connection and signing requests
 * Delegates to @ton/walletkit core
 */

import { getWalletKit, ensureWalletKitLoaded } from '../core/initialization';
import { required } from '../utils/validators';
import { resolveTonConnectUrl } from '../utils/helpers';

/**
 * Handle TonConnect URL (parse and initiate connection)
 */
export async function handleTonConnectUrl(args: unknown) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const url = resolveTonConnectUrl(args);
  if (!url) {
    throw new Error('TON Connect URL is missing');
  }

  console.log('[connectionOperations] Handling TonConnect URL:', url);
  const result = await walletKit.handleTonConnectUrl(url);
  
  return result;
}

/**
 * Approve connect request
 */
export async function approveConnectRequest(args: { event: any; walletAddress: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  required(args.event, 'event');
  required(args.walletAddress, 'walletAddress');

  // Ensure wallet kit is fully initialized
  if (typeof walletKit.ensureInitialized === 'function') {
    await walletKit.ensureInitialized();
  }

  const wallet = walletKit.getWallet?.(args.walletAddress);
  if (!wallet) {
    throw new Error('Wallet not found');
  }

  // Prepare event with wallet info
  const event = args.event;
  event.wallet = wallet;
  event.walletAddress = typeof wallet.getAddress === 'function' ? wallet.getAddress() : args.walletAddress;

  // Delegate to core
  const result = await walletKit.approveConnectRequest(event);

  // Handle void return (some implementations return nothing on success)
  if (result == null) {
    return { success: true };
  }

  if (!result?.success) {
    throw new Error(result?.message || 'Failed to approve connect request');
  }

  return result;
}

/**
 * Reject connect request
 */
export async function rejectConnectRequest(args: { event: any; reason?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  required(args.event, 'event');
  const result = await walletKit.rejectConnectRequest(args.event, args.reason);

  if (result == null) {
    return { success: true };
  }

  if (!result?.success) {
    throw new Error(result?.message || 'Failed to reject connect request');
  }

  return result;
}

/**
 * Approve sign data request
 */
export async function approveSignDataRequest(args: { event: any }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  required(args.event, 'event');
  console.log('[connectionOperations] Approving sign data request');
  
  const result = await walletKit.signDataRequest(args.event);
  console.log('[connectionOperations] Sign data result:', result);
  
  return result;
}

/**
 * Reject sign data request
 */
export async function rejectSignDataRequest(args: { event: any; reason?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  required(args.event, 'event');
  const result = await walletKit.rejectSignDataRequest(args.event, args.reason);

  if (result == null) {
    return { success: true };
  }

  if (!result?.success) {
    throw new Error(result?.message || 'Failed to reject sign data request');
  }

  return result;
}

/**
 * Process internal browser TonConnect requests
 * Handles requests from internal browser WebView (injected bridge)
 */
export async function processInternalBrowserRequest(args: { 
  messageId: string; 
  method: string; 
  params?: unknown; 
  url?: string; 
  manifestUrl?: string 
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  console.log('[connectionOperations] Processing internal browser request:', args.method, args.messageId);
  console.log('[connectionOperations] URL:', args.url);

  if (typeof walletKit.processInjectedBridgeRequest !== 'function') {
    throw new Error('walletKit.processInjectedBridgeRequest is not available');
  }

  // Extract domain from the dApp URL
  let domain = 'internal-browser';
  if (args.url) {
    try {
      const dappUrl = new URL(args.url);
      domain = dappUrl.hostname;
      console.log('[connectionOperations] Domain extracted:', domain);
    } catch (e) {
      console.warn('[connectionOperations] Failed to parse dApp URL:', e);
    }
  }

  const messageInfo = {
    messageId: args.messageId,
    tabId: args.messageId,
    domain,
  };

  // Inject manifestUrl for connect requests if provided
  let finalParams = args.params;
  if (args.method === 'connect' && args.manifestUrl && finalParams && typeof finalParams === 'object' && !Array.isArray(finalParams)) {
    const paramsObj = finalParams as Record<string, unknown>;
    const hasManifestUrl = paramsObj.manifestUrl || 
                          (paramsObj.manifest && typeof paramsObj.manifest === 'object' && (paramsObj.manifest as Record<string, unknown>).url);
    
    if (!hasManifestUrl) {
      console.log('[connectionOperations] Injecting manifestUrl:', args.manifestUrl);
      paramsObj.manifestUrl = args.manifestUrl;
      finalParams = paramsObj;
    }
  }

  const request = {
    id: args.messageId,
    method: args.method,
    params: finalParams,
  };

  // Create a promise that will be resolved when jsBridgeTransport is called with the response
  const responsePromise = new Promise<any>((resolve, reject) => {
    const timeoutId = setTimeout(() => {
      reject(new Error(`Request timeout: ${args.messageId}`));
    }, 60000); // 60 second timeout
    
    // Store resolver in global map
    if (!(globalThis as any).__internalBrowserResponseResolvers) {
      (globalThis as any).__internalBrowserResponseResolvers = new Map();
    }
    (globalThis as any).__internalBrowserResponseResolvers.set(args.messageId, {
      resolve: (response: any) => {
        clearTimeout(timeoutId);
        resolve(response);
      },
      reject: (error: any) => {
        clearTimeout(timeoutId);
        reject(error);
      }
    });
  });

  console.log('[connectionOperations] Forwarding to processInjectedBridgeRequest');
  await walletKit.processInjectedBridgeRequest(messageInfo, request);

  console.log('[connectionOperations] ⏳ Awaiting response from jsBridgeTransport...');
  const response = await responsePromise;
  console.log('[connectionOperations] ✅ Received response from jsBridgeTransport:', response);

  // Return the response payload
  const result = response.payload || response;
  console.log('[connectionOperations] ✅ Returning to Kotlin:', result);
  return result;
}
