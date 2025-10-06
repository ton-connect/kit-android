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
    };

declare global {
  interface Window {
    walletkitBridge?: typeof api & {
      onEvent: (handler: (event: WalletKitBridgeEvent) => void) => () => void;
    };
    __walletkitCall?: (id: string, method: WalletKitApiMethod, paramsJson?: string | null) => void;
    WalletKitNative?: { postMessage: (json: string) => void };
    AndroidBridge?: { postMessage: (json: string) => void };
    walletkit_request?: (json: string) => Promise<void>;
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

type CallContext = {
  id: string;
  method: WalletKitApiMethod;
};

let walletKit: any | null = null;
let initialized = false;

function resolveTonConnectUrl(input: unknown): string | null {
  console.log('[walletkitBridge] resolveTonConnectUrl called with input type:', typeof input);
  if (input == null) {
    console.log('[walletkitBridge] input is null/undefined');
    return null;
  }

  if (typeof input === 'string') {
    const trimmed = input.trim();
    console.log('[walletkitBridge] input is string, trimmed:', trimmed.substring(0, 100));
    if (!trimmed) {
      return null;
    }
    if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed) as unknown;
        return resolveTonConnectUrl(parsed);
      } catch (_error) {
        return null;
      }
    }
    return trimmed;
  }

  if (Array.isArray(input)) {
    console.log('[walletkitBridge] input is array, length:', input.length);
    for (const item of input) {
      const resolved = resolveTonConnectUrl(item);
      if (resolved) {
        return resolved;
      }
    }
    return null;
  }

  if (typeof input === 'object') {
    console.log('[walletkitBridge] input is object, keys:', Object.keys(input));
    const record = input as Record<string, unknown>;
    const candidates = [
      record.url,
      record.href,
      record.link,
      record.location,
      record.requestUrl,
      record.tonconnectUrl,
      record.value,
    ];
    for (const candidate of candidates) {
      if (typeof candidate === 'string') {
        const trimmed = candidate.trim();
        if (trimmed) {
          console.log('[walletkitBridge] found candidate URL:', trimmed.substring(0, 100));
          return trimmed;
        }
      }
    }

    const nestedSources = [record.params, record.payload, record.data, record.body];
    for (const source of nestedSources) {
      const resolved = resolveTonConnectUrl(source);
      if (resolved) {
        return resolved;
      }
    }
  }

  console.log('[walletkitBridge] no URL found in input');
  return null;
}

function resolveGlobalScope(): typeof globalThis {
  if (typeof globalThis !== 'undefined') {
    return globalThis;
  }
  if (typeof window !== 'undefined') {
    return window as typeof globalThis;
  }
  if (typeof self !== 'undefined') {
    return self as typeof globalThis;
  }
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return {} as any;
}

function resolveNativeBridge(scope: typeof globalThis) {
  const candidate = (scope as typeof globalThis & { WalletKitNative?: { postMessage?: (json: string) => void } }).WalletKitNative;
  if (candidate && typeof candidate.postMessage === 'function') {
    return candidate.postMessage.bind(candidate);
  }
  const windowRef = typeof scope.window === 'object' && scope.window ? scope.window : undefined;
  const windowCandidate = windowRef?.WalletKitNative;
  if (windowCandidate && typeof windowCandidate.postMessage === 'function') {
    return windowCandidate.postMessage.bind(windowCandidate);
  }
  return null;
}

function resolveAndroidBridge(scope: typeof globalThis) {
  const candidate = (scope as typeof globalThis & { AndroidBridge?: { postMessage?: (json: string) => void } }).AndroidBridge;
  if (candidate && typeof candidate.postMessage === 'function') {
    return candidate.postMessage.bind(candidate);
  }
  const windowRef = typeof scope.window === 'object' && scope.window ? scope.window : undefined;
  const windowCandidate = windowRef?.AndroidBridge;
  if (windowCandidate && typeof windowCandidate.postMessage === 'function') {
    return windowCandidate.postMessage.bind(windowCandidate);
  }
  return null;
}

