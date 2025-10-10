import path from 'path';
import { defineConfig } from 'vite';

export default defineConfig({
  define: {
    global: 'globalThis',
    'process.env.NODE_ENV': '"production"',
  },
  optimizeDeps: {
    esbuildOptions: {
      target: 'es2020',
      define: {
        global: 'globalThis',
        'process.env.NODE_ENV': '"production"',
      },
    },
  },
  build: {
    target: 'es2020',
    sourcemap: false,
    minify: false,
    emptyOutDir: false,
    outDir: 'dist-android-quickjs',
    lib: {
      entry: path.resolve(__dirname, 'js/src/index.ts'),
      name: 'WalletKitQuickJSBundle',
      formats: ['iife'],
      fileName: () => 'walletkit.quickjs.js',
    },
    rollupOptions: {
      output: {
        inlineDynamicImports: true,
        exports: 'none',
      },
    },
  },
});
