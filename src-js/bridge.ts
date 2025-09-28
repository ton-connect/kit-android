import type { WalletKitBridgeEvent, WalletKitBridgeInitConfig } from './types';

const walletKitModulePromise = import('@ton/walletkit');

let TonWalletKit: any;
let WalletInitConfigMnemonic: any;
let currentNetwork: 'mainnet' | 'testnet' = 'testnet';
let currentApiBase = 'https://testnet.tonapi.io';

async function ensureWalletKitLoaded() {
  if (TonWalletKit && WalletInitConfigMnemonic) {
    return;
  }
  const module = await walletKitModulePromise;
  TonWalletKit = (module as any).TonWalletKit;
  WalletInitConfigMnemonic = (module as any).WalletInitConfigMnemonic;
}

type WalletKitApiMethod = keyof typeof api;

type BridgePayload =
  | { kind: 'response'; id: string; result?: unknown; error?: { message: string } }
  | { kind: 'event'; event: WalletKitBridgeEvent }
  | { kind: 'ready' };

declare global {
  interface Window {
    walletkitBridge?: typeof api & {
      onEvent: (handler: (event: WalletKitBridgeEvent) => void) => () => void;
    };
    __walletkitCall?: (id: string, method: WalletKitApiMethod, paramsJson?: string | null) => void;
    WalletKitNative?: { postMessage: (json: string) => void };
    AndroidBridge?: { postMessage: (json: string) => void };
  }
}

const listeners = new Set<(event: WalletKitBridgeEvent) => void>();
const legacyRequests = new Set<string>();
const pendingConnectRequests = new Map<string, any>();
const activeSessionHints = new Map<string, SessionHint>();

type SessionHint = {
  dAppUrl?: string | null;
  manifestUrl?: string | null;
  iconUrl?: string | null;
};

let walletKit: any | null = null;
let initialized = false;

function postToNative(payload: BridgePayload) {
  const json = JSON.stringify(payload);
  if (typeof window.WalletKitNative?.postMessage === 'function') {
    window.WalletKitNative.postMessage(json);
  } else if (typeof window.AndroidBridge?.postMessage === 'function') {
    window.AndroidBridge.postMessage(json);
  } else {
    console.debug('[walletkitBridge] → native', payload);
  }
}

function emit(type: WalletKitBridgeEvent['type'], data?: WalletKitBridgeEvent['data']) {
  const event: WalletKitBridgeEvent = { type, data };
  listeners.forEach((listener) => {
    try {
      listener(event);
    } catch (err) {
      console.error('[walletkitBridge] listener error', err);
    }
  });
  postToNative({ kind: 'event', event });
  if (typeof window.AndroidBridge?.postMessage === 'function') {
    window.AndroidBridge.postMessage(JSON.stringify(event));
  }
}

function respond(id: string, result?: unknown, error?: { message: string }) {
  postToNative({ kind: 'response', id, result, error });
  if (legacyRequests.has(id)) {
    const legacyPayload = JSON.stringify({ id, result, error });
    legacyRequests.delete(id);
    window.AndroidBridge?.postMessage?.(legacyPayload);
  }
}

async function handleCall(id: string, method: WalletKitApiMethod, params?: unknown) {
  try {
    const fn = api[method];
    if (!fn) throw new Error(`Unknown method ${String(method)}`);
    const value = await fn(params as never);
    respond(id, value);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    respond(id, undefined, { message });
  }
}

window.__walletkitCall = (id, method, paramsJson) => {
  let params: unknown = undefined;
  if (paramsJson && paramsJson !== 'null') {
    try {
      params = JSON.parse(paramsJson);
    } catch (err) {
      respond(id, undefined, { message: 'Invalid params JSON' });
      return;
    }
  }
  void handleCall(id, method, params);
};