function postToNative(payload: BridgePayload) {
  if (payload === null || (typeof payload !== 'object' && typeof payload !== 'function')) {
    const diagnostic = {
      type: typeof payload,
      value: payload,
      stack: new Error('postToNative non-object payload').stack,
    };
    console.error('[walletkitBridge] postToNative received non-object payload', diagnostic);
  }
  const json = JSON.stringify(payload);
  const scope = resolveGlobalScope();
  const nativePostMessage = resolveNativeBridge(scope);
  if (nativePostMessage) {
    nativePostMessage(json);
    return;
  }
  const androidPostMessage = resolveAndroidBridge(scope);
  if (androidPostMessage) {
    androidPostMessage(json);
    return;
  }
  console.debug('[walletkitBridge] → native', payload);
}

function emitCallDiagnostic(id: string, method: WalletKitApiMethod, stage: 'start' | 'checkpoint' | 'success' | 'error', message?: string) {
  postToNative({
    kind: 'diagnostic-call',
    id,
    method,
    stage,
    timestamp: Date.now(),
    message,
  });
}

function emitCallCheckpoint(context: CallContext | undefined, message: string) {
  if (!context) return;
  emitCallDiagnostic(context.id, context.method, 'checkpoint', message);
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
  emitCallDiagnostic(id, method, 'start');
  try {
    console.log(`[walletkitBridge] handleCall ${method}, looking up api[${method}]`);
    const fn = api[method];
    console.log(`[walletkitBridge] fn found:`, typeof fn, fn ? 'exists' : 'missing');
    if (!fn) throw new Error(`Unknown method ${String(method)}`);
    const context: CallContext = { id, method };
    console.log(`[walletkitBridge] about to call fn for ${method}`);
    const value = await (fn as (args: unknown, context?: CallContext) => Promise<unknown> | unknown)(params as never, context);
    console.log(`[walletkitBridge] fn returned for ${method}`);
    emitCallDiagnostic(id, method, 'success');
    respond(id, value);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(`[walletkitBridge] handleCall error for ${method}:`, err);
    console.error(`[walletkitBridge] error type:`, typeof err);
    console.error(`[walletkitBridge] error message:`, message);
    console.error(`[walletkitBridge] error stack:`, err instanceof Error ? err.stack : 'no stack');
    emitCallDiagnostic(id, method, 'error', message);
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

async function initTonWalletKit(config?: WalletKitBridgeInitConfig, context?: CallContext) {
  if (initialized && walletKit) {
    emitCallCheckpoint(context, 'initTonWalletKit:already-initialized');
    return { ok: true };
  }
  emitCallCheckpoint(context, 'initTonWalletKit:begin');
  const network = config?.network || 'testnet';
  const tonApiUrl = config?.tonApiUrl || config?.apiBaseUrl || (network === 'mainnet' ? 'https://tonapi.io' : 'https://testnet.tonapi.io');
  const clientEndpoint = config?.tonClientEndpoint || config?.apiUrl || (network === 'mainnet' ? 'https://toncenter.com/api/v2/jsonRPC' : 'https://testnet.toncenter.com/api/v2/jsonRPC');
  currentNetwork = network;
  currentApiBase = tonApiUrl;
  pendingConnectRequests.clear();
  emitCallCheckpoint(context, 'initTonWalletKit:constructing-tonwalletkit');
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
  emitCallCheckpoint(context, 'initTonWalletKit:initialized');
  const readyDetails = {
    network,
    tonApiUrl,
    tonClientEndpoint: clientEndpoint,
  };
  emit('ready', readyDetails);
  postToNative({ kind: 'ready', ...readyDetails });
  console.log('[walletkitBridge] WalletKit ready');
  emitCallCheckpoint(context, 'initTonWalletKit:ready-dispatched');
  return { ok: true };
}

function requireWalletKit() {
  if (!initialized || !walletKit) {
    throw new Error('WalletKit not initialized');
  }
}

const api = {
  async init(config?: WalletKitBridgeInitConfig, context?: CallContext) {
    emitCallCheckpoint(context, 'init:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'init:after-ensureWalletKitLoaded');
    emitCallCheckpoint(context, 'init:before-initTonWalletKit');
    const result = await initTonWalletKit(config, context);
    emitCallCheckpoint(context, 'init:after-initTonWalletKit');
    return result;
  },

  async addWalletFromMnemonic(
    args: { words: string[]; version: 'v5r1' | 'v4r2'; network?: 'mainnet' | 'testnet' },
    context?: CallContext,
  ) {
    emitCallCheckpoint(context, 'addWalletFromMnemonic:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'addWalletFromMnemonic:after-ensureWalletKitLoaded');
    requireWalletKit();
    emitCallCheckpoint(context, 'addWalletFromMnemonic:after-requireWalletKit');
    const config = new WalletInitConfigMnemonic({
      mnemonic: args.words,
      version: args.version,
      mnemonicType: 'ton',
      network: args.network || 'testnet',
    });
    emitCallCheckpoint(context, 'addWalletFromMnemonic:before-walletKit.addWallet');
    await walletKit.addWallet(config);
    emitCallCheckpoint(context, 'addWalletFromMnemonic:after-walletKit.addWallet');
    return { ok: true };
  },

  async getWallets(_?: unknown, context?: CallContext) {
    emitCallCheckpoint(context, 'getWallets:enter');
    requireWalletKit();
    emitCallCheckpoint(context, 'getWallets:after-requireWalletKit');
    const wallets = walletKit.getWallets?.() || [];
    emitCallCheckpoint(context, 'getWallets:after-walletKit.getWallets');
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

  async getWalletState(args: { address: string }, context?: CallContext) {
    requireWalletKit();
    if (typeof walletKit.ensureInitialized === 'function') {
      emitCallCheckpoint(context, 'getWalletState:before-walletKit.ensureInitialized');
      await walletKit.ensureInitialized();
      emitCallCheckpoint(context, 'getWalletState:after-walletKit.ensureInitialized');
    }
    const wallet = walletKit.getWallet(args.address);
    if (!wallet) throw new Error('Wallet not found');
    emitCallCheckpoint(context, 'getWalletState:before-wallet.getBalance');
    const balance = await wallet.getBalance();
    emitCallCheckpoint(context, 'getWalletState:after-wallet.getBalance');
    console.log('[walletkitBridge] balance type:', typeof balance);
    console.log('[walletkitBridge] balance value:', balance);
    console.log('[walletkitBridge] balance.toString type:', typeof balance?.toString);
    const balanceStr = balance != null && typeof balance.toString === 'function' ? balance.toString() : String(balance);
    console.log('[walletkitBridge] balanceStr:', balanceStr);
    const transactions = wallet.getTransactions ? await wallet.getTransactions(10) : [];
    emitCallCheckpoint(context, 'getWalletState:after-wallet.getTransactions');
    return { balance: balanceStr, transactions };
  },

  async handleTonConnectUrl(args: unknown, context?: CallContext) {
    console.log('[walletkitBridge] handleTonConnectUrl called with args:', args);
    emitCallCheckpoint(context, 'handleTonConnectUrl:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'handleTonConnectUrl:after-ensureWalletKitLoaded');
    requireWalletKit();
    emitCallCheckpoint(context, 'handleTonConnectUrl:after-requireWalletKit');
    const url = resolveTonConnectUrl(args);
    console.log('[walletkitBridge] resolved URL:', url);
    if (!url) {
      throw new Error('TON Connect URL is missing');
    }
    console.log('[walletkitBridge] calling walletKit.handleTonConnectUrl with:', url);
    try {
      const result = await walletKit.handleTonConnectUrl(url);
      console.log('[walletkitBridge] handleTonConnectUrl result:', result);
      return result;
    } catch (err) {
      console.error('[walletkitBridge] handleTonConnectUrl error:', err);
      console.error('[walletkitBridge] error type:', typeof err);
      console.error('[walletkitBridge] error message:', err instanceof Error ? err.message : String(err));
      console.error('[walletkitBridge] error stack:', err instanceof Error ? err.stack : 'no stack');
      throw err;
    }
  },

  async approveConnectRequest(args: { requestId: any; walletAddress: string }, context?: CallContext) {
    emitCallCheckpoint(context, 'approveConnectRequest:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'approveConnectRequest:after-ensureWalletKitLoaded');
    requireWalletKit();
    emitCallCheckpoint(context, 'approveConnectRequest:after-requireWalletKit');
    const event = pendingConnectRequests.get(args.requestId);
    if (!event) {
      throw new Error('Connect request not found');
    }
    const wallet = walletKit.getWallet?.(args.walletAddress);
    if (!wallet) {
      throw new Error('Wallet not found');
    }
    event.wallet = wallet;
    emitCallCheckpoint(context, 'approveConnectRequest:before-walletKit.approveConnectRequest');
    await walletKit.approveConnectRequest(event);
    emitCallCheckpoint(context, 'approveConnectRequest:after-walletKit.approveConnectRequest');
    await updateSessionHintsFromWallet(event, wallet);
    pendingConnectRequests.delete(args.requestId);
    return { ok: true };
  },

  async rejectConnectRequest(args: { requestId: any; reason?: string }, context?: CallContext) {
    emitCallCheckpoint(context, 'rejectConnectRequest:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'rejectConnectRequest:after-ensureWalletKitLoaded');
    requireWalletKit();
    emitCallCheckpoint(context, 'rejectConnectRequest:after-requireWalletKit');
    const event = pendingConnectRequests.get(args.requestId);
    if (!event) {
      throw new Error('Connect request not found');
    }
    pendingConnectRequests.delete(args.requestId);
    await walletKit.rejectConnectRequest(event, args.reason);
    return { ok: true };
  },

  async approveTransactionRequest(args: { requestId: any }, context?: CallContext) {
    emitCallCheckpoint(context, 'approveTransactionRequest:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'approveTransactionRequest:after-ensureWalletKitLoaded');
    requireWalletKit();
    return walletKit.approveTransactionRequest(args.requestId);
  },

  async rejectTransactionRequest(args: { requestId: any; reason?: string }, context?: CallContext) {
    emitCallCheckpoint(context, 'rejectTransactionRequest:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'rejectTransactionRequest:after-ensureWalletKitLoaded');
    requireWalletKit();
    return walletKit.rejectTransactionRequest(args.requestId, args.reason);
  },

  async approveSignDataRequest(args: { requestId: any }, context?: CallContext) {
    emitCallCheckpoint(context, 'approveSignDataRequest:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'approveSignDataRequest:after-ensureWalletKitLoaded');
    requireWalletKit();
    return walletKit.signDataRequest(args.requestId);
  },

  async rejectSignDataRequest(args: { requestId: any; reason?: string }, context?: CallContext) {
    emitCallCheckpoint(context, 'rejectSignDataRequest:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'rejectSignDataRequest:after-ensureWalletKitLoaded');
    requireWalletKit();
    return walletKit.rejectSignDataRequest(args.requestId, args.reason);
  },

  async listSessions(_?: unknown, context?: CallContext) {
    emitCallCheckpoint(context, 'listSessions:enter');
    requireWalletKit();
    let sessions: any[] = [];
    if (typeof walletKit.listSessions === 'function') {
      emitCallCheckpoint(context, 'listSessions:before-walletKit.listSessions');
      try {
        sessions = (await walletKit.listSessions()) ?? [];
      } catch (error) {
        console.error('[walletkitBridge] walletKit.listSessions failed', error);
        throw error;
      }
      emitCallCheckpoint(context, 'listSessions:after-walletKit.listSessions');
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

  async disconnectSession(args?: { sessionId?: string }, context?: CallContext) {
    emitCallCheckpoint(context, 'disconnectSession:before-ensureWalletKitLoaded');
    await ensureWalletKitLoaded();
    emitCallCheckpoint(context, 'disconnectSession:after-ensureWalletKitLoaded');
    requireWalletKit();
    if (typeof walletKit.disconnect !== 'function') {
      throw new Error('walletKit.disconnect is not available');
    }
    emitCallCheckpoint(context, 'disconnectSession:before-walletKit.disconnect');
    await walletKit.disconnect(args?.sessionId);
    emitCallCheckpoint(context, 'disconnectSession:after-walletKit.disconnect');
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

postToNative({
  kind: 'ready',
  network: currentNetwork,
  tonApiUrl: currentApiBase,
});
console.log('[walletkitBridge] bootstrap complete');
