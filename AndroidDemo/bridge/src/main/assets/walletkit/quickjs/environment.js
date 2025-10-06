(function (global) {
  const nativeEventSource = global.WalletKitEventSource;
  if (nativeEventSource && typeof nativeEventSource.open === 'function') {
    const registry = new Map();
    global.WalletKitEventSource = {
      open(url, withCredentials) {
        const handle = nativeEventSource.open(url ?? '', Boolean(withCredentials));
        registry.set(handle, true);
        return handle;
      },
      close(handle) {
        if (!registry.has(handle)) return;
        registry.delete(handle);
        nativeEventSource.close(handle);
      },
    };
  }
})(typeof globalThis !== 'undefined' ? globalThis : this);
