import textEncoder from '../../ioskit/src-js/textEncoder';
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
    class PolyfillAbortController {
      signal = {
        aborted: false,
        addEventListener() {},
        removeEventListener() {},
      };
      abort() {
        (this.signal as { aborted: boolean }).aborted = true;
      }
    }
    target.AbortController = PolyfillAbortController as unknown as AbortController;
  }
}

export function setupPolyfills() {
  const scopes: Array<AnyGlobal | undefined> = [
    typeof globalThis !== 'undefined' ? (globalThis as AnyGlobal) : undefined,
    typeof window !== 'undefined' ? (window as AnyGlobal) : undefined,
    typeof self !== 'undefined' ? (self as AnyGlobal) : undefined,
  ];

  scopes.forEach((scope) => {
    if (!scope) return;
    applyTextEncoder(scope);
    ensureFetch(scope);
    ensureAbortController(scope);
    if (typeof scope.Buffer === 'undefined') {
      scope.Buffer = Buffer as unknown as typeof Buffer;
    }
    if (typeof scope.URL === 'undefined') {
      scope.URL = URL as unknown as typeof URL;
    }
    if (typeof scope.URLSearchParams === 'undefined') {
      scope.URLSearchParams = URLSearchParams as unknown as typeof URLSearchParams;
    }
  });
}
