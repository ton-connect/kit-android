/**
 * Helper Utilities
 * Miscellaneous helper functions
 */

/**
 * JSON replacer to handle BigInt serialization
 */
export function bigIntReplacer(_key: string, value: any): any {
  if (typeof value === 'bigint') {
    return value.toString();
  }
  return value;
}

/**
 * Resolve the global scope (handles different JS environments)
 */
export function resolveGlobalScope(): typeof globalThis {
  if (typeof globalThis !== 'undefined') return globalThis;
  if (typeof window !== 'undefined') return window as any;
  if (typeof global !== 'undefined') return global as any;
  if (typeof self !== 'undefined') return self as any;
  throw new Error('[walletkitBridge] Unable to resolve global scope');
}

/**
 * Resolve native bridge interface
 */
export function resolveNativeBridge(scope: typeof globalThis) {
  const androidBridge = resolveAndroidBridge(scope);
  if (androidBridge) return androidBridge;
  
  throw new Error(
    '[walletkitBridge] Unable to resolve native bridge. No Android bridge interface found.'
  );
}

/**
 * Resolve Android bridge interface
 */
export function resolveAndroidBridge(scope: typeof globalThis) {
  // WebView exposes WalletKitNative
  if ((scope as any).WalletKitNative?.postMessage) {
    return (scope as any).WalletKitNative;
  }
  // QuickJS might use a different name
  if ((scope as any).WalletKitJavascriptInterface?.postMessage) {
    return (scope as any).WalletKitJavascriptInterface;
  }
  // Legacy fallback
  if ((scope as any).AndroidBridge?.postMessage) {
    return (scope as any).AndroidBridge;
  }
  return null;
}

/**
 * Normalize network input (accept legacy names but return CHAIN enum values)
 */
export function normalizeNetworkValue(n: string | null | undefined, CHAIN: any): string {
  if (!n) return CHAIN.TESTNET;
  if (n === CHAIN.MAINNET) return CHAIN.MAINNET;
  if (n === CHAIN.TESTNET) return CHAIN.TESTNET;
  if (typeof n === 'string') {
    const lowered = n.toLowerCase();
    if (lowered === 'mainnet') return CHAIN.MAINNET;
    if (lowered === 'testnet') return CHAIN.TESTNET;
  }
  return CHAIN.TESTNET;
}

/**
 * Resolve TonConnect URL from various input formats
 */
export function resolveTonConnectUrl(input: unknown): string | null {
  if (!input) return null;
  
  if (typeof input === 'string') {
    const trimmed = input.trim();
    if (!trimmed) return null;
    
    // Direct URL
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return trimmed;
    }
    
    // tc:// protocol
    if (trimmed.startsWith('tc://')) {
      return trimmed;
    }
    
    return trimmed;
  }
  
  if (typeof input === 'object' && input !== null) {
    const obj = input as any;
    
    // Check for url property
    if (typeof obj.url === 'string') {
      return resolveTonConnectUrl(obj.url);
    }
    
    // Check for connectUrl property
    if (typeof obj.connectUrl === 'string') {
      return resolveTonConnectUrl(obj.connectUrl);
    }
    
    // Check for tonConnectUrl property
    if (typeof obj.tonConnectUrl === 'string') {
      return resolveTonConnectUrl(obj.tonConnectUrl);
    }
  }
  
  return null;
}

/**
 * Safe JSON stringify with BigInt support
 */
export function safeStringify(value: any): string {
  return JSON.stringify(value, bigIntReplacer);
}

/**
 * Create a delay promise
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
