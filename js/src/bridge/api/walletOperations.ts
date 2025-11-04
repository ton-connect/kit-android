/**
 * Wallet Operations API
 * Handles wallet creation, retrieval, and deletion
 * Delegates to @ton/walletkit core
 */

import { getWalletKit, ensureWalletKitLoaded, getExports, getCurrentNetwork } from '../core/initialization';
import { validateMnemonic, requiredString, required } from '../utils/validators';
import { normalizeNetworkValue } from '../utils/helpers';

/**
 * Create TON mnemonic (24 words)
 */
export async function createTonMnemonic(args: { count?: number } = { count: 24 }) {
  await ensureWalletKitLoaded();
  const { CreateTonMnemonic } = getExports();
  
  // CreateTonMnemonic is exported by @ton/walletkit and returns string[] (24 words)
  const mnemonicResult = await CreateTonMnemonic();
  return { mnemonic: mnemonicResult };
}

/**
 * Create V4R2 wallet from mnemonic and add it to WalletKit
 */
export async function createV4R2WalletUsingMnemonic(args: { mnemonic: string[]; network?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  const { Signer, WalletV4R2Adapter, CHAIN } = getExports();
  
  const mnemonic = validateMnemonic(args.mnemonic);
  const networkValue = normalizeNetworkValue(args.network, CHAIN);
  const chain = networkValue === CHAIN.MAINNET ? CHAIN.MAINNET : CHAIN.TESTNET;
  
  const signer = await Signer.fromMnemonic(mnemonic, { type: 'ton' });
  const adapter = await WalletV4R2Adapter.create(signer, {
    client: walletKit.getApiClient(),
    network: chain,
  });
  
  // Add wallet to WalletKit
  await walletKit.addWallet(adapter);
  
  return { ok: true };
}

/**
 * Create V4R2 wallet from secret key and add it to WalletKit
 */
export async function createV4R2WalletUsingSecretKey(args: { secretKey: string; network?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  const { Signer, WalletV4R2Adapter, CHAIN } = getExports();
  
  const secretKey = requiredString(args.secretKey, 'secretKey');
  const networkValue = normalizeNetworkValue(args.network, CHAIN);
  const chain = networkValue === CHAIN.MAINNET ? CHAIN.MAINNET : CHAIN.TESTNET;
  
  const signer = await Signer.fromPrivateKey(secretKey);
  const adapter = await WalletV4R2Adapter.create(signer, {
    client: walletKit.getApiClient(),
    network: chain,
  });
  
  // Add wallet to WalletKit
  await walletKit.addWallet(adapter);
  
  return { ok: true };
}

/**
 * Create V5R1 wallet from mnemonic and add it to WalletKit
 */
export async function createV5R1WalletUsingMnemonic(args: { mnemonic: string[]; network?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  const { Signer, WalletV5R1Adapter, CHAIN } = getExports();
  
  const mnemonic = validateMnemonic(args.mnemonic);
  const networkValue = normalizeNetworkValue(args.network, CHAIN);
  const chain = networkValue === CHAIN.MAINNET ? CHAIN.MAINNET : CHAIN.TESTNET;
  
  const signer = await Signer.fromMnemonic(mnemonic, { type: 'ton' });
  const adapter = await WalletV5R1Adapter.create(signer, {
    client: walletKit.getApiClient(),
    network: chain,
  });
  
  // Add wallet to WalletKit
  await walletKit.addWallet(adapter);
  
  return { ok: true };
}

/**
 * Create V5R1 wallet from secret key and add it to WalletKit
 */
export async function createV5R1WalletUsingSecretKey(args: { secretKey: string; network?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  const { Signer, WalletV5R1Adapter, CHAIN } = getExports();
  
  const secretKey = requiredString(args.secretKey, 'secretKey');
  const networkValue = normalizeNetworkValue(args.network, CHAIN);
  const chain = networkValue === CHAIN.MAINNET ? CHAIN.MAINNET : CHAIN.TESTNET;
  
  const signer = await Signer.fromPrivateKey(secretKey);
  const adapter = await WalletV5R1Adapter.create(signer, {
    client: walletKit.getApiClient(),
    network: chain,
  });
  
  // Add wallet to WalletKit
  await walletKit.addWallet(adapter);
  
  return { ok: true };
}

/**
 * Add wallet to WalletKit
 */
export async function addWallet(args: { wallet: any }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  
  required(args.wallet, 'wallet');
  await walletKit.addWallet(args.wallet);
  
  return { ok: true };
}

/**
 * Get all wallets
 */
export async function getWallets() {
  const walletKit = getWalletKit();
  
  if (typeof walletKit.ensureInitialized === 'function') {
    await walletKit.ensureInitialized();
  }
  
  const wallets = walletKit.getWallets?.() || [];
  const network = getCurrentNetwork();
  
  return wallets.map((wallet: any, index: number) => ({
    address: wallet.getAddress(),
    publicKey: Array.from(wallet.publicKey as Uint8Array)
      .map((b: number) => b.toString(16).padStart(2, '0'))
      .join(''),
    version: typeof wallet.version === 'string' ? wallet.version : 'unknown',
    index,
    network,
  }));
}

/**
 * Remove wallet by address
 */
export async function removeWallet(args: { address: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  
  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  
  if (!wallet) {
    return { removed: false };
  }
  
  await walletKit.removeWallet(address);
  return { removed: true };
}

/**
 * Get wallet balance by address
 */
export async function getWalletBalance(args: { address: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();
  
  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  
  if (!wallet) {
    throw new Error(`Wallet not found for address: ${address}`);
  }
  
  if (typeof walletKit.ensureInitialized === 'function') {
    await walletKit.ensureInitialized();
  }
  
  const balance = await wallet.getBalance();
  return { balance: String(balance) };
}

/**
 * Derive public key from mnemonic
 */
export async function derivePublicKeyFromMnemonic(args: { mnemonic: string[] }) {
  await ensureWalletKitLoaded();
  const { Signer } = getExports();
  
  const mnemonic = validateMnemonic(args.mnemonic);
  const signer = await Signer.fromMnemonic(mnemonic, { type: 'ton' });
  
  return { publicKey: signer.publicKey };
}

/**
 * Sign data with mnemonic
 */
export async function signDataWithMnemonic(args: { 
  words: string[]; 
  data: number[]; 
  mnemonicType?: 'ton' | 'bip39' 
}) {
  await ensureWalletKitLoaded();
  const { Signer } = getExports();
  
  const words = validateMnemonic(args.words, 'words');
  required(args.data, 'data');
  
  if (!Array.isArray(args.data)) {
    throw new Error('Data must be an array');
  }
  
  const signer = await Signer.fromMnemonic(words, { type: args.mnemonicType ?? 'ton' });
  const dataBytes = Uint8Array.from(args.data);
  const signatureResult = await signer.sign(dataBytes);
  
  let signatureBytes: Uint8Array;
  if (typeof signatureResult === 'string') {
    // Convert hex string to bytes
    const hex = signatureResult.startsWith('0x') ? signatureResult.slice(2) : signatureResult;
    signatureBytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
      signatureBytes[i / 2] = parseInt(hex.substr(i, 2), 16);
    }
  } else if (signatureResult instanceof Uint8Array) {
    signatureBytes = signatureResult;
  } else {
    throw new Error('Unexpected signature format');
  }
  
  return {
    signature: Array.from(signatureBytes),
  };
}
