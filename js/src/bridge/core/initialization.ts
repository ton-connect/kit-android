/**
 * WalletKit Initialization Module
 * Handles loading and initialization of @ton/walletkit core
 */

import type { WalletKitBridgeInitConfig } from '../../types';
import { AndroidStorageAdapter } from '../../AndroidStorageAdapter';
import { normalizeNetworkValue } from '../utils/helpers';
import { initFormatters } from '../utils/formatters';

// Module state
let TonWalletKit: any;
let createWalletInitConfigMnemonic: any;
let createWalletManifest: any;
let CreateTonMnemonic: any;
let Signer: any;
let WalletV4R2Adapter: any;
let WalletV5R1Adapter: any;
let Address: any;
let Cell: any;
let CHAIN: any;

let walletKit: any = null;
let initialized = false;
let currentNetwork: string = '';
let currentApiBase = 'https://testnet.tonapi.io';

const walletKitModulePromise = import('@ton/walletkit');
const tonCoreModulePromise = import('@ton/core');

/**
 * Ensure WalletKit modules are loaded
 */
export async function ensureWalletKitLoaded() {
  if (TonWalletKit && Address && Cell && Signer && CHAIN) {
    return;
  }

  // Load @ton/walletkit
  if (!TonWalletKit || !Signer || !CHAIN) {
    const module = await walletKitModulePromise;
    TonWalletKit = (module as any).TonWalletKit;
    createWalletInitConfigMnemonic = (module as any).createWalletInitConfigMnemonic;
    CreateTonMnemonic = (module as any).CreateTonMnemonic;
    createWalletManifest = (module as any).createWalletManifest;
    CHAIN = (module as any).CHAIN;
    Signer = (module as any).Signer;
    WalletV4R2Adapter = (module as any).WalletV4R2Adapter;
    WalletV5R1Adapter = (module as any).WalletV5R1Adapter;
  }

  // Load @ton/core
  if (!Address || !Cell) {
    const coreModule = await tonCoreModulePromise;
    Address = (coreModule as any).Address;
    Cell = (coreModule as any).Cell;
    
    // Initialize formatters with core dependencies
    initFormatters({ Address, Cell });
  }

  if (!CHAIN) {
    throw new Error('TonWalletKit did not expose CHAIN enum');
  }
}

/**
 * Initialize TonWalletKit instance
 */
