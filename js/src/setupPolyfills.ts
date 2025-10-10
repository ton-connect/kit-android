import textEncoder from './textEncoder';
import { Buffer } from 'buffer';
import { URL, URLSearchParams } from 'whatwg-url';

type AnyGlobal = typeof globalThis & Record<string, unknown>;

function applyTextEncoder(target: AnyGlobal) {
  try {
    textEncoder(target as any);
  } catch (err) {
    console.error('[walletkitBridge] Failed to apply TextEncoder polyfill', err);
  }
}

function ensureFetch(target: AnyGlobal) {
  if (typeof target.fetch === 'undefined' && typeof window !== 'undefined' && typeof window.fetch === 'function') {
    target.fetch = window.fetch.bind(window);
  }
}

function ensureAbortController(target: AnyGlobal) {
  if (typeof target.AbortController === 'undefined') {
    class PolyfillAbortController implements AbortController {
      signal = {
        aborted: false,
        addEventListener() {},
        removeEventListener() {},
        dispatchEvent() { return true; },
        onabort: null,
        reason: undefined,
        throwIfAborted() {},
      } as AbortSignal;
      
      abort() {
        (this.signal as any).aborted = true;
      }
    }
    target.AbortController = PolyfillAbortController as any;
  }
}

export function setupPolyfills() {
  const scopes: Array<AnyGlobal | undefined> = [
    typeof globalThis !== 'undefined' ? (globalThis as unknown as AnyGlobal) : undefined,
    typeof window !== 'undefined' ? (window as unknown as AnyGlobal) : undefined,
    typeof self !== 'undefined' ? (self as unknown as AnyGlobal) : undefined,
  ];

  scopes.forEach((scope) => {
    if (!scope) return;
    applyTextEncoder(scope);
    ensureFetch(scope);
    ensureAbortController(scope);
    if (typeof scope.Buffer === 'undefined') {
      scope.Buffer = Buffer as any;
    }
    if (typeof scope.URL === 'undefined') {
      scope.URL = URL as any;
    }
    if (typeof scope.URLSearchParams === 'undefined') {
      scope.URLSearchParams = URLSearchParams as any;
    }
  });
}
