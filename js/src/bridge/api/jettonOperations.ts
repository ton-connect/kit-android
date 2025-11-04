/**
 * Jetton Operations API
 * Handles Jetton and NFT operations
 * Delegates to @ton/walletkit core
 */

import { getWalletKit, ensureWalletKitLoaded } from '../core/initialization';
import { requiredString } from '../utils/validators';

/**
 * Get jettons for a wallet
 */
export async function getJettons(args: { address: string; limit?: number; offset?: number }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const limit = typeof args.limit === 'number' && args.limit > 0 ? Math.floor(args.limit) : 100;
  const offset = typeof args.offset === 'number' && args.offset >= 0 ? Math.floor(args.offset) : 0;

  console.log('[jettonOperations] Fetching jettons for:', address);
  const result = await wallet.getJettons({ limit, offset });

  return result;
}

/**
 * Get single jetton details
 */
export async function getJetton(args: { jettonAddress: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const jettonAddress = requiredString(args.jettonAddress, 'jettonAddress').trim();

  // Use any wallet to access client methods
  const wallets = walletKit.getWallets?.() || [];
  if (!wallets || wallets.length === 0) {
    throw new Error('No wallets available');
  }

  const wallet = wallets[0];
  const result = await wallet.getJetton(jettonAddress);

  return result || null;
}

/**
 * Create jetton transfer transaction
 */
export async function createTransferJettonTransaction(args: {
  address: string;
  jettonAddress: string;
  amount: string;
  toAddress: string;
  comment?: string;
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const params = {
    jettonAddress: requiredString(args.jettonAddress, 'jettonAddress'),
    amount: requiredString(args.amount, 'amount'),
    toAddress: requiredString(args.toAddress, 'toAddress'),
    comment: args.comment,
  };

  console.log('[jettonOperations] Creating jetton transfer transaction');
  const result = await wallet.createTransferJettonTransaction(params);

  return result;
}

/**
 * Get jetton balance for a wallet
 */
export async function getJettonBalance(args: { address: string; jettonAddress: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const jettonAddress = requiredString(args.jettonAddress, 'jettonAddress').trim();
  const result = await wallet.getJettonBalance(jettonAddress);

  return result;
}

/**
 * Get jetton wallet address
 */
export async function getJettonWalletAddress(args: { address: string; jettonAddress: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const jettonAddress = requiredString(args.jettonAddress, 'jettonAddress').trim();
  const result = await wallet.getJettonWalletAddress(jettonAddress);

  return result;
}

/**
 * Get NFTs for a wallet
 */
export async function getNfts(args: { address: string; limit?: number; offset?: number }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const limit = typeof args.limit === 'number' && args.limit > 0 ? Math.floor(args.limit) : 100;
  const offset = typeof args.offset === 'number' && args.offset >= 0 ? Math.floor(args.offset) : 0;

  console.log('[jettonOperations] Fetching NFTs for:', address);
  const result = await wallet.getNfts({ limit, offset });

  return result;
}

/**
 * Get single NFT details
 */
export async function getNft(args: { address: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const nftAddress = requiredString(args.address, 'address').trim();

  // Use any wallet to access client methods
  const wallets = walletKit.getWallets?.() || [];
  if (!wallets || wallets.length === 0) {
    throw new Error('No wallets available');
  }

  const wallet = wallets[0];
  const result = await wallet.getNft(nftAddress);

  return result || null;
}

/**
 * Create NFT transfer transaction
 */
export async function createTransferNftTransaction(args: {
  address: string;
  nftAddress: string;
  transferAmount: string;
  toAddress: string;
  comment?: string;
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const params = {
    nftAddress: requiredString(args.nftAddress, 'nftAddress'),
    transferAmount: requiredString(args.transferAmount, 'transferAmount'),
    toAddress: requiredString(args.toAddress, 'toAddress'),
    comment: args.comment,
  };

  console.log('[jettonOperations] Creating NFT transfer transaction');
  const result = await wallet.createTransferNftTransaction(params);

  return result;
}

/**
 * Create raw NFT transfer transaction
 */
export async function createTransferNftRawTransaction(args: {
  address: string;
  nftAddress: string;
  transferAmount: string;
  transferMessage: any;
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();
  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const params = {
    nftAddress: requiredString(args.nftAddress, 'nftAddress'),
    transferAmount: requiredString(args.transferAmount, 'transferAmount'),
    transferMessage: args.transferMessage,
  };

  console.log('[jettonOperations] Creating raw NFT transfer transaction');
  const result = await wallet.createTransferNftRawTransaction(params);

  return result;
}