async function initTonWalletKit(config?: WalletKitBridgeInitConfig) {
  if (initialized && walletKit) {
    return { ok: true };
  }
  const network = config?.network || 'testnet';
  const tonApiUrl = config?.tonApiUrl || config?.apiBaseUrl || (network === 'mainnet' ? 'https://tonapi.io' : 'https://testnet.tonapi.io');
  const clientEndpoint = config?.tonClientEndpoint || config?.apiUrl || (network === 'mainnet' ? 'https://toncenter.com/api/v2/jsonRPC' : 'https://testnet.toncenter.com/api/v2/jsonRPC');
  currentNetwork = network;
  currentApiBase = tonApiUrl;
  pendingConnectRequests.clear();
  walletKit = new TonWalletKit({
    config: {
      bridge: {
        enableJsBridge: true,
        bridgeUrl: config?.bridgeUrl || 'https://bridge.tonapi.io/bridge',
        bridgeName: config?.bridgeName || 'tonkeeper',
      },
      eventProcessor: { disableEvents: false },
      storage: { allowMemory: config?.allowMemoryStorage ?? true },
    },
    network,
    apiUrl: clientEndpoint,
  });

  walletKit.onConnectRequest((event: any) => {
    if (event && typeof event === 'object' && event.id != null) {
      pendingConnectRequests.set(event.id, event);
    }
    emit('connectRequest', event);
  });
  walletKit.onTransactionRequest((event: unknown) => emit('transactionRequest', event));
  walletKit.onSignDataRequest((event: unknown) => emit('signDataRequest', event));
  walletKit.onDisconnect((event: unknown) => {
    console.log('[walletkitBridge] disconnect event', event);
    emit('disconnect', event);
  });

  initialized = true;
  emit('ready');
  postToNative({ kind: 'ready' });
  console.log('[walletkitBridge] WalletKit ready');
  return { ok: true };
}

function requireWalletKit() {
  if (!initialized || !walletKit) {
    throw new Error('WalletKit not initialized');
  }
}

const api = {
  async init(config?: WalletKitBridgeInitConfig) {
    await ensureWalletKitLoaded();
    return initTonWalletKit(config);
  },

  async addWalletFromMnemonic(args: { words: string[]; version: 'v5r1' | 'v4r2'; network?: 'mainnet' | 'testnet' }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    const config = new WalletInitConfigMnemonic({
      mnemonic: args.words,
      version: args.version,
      mnemonicType: 'ton',
      network: args.network || 'testnet',
    });
    await walletKit.addWallet(config);
    return { ok: true };
  },

  async getWallets() {
    requireWalletKit();
    const wallets = walletKit.getWallets?.() || [];
    return wallets.map((wallet: any, index: number) => ({
      address: wallet.getAddress(),
      publicKey: Array.from(wallet.publicKey as Uint8Array)
        .map((b: number) => b.toString(16).padStart(2, '0'))
        .join(''),
      version: typeof wallet.version === 'string' ? wallet.version : 'unknown',
      index,
      network: currentNetwork,
    }));
  },

  async getWalletState(args: { address: string }) {
    requireWalletKit();
    if (typeof walletKit.ensureInitialized === 'function') {
      await walletKit.ensureInitialized();
    }
    const wallet = walletKit.getWallet(args.address);
    if (!wallet) throw new Error('Wallet not found');
    const balance = await wallet.getBalance();
    const transactions = wallet.getTransactions ? await wallet.getTransactions(10) : [];
    return { balance: balance.toString(), transactions };
  },

  async handleTonConnectUrl(args: { url: string }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    return walletKit.handleTonConnectUrl(args.url);
  },

  async approveConnectRequest(args: { requestId: any; walletAddress: string }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    const event = pendingConnectRequests.get(args.requestId);
    if (!event) {
      throw new Error('Connect request not found');
    }
    const wallet = walletKit.getWallet?.(args.walletAddress);
    if (!wallet) {
      throw new Error('Wallet not found');
    }
    event.wallet = wallet;
    await walletKit.approveConnectRequest(event);
    await updateSessionHintsFromWallet(event, wallet);
    pendingConnectRequests.delete(args.requestId);
    return { ok: true };
  },

  async rejectConnectRequest(args: { requestId: any; reason?: string }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    const event = pendingConnectRequests.get(args.requestId);
    if (!event) {
      throw new Error('Connect request not found');
    }
    pendingConnectRequests.delete(args.requestId);
    await walletKit.rejectConnectRequest(event, args.reason);
    return { ok: true };
  },

  async approveTransactionRequest(args: { requestId: any }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    return walletKit.approveTransactionRequest(args.requestId);
  },

  async rejectTransactionRequest(args: { requestId: any; reason?: string }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    return walletKit.rejectTransactionRequest(args.requestId, args.reason);
  },

  async approveSignDataRequest(args: { requestId: any }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    return walletKit.signDataRequest(args.requestId);
  },

  async rejectSignDataRequest(args: { requestId: any; reason?: string }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    return walletKit.rejectSignDataRequest(args.requestId, args.reason);
  },

  async listSessions() {
    requireWalletKit();
    let sessions: any[] = [];
    if (typeof walletKit.listSessions === 'function') {
      sessions = (await walletKit.listSessions()) ?? [];
    }
    return sessions.map((session: any) => {
      const sessionId = session.sessionId || session.id;
      const hintKey = sessionId ?? `${session.walletAddress || ''}::${session.dAppName || session.name || ''}`;
      const hint = activeSessionHints.get(hintKey);
      return {
        sessionId,
        dAppName: session.dAppName || session.name || '',
        walletAddress: session.walletAddress,
        dAppUrl: session.dAppUrl || session.url || hint?.dAppUrl || null,
        manifestUrl: session.manifestUrl || hint?.manifestUrl || null,
        iconUrl: session.dAppIconUrl || session.iconUrl || hint?.iconUrl || null,
        createdAt: serializeDate(session.createdAt),
        lastActivity: serializeDate(session.lastActivity),
      };
    });
  },

  async disconnectSession(args?: { sessionId?: string }) {
    await ensureWalletKitLoaded();
    requireWalletKit();
    if (typeof walletKit.disconnect !== 'function') {
      throw new Error('walletKit.disconnect is not available');
    }
    await walletKit.disconnect(args?.sessionId);
    if (args?.sessionId) {
      activeSessionHints.delete(args.sessionId);
    }
    return { ok: true };
  },
};

