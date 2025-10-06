(function (global) {
  if (typeof global.window === 'undefined') global.window = global;
  if (typeof global.self === 'undefined') global.self = global;
  if (typeof global.global === 'undefined') global.global = global;

  // Add window.location for platform detection
  if (typeof global.window.location === 'undefined') {
    global.window.location = {
      href: 'http://localhost',
      origin: 'http://localhost',
      protocol: 'http:',
      host: 'localhost',
      hostname: 'localhost',
      port: '',
      pathname: '/',
      search: '',
      hash: ''
    };
  }

  const consoleLevels = ['log', 'info', 'warn', 'error', 'debug'];
  global.console = global.console || {};
  consoleLevels.forEach((level) => {
    global.console[level] = (...args) => {
      const message = args
        .map((item) => {
          try {
            if (typeof item === 'string') return item;
            return JSON.stringify(item);
          } catch (_error) {
            return String(item);
          }
        })
        .join(' ');
      // QuickJS JNI bridge: All calls go through unified WalletKitNative host
      WalletKitNative.consoleLog(`[${level}] ${message}`);
    };
  });
  global.console.trace = global.console.trace || global.console.debug;

  global.btoa = global.btoa || ((value) => WalletKitNative.base64Encode(value));
  global.atob = global.atob || ((value) => WalletKitNative.base64Decode(value));

  const timerCallbacks = new Map();
  global.__walletkitRunTimer = (id) => {
    const entry = timerCallbacks.get(id);
    if (!entry) return;
    try {
      entry.callback(...entry.args);
    } finally {
      if (!entry.repeat) {
        timerCallbacks.delete(id);
      }
    }
  };
  global.setTimeout = (cb, delay = 0, ...args) => {
    if (typeof cb !== 'function') throw new TypeError('setTimeout expects a function');
    // QuickJS JNI: Pass parameters as JSON string
    const id = parseInt(WalletKitNative.timerRequest(JSON.stringify({ delay: delay, repeat: false })), 10);
    timerCallbacks.set(id, { callback: cb, args, repeat: false });
    return id;
  };
  global.clearTimeout = (id) => {
    if (timerCallbacks.delete(id)) WalletKitNative.timerClear(String(id));
  };
  global.setInterval = (cb, delay = 0, ...args) => {
    if (typeof cb !== 'function') throw new TypeError('setInterval expects a function');
    // QuickJS JNI: Pass parameters as JSON string
    const id = parseInt(WalletKitNative.timerRequest(JSON.stringify({ delay: delay, repeat: true })), 10);
    timerCallbacks.set(id, { callback: cb, args, repeat: true });
    return id;
  };
  global.clearInterval = (id) => {
    if (timerCallbacks.delete(id)) WalletKitNative.timerClear(String(id));
  };
  global.queueMicrotask = global.queueMicrotask || ((fn) => Promise.resolve().then(fn));

  const fetchResolvers = new Map();
  global.__walletkitResolveFetch = (id, metaJson, bodyBase64) => {
    const entry = fetchResolvers.get(id);
    if (!entry) return;
    fetchResolvers.delete(id);
    if (!metaJson) {
      entry.reject(new Error('Fetch failed'));
      return;
    }
    const meta = JSON.parse(metaJson);
    const headers = new Map(
      (meta.headers || []).map(([name, value]) => [String(name).toLowerCase(), String(value)]),
    );
    const bodyString = bodyBase64 ? atob(bodyBase64) : '';
    const bodyBytes = new Uint8Array(bodyString.length);
    for (let i = 0; i < bodyString.length; i++) {
      bodyBytes[i] = bodyString.charCodeAt(i) & 0xff;
    }
    const response = {
      ok: meta.status >= 200 && meta.status < 300,
      status: meta.status,
      statusText: meta.statusText || '',
      headers: {
        get(name) {
          return headers.get(String(name).toLowerCase()) ?? null;
        },
        has(name) {
          return headers.has(String(name).toLowerCase());
        },
        forEach(callback) {
          headers.forEach((value, key) => callback(value, key));
        },
        entries() {
          return headers.entries();
        },
        [Symbol.iterator]() {
          return headers.entries();
        },
      },
      clone() {
        return this;
      },
      text: async () => bodyString,
      json: async () => {
        if (!bodyString) return null;
        return JSON.parse(bodyString);
      },
      arrayBuffer: async () =>
        bodyBytes.buffer.slice(bodyBytes.byteOffset, bodyBytes.byteOffset + bodyBytes.byteLength),
    };
    entry.resolve(response);
  };
  global.__walletkitRejectFetch = (id, message) => {
    const entry = fetchResolvers.get(id);
    if (!entry) return;
    fetchResolvers.delete(id);
    entry.reject(new Error(message || 'Fetch request failed'));
  };

  // Helper function to convert UTF-8 string to base64
  // btoa() only works with binary strings (Latin-1), so we need to encode UTF-8 properly
  const utf8ToBase64 = (str) => {
    // Convert UTF-8 string to UTF-8 bytes, then to base64
    const utf8Bytes = [];
    for (let i = 0; i < str.length; i++) {
      const charCode = str.charCodeAt(i);
      if (charCode < 0x80) {
        utf8Bytes.push(charCode);
      } else if (charCode < 0x800) {
        utf8Bytes.push(0xc0 | (charCode >> 6));
        utf8Bytes.push(0x80 | (charCode & 0x3f));
      } else if (charCode < 0xd800 || charCode >= 0xe000) {
        utf8Bytes.push(0xe0 | (charCode >> 12));
        utf8Bytes.push(0x80 | ((charCode >> 6) & 0x3f));
        utf8Bytes.push(0x80 | (charCode & 0x3f));
      } else {
        // Surrogate pair
        i++;
        const code = 0x10000 + (((charCode & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
        utf8Bytes.push(0xf0 | (code >> 18));
        utf8Bytes.push(0x80 | ((code >> 12) & 0x3f));
        utf8Bytes.push(0x80 | ((code >> 6) & 0x3f));
        utf8Bytes.push(0x80 | (code & 0x3f));
      }
    }
    // Convert bytes to binary string for btoa
    let binary = '';
    for (let i = 0; i < utf8Bytes.length; i++) {
      binary += String.fromCharCode(utf8Bytes[i]);
    }
    return btoa(binary);
  };

  global.fetch = (input, init = {}) => {
    try {
      const request = { headers: [], method: 'GET' };
      if (typeof input === 'string') {
        request.url = input;
      } else if (input && typeof input === 'object') {
        // Handle both Request objects (input.url) and URL objects (input.href)
        request.url = input.url || input.href;
        if (input.method) init.method = input.method;
        if (input.headers && !init.headers) init.headers = input.headers;
        if (input.body && init.body === undefined) init.body = input.body;
      }
      if (!request.url) {
        console.error('[fetch] Missing URL - input type:', typeof input, 'input keys:', input ? Object.keys(input) : 'null', 'init:', init);
        throw new TypeError('fetch requires a URL');
      }
      if (init.method) request.method = init.method;
      const headersInit = init.headers;
      if (headersInit) {
        console.log('[fetch] Headers type:', typeof headersInit, 'constructor:', headersInit.constructor?.name, 'hasForEach:', typeof headersInit.forEach);
        console.log('[fetch] Headers keys:', Object.keys(headersInit));
        console.log('[fetch] Headers entries:', JSON.stringify(Object.entries(headersInit)));
        if (Array.isArray(headersInit)) {
          headersInit.forEach(([name, value]) => request.headers.push([String(name), String(value)]));
        } else if (typeof headersInit.forEach === 'function') {
          headersInit.forEach((value, name) => request.headers.push([String(name), String(value)]));
        } else {
          Object.entries(headersInit).forEach(([name, value]) =>
            request.headers.push([String(name), String(value)]),
          );
        }
        console.log('[fetch] Final request.headers:', JSON.stringify(request.headers));
      } else {
        console.log('[fetch] No headers in init');
      }
      if (init.body != null) {
        if (init.body instanceof Uint8Array || ArrayBuffer.isView(init.body)) {
          let binary = '';
          for (let i = 0; i < init.body.length; i++) {
            binary += String.fromCharCode(init.body[i]);
          }
          request.bodyBase64 = btoa(binary);
        } else if (init.body instanceof ArrayBuffer) {
          const view = new Uint8Array(init.body);
          let binary = '';
          for (let i = 0; i < view.length; i++) {
            binary += String.fromCharCode(view[i]);
          }
          request.bodyBase64 = btoa(binary);
        } else if (typeof init.body === 'string') {
          // Use UTF-8 encoding for string bodies
          request.bodyBase64 = utf8ToBase64(init.body);
          // Add Content-Type: text/plain if not already set
          const hasContentType = request.headers.some(
            ([name]) => name.toLowerCase() === 'content-type',
          );
          if (!hasContentType) {
            request.headers.push(['Content-Type', 'text/plain;charset=UTF-8']);
          }
        } else {
          // Use UTF-8 encoding for JSON bodies
          request.bodyBase64 = utf8ToBase64(JSON.stringify(init.body));
          const hasContentType = request.headers.some(
            ([name]) => name.toLowerCase() === 'content-type',
          );
          if (!hasContentType) {
            request.headers.push(['Content-Type', 'application/json']);
          }
        }
      }
      console.log('[fetch] Calling native with:', { url: request.url, method: request.method, headersCount: request.headers.length, hasBody: !!request.bodyBase64 });
      const id = parseInt(WalletKitNative.fetchPerform(JSON.stringify(request)), 10);
      if (init.signal && typeof init.signal === 'object') {
        if (init.signal.aborted) {
          WalletKitNative.fetchAbort(String(id));
          return Promise.reject(new Error('Aborted'));
        }
        if (typeof init.signal.addEventListener === 'function') {
          init.signal.addEventListener('abort', () => WalletKitNative.fetchAbort(String(id)));
        }
      }
      return new Promise((resolve, reject) => {
        fetchResolvers.set(id, { resolve, reject });
      });
    } catch (err) {
      console.error('[fetch] Error in fetch setup:', {
        message: err.message,
        name: err.name,
        stack: err.stack,
        toString: String(err)
      });
      return Promise.reject(err);
    }
  };

  // Only polyfill crypto in QuickJS (check for WalletKitNative presence)
  if (typeof WalletKitNative !== 'undefined' && WalletKitNative.cryptoRandomBytes) {
    if (!global.crypto) {
      global.crypto = {};
    }
    if (!global.crypto.getRandomValues) {
      global.crypto.getRandomValues = (array) => {
        const length = array.length >>> 0;
        console.log('[crypto.getRandomValues] Called with length:', length);
        // QuickJS JNI: Pass length as string, returns Base64
        const base64 = WalletKitNative.cryptoRandomBytes(String(length));
        const decoded = atob(base64);
        for (let i = 0; i < length && i < decoded.length; i++) {
          array[i] = decoded.charCodeAt(i) & 0xff;
        }
        console.log('[crypto.getRandomValues] Filled array, first 8 bytes:', Array.from(array.slice(0, 8)));
        return array;
      };
    }
    global.crypto.randomUUID = global.crypto.randomUUID || (() => WalletKitNative.cryptoRandomUuid());
  }
  
  // Polyfill for Web Crypto API (crypto.subtle) for PBKDF2, HMAC, and SHA256
  if (!global.crypto.subtle) {
    global.crypto.subtle = {
      importKey: async function(format, keyData, algorithm, extractable, keyUsages) {
        if (format === 'raw') {
          // Support PBKDF2, HMAC
          if (algorithm.name === 'PBKDF2' || algorithm.name === 'HMAC') {
            return {
              _keyData: keyData,
              _algorithm: algorithm,
              type: 'secret',
              extractable,
              usages: keyUsages
            };
          }
        }
        throw new Error('Unsupported algorithm: ' + algorithm.name);
      },
      
      deriveBits: async function(algorithm, baseKey, length) {
        if (algorithm.name === 'PBKDF2') {
          // Convert key data to base64
          const keyArray = new Uint8Array(baseKey._keyData);
          let keyBinary = '';
          for (let i = 0; i < keyArray.length; i++) {
            keyBinary += String.fromCharCode(keyArray[i]);
          }
          const keyBase64 = btoa(keyBinary);
          
          // Convert salt to base64
          const saltArray = new Uint8Array(algorithm.salt);
          let saltBinary = '';
          for (let i = 0; i < saltArray.length; i++) {
            saltBinary += String.fromCharCode(saltArray[i]);
          }
          const saltBase64 = btoa(saltBinary);
          
          // Call native pbkdf2
          const resultBase64 = WalletKitNative.cryptoPbkdf2Sha512(
            keyBase64,
            saltBase64,
            algorithm.iterations,
            length / 8  // Convert bits to bytes
          );
          
          // Convert result back to ArrayBuffer
          const resultBinary = atob(resultBase64);
          const resultArray = new Uint8Array(resultBinary.length);
          for (let i = 0; i < resultBinary.length; i++) {
            resultArray[i] = resultBinary.charCodeAt(i);
          }
          return resultArray.buffer;
        }
        throw new Error('Unsupported algorithm: ' + algorithm.name);
      },
      
      sign: async function(algorithm, key, data) {
        if (algorithm.name === 'HMAC') {
          // Convert key to base64
          const keyArray = new Uint8Array(key._keyData);
          let keyBinary = '';
          for (let i = 0; i < keyArray.length; i++) {
            keyBinary += String.fromCharCode(keyArray[i]);
          }
          const keyBase64 = btoa(keyBinary);
          
          // Convert data to base64
          const dataArray = new Uint8Array(data);
          let dataBinary = '';
          for (let i = 0; i < dataArray.length; i++) {
            dataBinary += String.fromCharCode(dataArray[i]);
          }
          const dataBase64 = btoa(dataBinary);
          
          // Call native HMAC
          const resultBase64 = WalletKitNative.cryptoHmacSha512(keyBase64, dataBase64);
          
          // Convert result back to ArrayBuffer
          const resultBinary = atob(resultBase64);
          const resultArray = new Uint8Array(resultBinary.length);
          for (let i = 0; i < resultBinary.length; i++) {
            resultArray[i] = resultBinary.charCodeAt(i);
          }
          return resultArray.buffer;
        }
        throw new Error('Unsupported algorithm: ' + algorithm.name);
      },
      
      digest: async function(algorithm, data) {
        if (algorithm === 'SHA-256') {
          // Convert data to base64
          const dataArray = new Uint8Array(data);
          let dataBinary = '';
          for (let i = 0; i < dataArray.length; i++) {
            dataBinary += String.fromCharCode(dataArray[i]);
          }
          const dataBase64 = btoa(dataBinary);
          
          // Call native SHA256
          const resultBase64 = WalletKitNative.cryptoSha256(dataBase64);
          
          // Convert result back to ArrayBuffer
          const resultBinary = atob(resultBase64);
          const resultArray = new Uint8Array(resultBinary.length);
          for (let i = 0; i < resultBinary.length; i++) {
            resultArray[i] = resultBinary.charCodeAt(i);
          }
          return resultArray.buffer;
        }
        throw new Error('Unsupported algorithm: ' + algorithm);
      }
    };
  }

  // EventSource polyfill - Constructor that bundle code can use with "new EventSource(url)"
  (function() {
    const connections = new Map();
    
    // Callbacks from native side - match exact names and signatures from QuickJsWalletKitEngine.kt
    global.__walletkitEventSourceOnOpen = (id) => {
      const conn = connections.get(id);
      if (!conn) return;
      conn.readyState = 1; // OPEN
      if (conn.onopen) conn.onopen({ type: 'open' });
    };
    
    global.__walletkitEventSourceOnMessage = (id, eventType, data, lastEventId) => {
      const conn = connections.get(id);
      if (!conn) return;
      try {
        const event = {
          type: eventType || 'message',
          data: data || '',
          lastEventId: lastEventId || '',
          origin: conn.url
        };
        if (conn.onmessage) conn.onmessage(event);
      } catch (err) {
        console.error('EventSource message error:', err);
      }
    };
    
    global.__walletkitEventSourceOnError = (id, message) => {
      const conn = connections.get(id);
      if (!conn) return;
      conn.readyState = 2; // CLOSED
      if (conn.onerror) conn.onerror({ type: 'error', message: message || 'Connection error' });
    };
    
    global.__walletkitEventSourceOnClose = (id, reason) => {
      const conn = connections.get(id);
      if (!conn) return;
      conn.readyState = 2; // CLOSED
      connections.delete(id);
    };
    
    // EventSource constructor for bundle code
    function EventSource(url, options) {
      const withCredentials = options && options.withCredentials || false;
      const paramsJson = JSON.stringify({ url: url || '', withCredentials });
      
      // Call native to open connection
      const id = parseInt(WalletKitNative.eventSourceOpen(paramsJson), 10);
      
      // Store connection info
      const connection = {
        id,
        url,
        readyState: 0, // CONNECTING
        onopen: null,
        onmessage: null,
        onerror: null
      };
      connections.set(id, connection);
      
      // Return connection object
      this.readyState = connection.readyState;
      this.url = url;
      this._id = id;
      this._connection = connection;
      
      // Proxy property access to connection
      Object.defineProperty(this, 'onopen', {
        get() { return connection.onopen; },
        set(fn) { connection.onopen = fn; }
      });
      Object.defineProperty(this, 'onmessage', {
        get() { return connection.onmessage; },
        set(fn) { connection.onmessage = fn; }
      });
      Object.defineProperty(this, 'onerror', {
        get() { return connection.onerror; },
        set(fn) { connection.onerror = fn; }
      });
    }
    
    EventSource.prototype.close = function() {
      WalletKitNative.eventSourceClose(String(this._id));
      connections.delete(this._id);
    };
    
    EventSource.prototype.addEventListener = function(type, listener) {
      // Simplified - just use onmessage for now
      if (type === 'message' && !this._connection.onmessage) {
        this._connection.onmessage = listener;
      }
    };
    
    // Make EventSource available globally for bundle code
    global.EventSource = EventSource;
    
    // Also provide the wrapper interface that environment.js expects
    global.WalletKitEventSource = {
      open(url, withCredentials) {
        const paramsJson = JSON.stringify({ url: url || '', withCredentials: Boolean(withCredentials) });
        const id = parseInt(WalletKitNative.eventSourceOpen(paramsJson), 10);
        
        const entry = {
          id,
          url,
          readyState: 0,
          onopen: null,
          onmessage: null,
          onerror: null,
          eventListeners: new Map(),
        };
        connections.set(id, entry);
        return id;
      },
      close(id) {
        WalletKitNative.eventSourceClose(String(id));
        connections.delete(id);
      },
    };
  })();

  // Provide in-memory localStorage and sessionStorage for libraries that need them
  const createInMemoryStorage = () => {
    const store = new Map();
    return {
      getItem(key) {
        return store.get(String(key)) ?? null;
      },
      setItem(key, value) {
        store.set(String(key), String(value));
      },
      removeItem(key) {
        store.delete(String(key));
      },
      clear() {
        store.clear();
      },
      get length() {
        return store.size;
      },
      key(index) {
        const keys = Array.from(store.keys());
        return keys[index] ?? null;
      },
    };
  };

  // Provide persistent localStorage backed by Android SharedPreferences
  if (typeof global.localStorage === 'undefined') {
    global.localStorage = {
      getItem(key) {
        try {
          const value = WalletKitNative.localStorageGetItem(String(key));
          return value !== null && value !== undefined ? value : null;
        } catch (err) {
          console.error('[localStorage] getItem error:', err);
          return null;
        }
      },
      setItem(key, value) {
        try {
          WalletKitNative.localStorageSetItem(String(key), String(value));
        } catch (err) {
          console.error('[localStorage] setItem error:', err);
        }
      },
      removeItem(key) {
        try {
          WalletKitNative.localStorageRemoveItem(String(key));
        } catch (err) {
          console.error('[localStorage] removeItem error:', err);
        }
      },
      clear() {
        try {
          WalletKitNative.localStorageClear();
        } catch (err) {
          console.error('[localStorage] clear error:', err);
        }
      },
      get length() {
        try {
          return WalletKitNative.localStorageLength();
        } catch (err) {
          console.error('[localStorage] length error:', err);
          return 0;
        }
      },
      key(index) {
        try {
          const key = WalletKitNative.localStorageKey(index);
          return key !== null && key !== undefined ? key : null;
        } catch (err) {
          console.error('[localStorage] key error:', err);
          return null;
        }
      },
    };
    console.log('[walletkitBridge] Persistent localStorage installed');
  }
  
  // Keep sessionStorage as in-memory (session-only, not persisted)
  if (typeof global.sessionStorage === 'undefined') {
    global.sessionStorage = createInMemoryStorage();
  }

  // Headers polyfill for fetch API
  if (typeof global.Headers === 'undefined') {
    console.log('[walletkitBridge] Installing Headers polyfill');
    
    class Headers {
      constructor(init) {
        this._headers = new Map();
        
        if (init) {
          if (init instanceof Headers) {
            init.forEach((value, name) => this.append(name, value));
          } else if (Array.isArray(init)) {
            init.forEach(([name, value]) => this.append(name, value));
          } else if (typeof init === 'object') {
            Object.entries(init).forEach(([name, value]) => this.append(name, value));
          }
        }
      }

      append(name, value) {
        const normalizedName = String(name).toLowerCase();
        const normalizedValue = String(value);
        if (this._headers.has(normalizedName)) {
          const existing = this._headers.get(normalizedName);
          this._headers.set(normalizedName, existing + ', ' + normalizedValue);
        } else {
          this._headers.set(normalizedName, normalizedValue);
        }
      }

      delete(name) {
        this._headers.delete(String(name).toLowerCase());
      }

      get(name) {
        return this._headers.get(String(name).toLowerCase()) || null;
      }

      has(name) {
        return this._headers.has(String(name).toLowerCase());
      }

      set(name, value) {
        this._headers.set(String(name).toLowerCase(), String(value));
      }

      forEach(callback, thisArg) {
        this._headers.forEach((value, name) => {
          callback.call(thisArg, value, name, this);
        });
      }

      entries() {
        return this._headers.entries();
      }

      keys() {
        return this._headers.keys();
      }

      values() {
        return this._headers.values();
      }

      [Symbol.iterator]() {
        return this._headers.entries();
      }
    }

    global.Headers = Headers;
    console.log('[walletkitBridge] Headers polyfill installed');
  }

  // Request polyfill for fetch API
  if (typeof global.Request === 'undefined') {
    console.log('[walletkitBridge] Installing Request polyfill');
    
    class Request {
      constructor(input, init = {}) {
        // Parse URL
        if (typeof input === 'string') {
          this.url = input;
        } else if (input instanceof Request) {
          this.url = input.url;
          this.method = init.method || input.method;
          this.headers = new Headers(init.headers || input.headers);
          this.body = init.body !== undefined ? init.body : input.body;
          return;
        } else if (input && typeof input === 'object') {
          this.url = input.url || input.href;
        }

        // Set method
        this.method = (init.method || 'GET').toUpperCase();

        // Set headers
        this.headers = new Headers(init.headers);

        // Set body
        this.body = init.body !== undefined ? init.body : null;

        // Auto-set Content-Type header if body exists and no Content-Type set
        if (this.body != null && !this.headers.get('content-type')) {
          if (typeof this.body === 'string') {
            this.headers.set('content-type', 'text/plain;charset=UTF-8');
          } else if (this.body instanceof Blob) {
            this.headers.set('content-type', this.body.type || 'application/octet-stream');
          } else if (this.body instanceof FormData) {
            // FormData sets its own boundary
            this.headers.set('content-type', 'application/x-www-form-urlencoded;charset=UTF-8');
          }
        }
      }
    }

    global.Request = Request;
    console.log('[walletkitBridge] Request polyfill installed');
  }

  // AbortController and AbortSignal polyfill for QuickJS
  if (typeof global.AbortController === 'undefined') {
    console.log('[walletkitBridge] Installing AbortController polyfill');
    
    class AbortSignal {
      constructor() {
        this.aborted = false;
        this.reason = undefined;
        this._listeners = [];
      }

      addEventListener(type, listener, options) {
        if (type === 'abort') {
          this._listeners.push({ listener, once: options?.once });
          // If already aborted, call immediately
          if (this.aborted) {
            listener({ type: 'abort', target: this });
            if (options?.once) {
              const index = this._listeners.findIndex(l => l.listener === listener);
              if (index !== -1) this._listeners.splice(index, 1);
            }
          }
        }
      }

      removeEventListener(type, listener) {
        if (type === 'abort') {
          const index = this._listeners.findIndex(l => l.listener === listener);
          if (index !== -1) this._listeners.splice(index, 1);
        }
      }

      _abort(reason) {
        if (this.aborted) return;
        this.aborted = true;
        this.reason = reason;
        const listeners = this._listeners.slice();
        this._listeners = [];
        listeners.forEach(({ listener, once }) => {
          try {
            listener({ type: 'abort', target: this });
          } catch (err) {
            console.error('[AbortSignal] Error in abort listener:', err);
          }
        });
      }

      throwIfAborted() {
        if (this.aborted) {
          throw this.reason || new Error('Aborted');
        }
      }
    }

    class AbortController {
      constructor() {
        this.signal = new AbortSignal();
      }

      abort(reason) {
        this.signal._abort(reason);
      }
    }

    global.AbortController = AbortController;
    global.AbortSignal = AbortSignal;
    console.log('[walletkitBridge] AbortController polyfill installed');
  }

  // URL and URLSearchParams polyfill for QuickJS
  if (typeof global.URL === 'undefined' || typeof global.URLSearchParams === 'undefined') {
    console.log('[walletkitBridge] Installing URL polyfill');
    
    class URLSearchParams {
      constructor(init) {
        this._params = new Map();
        if (typeof init === 'string') {
          init = init.startsWith('?') ? init.slice(1) : init;
          if (init) {
            init.split('&').forEach(pair => {
              const [key, value] = pair.split('=').map(decodeURIComponent);
              this.append(key, value || '');
            });
          }
        } else if (init && typeof init === 'object') {
          if (init instanceof URLSearchParams) {
            init._params.forEach((value, key) => this.append(key, value));
          } else if (Array.isArray(init)) {
            init.forEach(([key, value]) => this.append(key, value));
          } else {
            Object.keys(init).forEach(key => this.append(key, init[key]));
          }
        }
      }

      append(name, value) {
        const key = String(name);
        const val = String(value);
        if (!this._params.has(key)) {
          this._params.set(key, []);
        }
        this._params.get(key).push(val);
      }

      delete(name) {
        this._params.delete(String(name));
      }

      get(name) {
        const values = this._params.get(String(name));
        return values ? values[0] : null;
      }

      getAll(name) {
        return this._params.get(String(name)) || [];
      }

      has(name) {
        return this._params.has(String(name));
      }

      set(name, value) {
        this._params.set(String(name), [String(value)]);
      }

      toString() {
        const pairs = [];
        this._params.forEach((values, key) => {
          values.forEach(value => {
            pairs.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
          });
        });
        return pairs.join('&');
      }

      forEach(callback, thisArg) {
        this._params.forEach((values, key) => {
          values.forEach(value => {
            callback.call(thisArg, value, key, this);
          });
        });
      }

      entries() {
        const entries = [];
        this._params.forEach((values, key) => {
          values.forEach(value => {
            entries.push([key, value]);
          });
        });
        return entries[Symbol.iterator]();
      }

      keys() {
        const keys = [];
        this._params.forEach((values, key) => {
          keys.push(key);
        });
        return keys[Symbol.iterator]();
      }

      values() {
        const vals = [];
        this._params.forEach((values, key) => {
          values.forEach(value => {
            vals.push(value);
          });
        });
        return vals[Symbol.iterator]();
      }
    }

    class URL {
      constructor(url, base) {
        try {
          // If base is provided, resolve url against it
          let fullUrl = url;
          if (base) {
            if (url.match(/^[a-zA-Z][a-zA-Z0-9+.-]*:/)) {
              // url has a scheme, use it as-is
              fullUrl = url;
            } else if (url.startsWith('/')) {
              const baseUrl = new URL(base);
              fullUrl = baseUrl.origin + url;
            } else {
              const baseUrl = new URL(base);
              const basePath = baseUrl.pathname.replace(/\/[^\/]*$/, '/');
              fullUrl = baseUrl.origin + basePath + url;
            }
          }

          // Match any URL scheme (not just http/https)
          // Format: scheme://authority/path?query#hash
          // Also support empty authority like tc://?params
          const match = fullUrl.match(/^([a-zA-Z][a-zA-Z0-9+.-]*:)\/\/([^\/\?#]*)(\/[^\?#]*)?([\?][^#]*)?(#.*)?$/);
          
          if (!match) {
            throw new TypeError(`Invalid URL: ${url}`);
          }

          this.protocol = match[1] || '';
          this.hostname = match[2] || '';  // Can be empty for tc://?params
          this.pathname = match[3] || '/';
          this.search = match[4] || '';
          this.hash = match[5] || '';

          // Parse port from hostname if present
          if (this.hostname) {
            const portMatch = this.hostname.match(/^(.+):(\d+)$/);
            if (portMatch) {
              this.hostname = portMatch[1];
              this.port = portMatch[2];
            } else {
              this.port = '';
            }
            this.host = this.port ? `${this.hostname}:${this.port}` : this.hostname;
            this.origin = `${this.protocol}//${this.host}`;
          } else {
            // No hostname (empty authority like tc://)
            this.port = '';
            this.host = '';
            this.origin = this.protocol + '//';
          }
          
          this.searchParams = new URLSearchParams(this.search);
          
          // Note: We don't auto-sync searchParams back to search in QuickJS
          // The bundle should read href to get the full URL with params
        } catch (error) {
          console.error('[URL polyfill] constructor error:', error);
          throw error;
        }
      }

      get href() {
        // Rebuild search from searchParams
        const searchStr = this.searchParams.toString();
        const search = searchStr ? '?' + searchStr : '';
        return this.origin + this.pathname + search + this.hash;
      }

      set href(value) {
        const newUrl = new URL(value);
        Object.assign(this, newUrl);
      }

      toString() {
        return this.href;
      }

      toJSON() {
        return this.href;
      }
    }

    global.URL = URL;
    global.URLSearchParams = URLSearchParams;
    
    // Test the polyfill with various URL formats
    try {
      // Test HTTP URL
      const testUrl1 = new URL('https://example.com/path?foo=bar');
      testUrl1.searchParams.append('baz', 'qux');
      console.log('[walletkitBridge] URL polyfill test (HTTP):', testUrl1.href);
      
            // Test custom scheme (TonConnect)
      const testUrl2 = new URL('tc://?v=2&id=test&r=data');
      console.log('[walletkitBridge] URL polyfill test (tc://):', 
        'protocol=' + testUrl2.protocol, 
        'hostname=' + testUrl2.hostname,
        'pathname=' + testUrl2.pathname,
        'search=' + testUrl2.search);
      console.log('[walletkitBridge] URL polyfill test searchParams.get("v"):', testUrl2.searchParams.get('v'));
      console.log('[walletkitBridge] URL polyfill test searchParams.get("id"):', testUrl2.searchParams.get('id'));
      console.log('[walletkitBridge] URL polyfill test searchParams.get("r"):', testUrl2.searchParams.get('r'));
      console.log('[walletkitBridge] URL polyfill test searchParams.entries():', 
        Array.from(testUrl2.searchParams._params.entries()).map(([k,v]) => k + '=' + v.join(',')).join('&'));
    } catch (error) {
      console.error('[walletkitBridge] URL polyfill test failed:', error);
    }
  }

  console.log('[walletkitBridge] bootstrap complete');
})(typeof globalThis !== 'undefined' ? globalThis : this);