export async function init(config?: WalletKitBridgeInitConfig, emitReady?: (data: any) => void) {
  console.log('[init] Starting WalletKit initialization...');
  
  if (initialized && walletKit) {
    console.log('[init] WalletKit already initialized');
    return { ok: true };
  }

  try {
    console.log('[init] Loading WalletKit modules...');
    await ensureWalletKitLoaded();
    console.log('[init] Modules loaded successfully');

    const networkRaw = (config?.network as string | undefined) ?? 'testnet';
    const network = normalizeNetworkValue(networkRaw, CHAIN);
    currentNetwork = network;
    
    const isMainnet = network === CHAIN.MAINNET;
    const tonApiUrl = config?.tonApiUrl || config?.apiBaseUrl || 
      (isMainnet ? 'https://tonapi.io' : 'https://testnet.tonapi.io');
    const clientEndpoint = config?.tonClientEndpoint || config?.apiUrl || 
      (isMainnet ? 'https://toncenter.com/api/v2/jsonRPC' : 'https://testnet.toncenter.com/api/v2/jsonRPC');
    
    currentApiBase = tonApiUrl;
    const chain = isMainnet ? CHAIN.MAINNET : CHAIN.TESTNET;
    
    console.log('[init] Configuration:', { network, chain, tonApiUrl, clientEndpoint });

  let walletManifest = config?.walletManifest;
  if (!walletManifest && config?.bridgeUrl && typeof createWalletManifest === 'function') {
    walletManifest = createWalletManifest({
      bridgeUrl: config.bridgeUrl,
      name: config.bridgeName ?? 'Wallet',
      appName: config.bridgeName ?? 'Wallet',
    });
  }

  const kitOptions: Record<string, unknown> = {
    network: chain,
    apiClient: { url: clientEndpoint },
  };

  if (config?.deviceInfo) {
    kitOptions.deviceInfo = config.deviceInfo;
  }

  if (walletManifest) {
    kitOptions.walletManifest = walletManifest;
  }

  const resolvedBridgeUrl = config?.bridgeUrl ?? 
    (walletManifest && typeof walletManifest === 'object' ? walletManifest.bridgeUrl : undefined);
  
  if (resolvedBridgeUrl) {
    kitOptions.bridge = {
      bridgeUrl: resolvedBridgeUrl,
      jsBridgeTransport: createJsBridgeTransport(),
    };
  }

  // Use Android native storage if available
  const nativeStorageBridge =
    (window as any).WalletKitNativeStorage ??
    ((window as any).WalletKitNative && typeof (window as any).WalletKitNative.storageGet === 'function'
      ? (window as any).WalletKitNative
      : undefined) ??
    (window as any).Android;

  const hasStorageMethods =
    nativeStorageBridge &&
    (typeof nativeStorageBridge.storageGet === 'function' ||
      typeof nativeStorageBridge.getItem === 'function') &&
    (typeof nativeStorageBridge.storageSet === 'function' ||
      typeof nativeStorageBridge.setItem === 'function');

  if (hasStorageMethods) {
    console.log('[init] Using Android native storage');
    kitOptions.storage = new AndroidStorageAdapter();
  } else if (config?.allowMemoryStorage) {
    console.log('[init] Using memory storage');
    kitOptions.storage = { allowMemory: true };
  }

  console.log('[init] Creating TonWalletKit instance with options:', { 
    network: chain, 
    hasStorage: !!kitOptions.storage,
    hasBridge: !!kitOptions.bridge,
    hasManifest: !!walletManifest 
  });
  
  walletKit = new TonWalletKit(kitOptions);
  console.log('[init] TonWalletKit instance created');

  if (typeof walletKit.ensureInitialized === 'function') {
    console.log('[init] Calling walletKit.ensureInitialized()...');
    await walletKit.ensureInitialized();
    console.log('[init] walletKit.ensureInitialized() completed');
  } else {
    console.log('[init] No ensureInitialized method found, skipping');
  }

  initialized = true;
  
  const readyDetails = {
    network,
    tonApiUrl,
    tonClientEndpoint: clientEndpoint,
  };
  
  console.log('[init] Emitting ready event with details:', readyDetails);
  if (emitReady) {
    emitReady(readyDetails);
  }
  
  console.log('[init] WalletKit ready');
  return { ok: true };
  } catch (error) {
    console.error('[init] Failed to initialize WalletKit:', error);
    throw error;
  }
}

/**
 * Create JS bridge transport for TonConnect
 */
function createJsBridgeTransport() {
  return async (sessionId: string, message: any) => {
    console.log('[jsBridgeTransport] Message:', { sessionId, messageType: message.type });
    
    // Transform disconnect responses into events
    if (message.type === 'TONCONNECT_BRIDGE_RESPONSE' && 
        message.payload?.event === 'disconnect' && 
        !message.messageId) {
      message = {
        type: 'TONCONNECT_BRIDGE_EVENT',
        source: message.source,
        event: message.payload,
        traceId: message.traceId,
      };
    }
    
    // Handle responses with messageId
    if (message.messageId) {
      const resolvers = (globalThis as any).__internalBrowserResponseResolvers;
      if (resolvers && resolvers.has(message.messageId)) {
        const { resolve } = resolvers.get(message.messageId);
        resolvers.delete(message.messageId);
        resolve(message);
      }
    }
    
    // Forward events to Android (will be handled by RPC layer)
    if (message.type === 'TONCONNECT_BRIDGE_EVENT') {
      // Store for retrieval by RPC handler
      if (!(globalThis as any).__bridgeEvents) {
        (globalThis as any).__bridgeEvents = [];
      }
      (globalThis as any).__bridgeEvents.push({ sessionId, message });
    }
    
    return Promise.resolve();
  };
}

/**
 * Get the initialized WalletKit instance
 */
export function getWalletKit() {
  if (!initialized || !walletKit) {
    throw new Error('WalletKit not initialized. Call init() first.');
  }
  return walletKit;
}

/**
 * Check if WalletKit is initialized
 */
export function isInitialized() {
  return initialized;
}

/**
 * Get current network
 */
export function getCurrentNetwork() {
  return currentNetwork;
}

/**
 * Get current API base URL
 */
export function getCurrentApiBase() {
  return currentApiBase;
}

/**
 * Get exported classes (for creating wallets, etc.)
 */
export function getExports() {
  return {
    TonWalletKit,
    Signer,
    WalletV4R2Adapter,
    WalletV5R1Adapter,
    Address,
    Cell,
    CHAIN,
    CreateTonMnemonic,
    createWalletInitConfigMnemonic,
  };
}