async function updateSessionHintsFromWallet(event: any, wallet: any) {
  if (typeof walletKit.listSessions !== 'function') {
    return;
  }
  const sessions: any[] = (await walletKit.listSessions()) ?? [];
  const walletAddress = wallet.getAddress?.() || wallet.address || event.walletAddress;
  const dAppName = event.dAppName || event.name || '';
  const hintData: SessionHint = {
    dAppUrl: event.dAppUrl || event.url || event?.preview?.manifest?.url || null,
    manifestUrl: event.manifestUrl || event?.preview?.manifest?.url || null,
    iconUrl: event.dAppIconUrl || event.iconUrl || event?.preview?.manifest?.iconUrl || null,
  };

  sessions.forEach((session: any) => {
    const sessionId = session.sessionId || session.id;
    if (!sessionId) {
      return;
    }
    const matchesWallet = walletAddress && session.walletAddress === walletAddress;
    const matchesName = session.dAppName === dAppName || !dAppName;
    if (matchesWallet || matchesName) {
      activeSessionHints.set(sessionId, hintData);
    }
  });
}

function serializeDate(value: unknown): string | null {
  if (!value) return null;
  if (value instanceof Date) return value.toISOString();
  const timestamp = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(timestamp)) return null;
  return new Date(timestamp).toISOString();
}

window.walletkitBridge = Object.assign({}, api, {
  onEvent: (handler: (event: WalletKitBridgeEvent) => void) => {
    listeners.add(handler);
    return () => listeners.delete(handler);
  },
});

window.walletkit_request = async (json: string) => {
  try {
    const parsed = JSON.parse(json);
    const { id, method, params } = parsed || {};
    if (!id || !method) throw new Error('Invalid request');
    legacyRequests.add(id);
    await handleCall(id, method as WalletKitApiMethod, params);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    window.AndroidBridge?.postMessage?.(JSON.stringify({ error: { message } }));
  }
};

postToNative({ kind: 'ready' });
console.log('[walletkitBridge] bootstrap complete');
