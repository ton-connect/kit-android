var __defProp = Object.defineProperty;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __publicField = (obj, key, value) => __defNormalProp(obj, typeof key !== "symbol" ? key + "" : key, value);
const DEFAULT_DEVICE_INFO = {
  platform: "browser",
  appName: "Wallet",
  appVersion: "1.0.0",
  maxProtocolVersion: 2,
  features: [
    "SendTransaction",
    {
      name: "SendTransaction",
      maxMessages: 1
    }
  ]
};
const DEFAULT_WALLET_INFO = {
  name: "Wallet",
  appName: "Wallet",
  imageUrl: "https://example.com/image.png",
  bridgeUrl: "https://example.com/bridge.png",
  universalLink: "https://example.com/universal-link",
  aboutUrl: "https://example.com/about",
  platforms: ["chrome", "firefox", "safari", "android", "ios", "windows", "macos", "linux"],
  jsBridgeKey: "wallet"
};
function getDeviceInfoWithDefaults(options) {
  const deviceInfo = {
    ...DEFAULT_DEVICE_INFO,
    ...options
  };
  return deviceInfo;
}
function getWalletInfoWithDefaults(options) {
  const walletInfo = {
    ...DEFAULT_WALLET_INFO,
    ...options
  };
  return walletInfo;
}
function validateBridgeConfig(config) {
  if (!config.deviceInfo) {
    throw new Error("deviceInfo is required");
  }
  if (!config.walletInfo) {
    throw new Error("walletInfo is required");
  }
  if (!config.jsBridgeKey || typeof config.jsBridgeKey !== "string") {
    throw new Error("jsBridgeKey must be a non-empty string");
  }
  if (config.protocolVersion < 2) {
    throw new Error("protocolVersion must be at least 2");
  }
}
class TonConnectBridge {
  constructor(config, transport) {
    // Public properties as per TonConnect spec
    __publicField(this, "deviceInfo");
    __publicField(this, "walletInfo");
    __publicField(this, "protocolVersion");
    __publicField(this, "isWalletBrowser");
    // Private state
    __publicField(this, "transport");
    __publicField(this, "eventListeners", []);
    this.deviceInfo = config.deviceInfo;
    this.walletInfo = config.walletInfo;
    this.protocolVersion = config.protocolVersion;
    this.isWalletBrowser = config.isWalletBrowser;
    this.transport = transport;
    this.transport.onEvent((event) => {
      this.notifyListeners(event);
    });
  }
  /**
   * Initiates connect request - forwards to transport
   */
  async connect(protocolVersion, message) {
    if (protocolVersion < 2) {
      throw new Error("Unsupported protocol version");
    }
    return this.transport.send({
      method: "connect",
      params: { protocolVersion, ...message }
    });
  }
  /**
   * Attempts to restore previous connection - forwards to transport
   */
  async restoreConnection() {
    return this.transport.send({
      method: "restoreConnection",
      params: []
    });
  }
  /**
   * Sends a message to the bridge - forwards to transport
   */
  async send(message) {
    return this.transport.send({
      method: "send",
      params: [message]
    });
  }
  /**
   * Registers a listener for events from the wallet
   * Returns unsubscribe function
   */
  listen(callback) {
    if (typeof callback !== "function") {
      throw new Error("Callback must be a function");
    }
    this.eventListeners.push(callback);
    return () => {
      const index = this.eventListeners.indexOf(callback);
      if (index > -1) {
        this.eventListeners.splice(index, 1);
      }
    };
  }
  /**
   * Expose listener count for environments that need to fan-out events across frames.
   */
  hasListeners() {
    return this.eventListeners.length > 0;
  }
  /**
   * Notify all registered listeners of an event
   */
  notifyListeners(event) {
    this.eventListeners.forEach((callback) => {
      try {
        callback(event);
      } catch (error2) {
        console.error("TonConnect event listener error:", error2);
      }
    });
  }
  /**
   * Check if transport is available
   */
  isTransportAvailable() {
    return this.transport.isAvailable();
  }
  /**
   * Cleanup resources
   */
  destroy() {
    this.eventListeners.length = 0;
    this.transport.destroy();
  }
}
const TONCONNECT_BRIDGE_REQUEST = "TONCONNECT_BRIDGE_REQUEST";
const TONCONNECT_BRIDGE_RESPONSE = "TONCONNECT_BRIDGE_RESPONSE";
const TONCONNECT_BRIDGE_EVENT = "TONCONNECT_BRIDGE_EVENT";
const INJECT_CONTENT_SCRIPT = "INJECT_CONTENT_SCRIPT";
const DEFAULT_REQUEST_TIMEOUT = 3e5;
const RESTORE_CONNECTION_TIMEOUT = 1e4;
const SUPPORTED_PROTOCOL_VERSION = 2;
class ExtensionTransport {
  constructor(window2, source) {
    __publicField(this, "extensionId", null);
    __publicField(this, "source");
    __publicField(this, "window");
    __publicField(this, "pendingRequests", /* @__PURE__ */ new Map());
    __publicField(this, "eventCallback", null);
    __publicField(this, "messageListener", null);
    this.window = window2;
    this.source = source;
    this.setupMessageListener();
  }
  /**
   * Setup listener for messages from extension
   */
  setupMessageListener() {
    this.messageListener = (event) => {
      if (event.source !== this.window)
        return;
      const data = event.data;
      if (!data || typeof data !== "object")
        return;
      if (data.type === "INJECT_EXTENSION_ID") {
        this.extensionId = data.extensionId;
        return;
      }
      if (data.type === TONCONNECT_BRIDGE_RESPONSE && data.source === this.source) {
        this.handleResponse(data);
        return;
      }
      if (data.type === TONCONNECT_BRIDGE_EVENT && data.source === this.source) {
        this.handleEvent(data.event);
        return;
      }
    };
    this.window.addEventListener("message", this.messageListener);
  }
  /**
   * Handle response from extension
   */
  handleResponse(data) {
    const pendingRequest = this.pendingRequests.get(data.messageId);
    if (!pendingRequest)
      return;
    const { resolve, reject, timeoutId } = pendingRequest;
    this.pendingRequests.delete(data.messageId);
    clearTimeout(timeoutId);
    if (data.success) {
      resolve(data.payload);
    } else {
      reject(data.error);
    }
  }
  /**
   * Handle event from extension
   */
  handleEvent(event) {
    if (this.eventCallback) {
      try {
        this.eventCallback(event);
      } catch (error2) {
        console.error("TonConnect event callback error:", error2);
      }
    }
  }
  /**
   * Send request to extension
   */
  async send(request) {
    if (!this.isAvailable()) {
      throw new Error("Chrome extension transport is not available");
    }
    return new Promise((resolve, reject) => {
      const messageId = crypto.randomUUID();
      const timeout = request.method === "restoreConnection" ? RESTORE_CONNECTION_TIMEOUT : DEFAULT_REQUEST_TIMEOUT;
      const timeoutId = setTimeout(() => {
        if (this.pendingRequests.has(messageId)) {
          this.pendingRequests.delete(messageId);
          reject(new Error(`Request timeout: ${request.method}`));
        }
      }, timeout);
      this.pendingRequests.set(messageId, { resolve, reject, timeoutId });
      try {
        chrome.runtime.sendMessage(this.extensionId, {
          type: TONCONNECT_BRIDGE_REQUEST,
          source: this.source,
          payload: request,
          messageId
        });
      } catch (error2) {
        this.pendingRequests.delete(messageId);
        clearTimeout(timeoutId);
        reject(error2);
      }
    });
  }
  /**
   * Register event callback
   */
  onEvent(callback) {
    this.eventCallback = callback;
  }
  /**
   * Check if transport is available
   */
  isAvailable() {
    return typeof chrome !== "undefined" && this.extensionId !== null;
  }
  /**
   * Request content script injection for iframes
   */
  requestContentScriptInjection() {
    if (!this.isAvailable())
      return;
    try {
      chrome.runtime.sendMessage(this.extensionId, {
        type: INJECT_CONTENT_SCRIPT
      });
    } catch (error2) {
      console.error("Failed to request content script injection:", error2);
    }
  }
  /**
   * Cleanup resources
   */
  destroy() {
    this.pendingRequests.forEach(({ timeoutId }) => clearTimeout(timeoutId));
    this.pendingRequests.clear();
    if (this.messageListener) {
      this.window.removeEventListener("message", this.messageListener);
      this.messageListener = null;
    }
    this.eventCallback = null;
    this.extensionId = null;
  }
}
class IframeWatcher {
  constructor(onIframeDetected) {
    __publicField(this, "onIframeDetected");
    __publicField(this, "observer", null);
    this.onIframeDetected = onIframeDetected;
  }
  /**
   * Start watching for iframes
   */
  start() {
    if (this.observer) {
      return;
    }
    this.observer = new MutationObserver((mutations) => {
      this.handleMutations(mutations);
    });
    this.observer.observe(document.body, {
      childList: true,
      subtree: true
    });
  }
  /**
   * Stop watching for iframes
   */
  stop() {
    if (this.observer) {
      this.observer.disconnect();
      this.observer = null;
    }
  }
  /**
   * Handle DOM mutations
   */
  handleMutations(mutations) {
    for (const mutation of mutations) {
      if (mutation.type !== "childList") {
        continue;
      }
      for (const node of mutation.addedNodes) {
        this.handleAddedNode(node);
      }
    }
  }
  /**
   * Handle a single added node
   */
  handleAddedNode(node) {
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return;
    }
    const element = node;
    if (element.tagName === "IFRAME") {
      this.setupIframeListeners(element);
      this.onIframeDetected();
      return;
    }
    const iframes = element.querySelectorAll("iframe");
    if (iframes.length > 0) {
      iframes.forEach((iframe) => {
        this.setupIframeListeners(iframe);
      });
      this.onIframeDetected();
    }
  }
  /**
   * Setup event listeners for iframe
   */
  setupIframeListeners(iframe) {
    const handleIframeEvent = () => {
      this.onIframeDetected();
    };
    iframe.removeEventListener("load", handleIframeEvent);
    iframe.removeEventListener("error", handleIframeEvent);
    iframe.addEventListener("load", handleIframeEvent);
    iframe.addEventListener("error", handleIframeEvent);
  }
}
class WindowAccessor {
  constructor(window2, { bridgeKey, injectTonKey }) {
    __publicField(this, "window");
    __publicField(this, "bridgeKey");
    __publicField(this, "injectTonKey");
    this.window = window2;
    this.bridgeKey = bridgeKey;
    this.injectTonKey = injectTonKey ?? true;
  }
  /**
   * Check if bridge already exists
   */
  exists() {
    const windowObj = this.window;
    return !!(windowObj[this.bridgeKey] && windowObj[this.bridgeKey].tonconnect);
  }
  /**
   * Get bridge key name
   */
  getBridgeKey() {
    return this.bridgeKey;
  }
  get tonKey() {
    return "ton";
  }
  /**
   * Ensure wallet object exists on window
   */
  ensureWalletObject() {
    const windowObj = this.window;
    if (!windowObj[this.bridgeKey]) {
      windowObj[this.bridgeKey] = {};
    }
    if (this.injectTonKey) {
      if (!windowObj[this.tonKey]) {
        windowObj[this.tonKey] = {};
      }
    }
  }
  /**
   * Inject bridge into window object
   */
  injectBridge(bridge) {
    this.ensureWalletObject();
    const windowObj = this.window;
    Object.defineProperty(windowObj[this.bridgeKey], "tonconnect", {
      value: bridge,
      writable: false,
      enumerable: true,
      configurable: false
    });
    if (this.injectTonKey) {
      Object.defineProperty(windowObj[this.tonKey], "tonconnect", {
        value: bridge,
        writable: false,
        enumerable: true,
        configurable: false
      });
    }
  }
}
function resolveJsBridgeKey(options) {
  if (options.jsBridgeKey) {
    return options.jsBridgeKey;
  }
  if (options.walletInfo) {
    if ("jsBridgeKey" in options.walletInfo) {
      return options.walletInfo.jsBridgeKey;
    }
    if ("name" in options.walletInfo) {
      return options.walletInfo.name;
    }
  }
  return "unknown-wallet";
}
function createBridgeConfig(options) {
  const deviceInfo = getDeviceInfoWithDefaults(options.deviceInfo);
  const walletInfo = getWalletInfoWithDefaults(options.walletInfo);
  const jsBridgeKey = resolveJsBridgeKey(options);
  return {
    deviceInfo,
    walletInfo,
    jsBridgeKey,
    isWalletBrowser: options.isWalletBrowser ?? false,
    protocolVersion: SUPPORTED_PROTOCOL_VERSION
  };
}
function injectBridge(window2, options, argsTransport) {
  const config = createBridgeConfig(options);
  validateBridgeConfig(config);
  let shouldInjectTonKey = void 0;
  if (options.injectTonKey !== void 0) {
    shouldInjectTonKey = options.injectTonKey;
  } else if (options.isWalletBrowser === true) {
    shouldInjectTonKey = true;
  } else {
    shouldInjectTonKey = true;
  }
  const windowAccessor = new WindowAccessor(window2, {
    bridgeKey: config.jsBridgeKey,
    injectTonKey: shouldInjectTonKey
  });
  if (windowAccessor.exists()) {
    console.log(`${config.jsBridgeKey}.tonconnect already exists, skipping injection`);
    return;
  }
  let transport;
  if (argsTransport) {
    transport = argsTransport;
  } else {
    const source = `${config.jsBridgeKey}-tonconnect`;
    transport = new ExtensionTransport(window2, source);
  }
  const bridge = new TonConnectBridge(config, transport);
  windowAccessor.injectBridge(bridge);
  console.log(`TonConnect JS Bridge injected for ${config.jsBridgeKey} - forwarding to extension`);
  const iframeWatcher = new IframeWatcher(() => {
    transport.requestContentScriptInjection();
  });
  iframeWatcher.start();
  return;
}
function injectBridgeCode(window2, options, transport) {
  injectBridge(window2, options, transport);
}
var LogLevel = /* @__PURE__ */ ((LogLevel2) => {
  LogLevel2[LogLevel2["OFF"] = 0] = "OFF";
  LogLevel2[LogLevel2["ERROR"] = 1] = "ERROR";
  LogLevel2[LogLevel2["WARN"] = 2] = "WARN";
  LogLevel2[LogLevel2["INFO"] = 3] = "INFO";
  LogLevel2[LogLevel2["DEBUG"] = 4] = "DEBUG";
  return LogLevel2;
})(LogLevel || {});
const logWindow = window;
const consoleRef = globalThis.console;
function getCurrentLogLevel() {
  var _a;
  const levelStr = logWindow.__WALLETKIT_LOG_LEVEL__ || "OFF";
  return (_a = LogLevel[levelStr]) != null ? _a : 0;
}
const error = (...args) => {
  var _a;
  if (getCurrentLogLevel() >= 1) {
    (_a = consoleRef == null ? void 0 : consoleRef.error) == null ? void 0 : _a.call(consoleRef, "[WalletKit]", ...args);
  }
};
var __async = (__this, __arguments, generator) => {
  return new Promise((resolve, reject) => {
    var fulfilled = (value) => {
      try {
        step(generator.next(value));
      } catch (e) {
        reject(e);
      }
    };
    var rejected = (value) => {
      try {
        step(generator.throw(value));
      } catch (e) {
        reject(e);
      }
    };
    var step = (x) => x.done ? resolve(x.value) : Promise.resolve(x.value).then(fulfilled, rejected);
    step((generator = generator.apply(__this, __arguments)).next());
  });
};
const tonWindow = window;
const frameId = tonWindow.__tonconnect_frameId || (tonWindow.__tonconnect_frameId = window === window.top ? "main" : `frame-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`);
const isAndroidWebView = typeof tonWindow.AndroidTonConnect !== "undefined";
class AndroidWebViewTransport {
  constructor() {
    this.pendingRequests = /* @__PURE__ */ new Map();
    this.eventCallbacks = [];
    this.setupNotificationHandlers();
    this.setupPostMessageRelay();
  }
  setupNotificationHandlers() {
    const bridge = tonWindow.AndroidTonConnect;
    if (!bridge) return;
    if (window === window.top) {
      bridge.__notifyResponse = (messageId) => {
        this.handleResponseNotification(messageId);
      };
      bridge.__notifyEvent = () => {
        this.handleEventNotification();
      };
    }
  }
  setupPostMessageRelay() {
    window.addEventListener("message", (event) => {
      var _a, _b;
      if (event.source === window) return;
      if (((_a = event.data) == null ? void 0 : _a.type) === "ANDROID_BRIDGE_RESPONSE") {
        this.pullAndDeliverResponse(event.data.messageId);
        document.querySelectorAll("iframe").forEach((iframe) => {
          var _a2;
          try {
            (_a2 = iframe.contentWindow) == null ? void 0 : _a2.postMessage(event.data, "*");
          } catch (_e) {
          }
        });
      } else if (((_b = event.data) == null ? void 0 : _b.type) === "ANDROID_BRIDGE_EVENT") {
        this.pullAndDeliverEvent();
        document.querySelectorAll("iframe").forEach((iframe) => {
          var _a2;
          try {
            (_a2 = iframe.contentWindow) == null ? void 0 : _a2.postMessage(event.data, "*");
          } catch (_e) {
          }
        });
      }
    });
  }
  handleResponseNotification(messageId) {
    this.pullAndDeliverResponse(messageId);
    document.querySelectorAll("iframe").forEach((iframe) => {
      var _a;
      try {
        (_a = iframe.contentWindow) == null ? void 0 : _a.postMessage({ type: "ANDROID_BRIDGE_RESPONSE", messageId }, "*");
      } catch (_e) {
      }
    });
  }
  handleEventNotification() {
    this.pullAndDeliverEvent();
    document.querySelectorAll("iframe").forEach((iframe) => {
      var _a;
      try {
        (_a = iframe.contentWindow) == null ? void 0 : _a.postMessage({ type: "ANDROID_BRIDGE_EVENT" }, "*");
      } catch (_e) {
      }
    });
  }
  pullAndDeliverResponse(messageId) {
    const pending = this.pendingRequests.get(messageId);
    if (!pending) return;
    try {
      const bridge = tonWindow.AndroidTonConnect;
      if (!(bridge == null ? void 0 : bridge.pullResponse)) return;
      const responseStr = bridge.pullResponse(messageId);
      if (responseStr) {
        const response = JSON.parse(responseStr);
        clearTimeout(pending.timeout);
        this.pendingRequests.delete(messageId);
        if (response.error) {
          pending.reject(new Error(response.error.message || "Failed"));
        } else {
          pending.resolve(response.payload);
        }
      }
    } catch (err) {
      error("[AndroidTransport] Failed to pull/process response:", err);
      pending.reject(err);
    }
  }
  pullAndDeliverEvent() {
    try {
      const bridge = tonWindow.AndroidTonConnect;
      if (!(bridge == null ? void 0 : bridge.pullEvent) || !(bridge == null ? void 0 : bridge.hasEvent)) return;
      while (bridge.hasEvent(frameId)) {
        const eventStr = bridge.pullEvent(frameId);
        if (eventStr) {
          const data = JSON.parse(eventStr);
          if (data.type === "TONCONNECT_BRIDGE_EVENT" && data.event) {
            this.eventCallbacks.forEach((callback) => {
              try {
                callback(data.event);
              } catch (err) {
                error("[AndroidTransport] Event callback error:", err);
              }
            });
          }
        }
      }
    } catch (err) {
      error("[AndroidTransport] Failed to pull/process event:", err);
    }
  }
  send(request) {
    return __async(this, null, function* () {
      const messageId = `msg-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
      const bridge = tonWindow.AndroidTonConnect;
      if (!(bridge == null ? void 0 : bridge.postMessage)) {
        throw new Error("AndroidTonConnect postMessage is not available");
      }
      bridge.postMessage(
        JSON.stringify({
          type: "TONCONNECT_BRIDGE_REQUEST",
          messageId,
          method: request.method || "unknown",
          params: request.params || {},
          frameId
        })
      );
      return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          this.pendingRequests.delete(messageId);
          reject(new Error("Request timeout"));
        }, 3e4);
        this.pendingRequests.set(messageId, { resolve, reject, timeout });
      });
    });
  }
  onEvent(callback) {
    this.eventCallbacks.push(callback);
  }
  isAvailable() {
    return isAndroidWebView;
  }
  requestContentScriptInjection() {
    const iframes = document.querySelectorAll("iframe");
    iframes.forEach((iframe) => {
      var _a;
      try {
        const iframeWindowRaw = iframe.contentWindow;
        if (!iframeWindowRaw) {
          return;
        }
        if (iframeWindowRaw === window) {
          return;
        }
        const iframeWindow = iframeWindowRaw;
        const hasExtension = !!((_a = iframeWindow.tonkeeper) == null ? void 0 : _a.tonconnect);
        if (!hasExtension) {
          const mainWindow = window;
          if (iframeWindow.injectWalletKit && mainWindow.__walletKitOptions) {
            iframeWindow.injectWalletKit(mainWindow.__walletKitOptions);
          }
        }
      } catch (_e) {
      }
    });
  }
  destroy() {
    this.pendingRequests.forEach(({ timeout, reject }) => {
      clearTimeout(timeout);
      reject(new Error("Transport destroyed"));
    });
    this.pendingRequests.clear();
    this.eventCallbacks = [];
    const bridge = tonWindow.AndroidTonConnect;
    if (bridge) {
      delete bridge.__notifyResponse;
      delete bridge.__notifyEvent;
    }
  }
}
window.injectWalletKit = (options) => {
  try {
    window.__walletKitOptions = options;
    const transport = isAndroidWebView ? new AndroidWebViewTransport() : void 0;
    injectBridgeCode(window, options, transport);
  } catch (_error) {
  }
};
//# sourceMappingURL=inject.mjs.map
