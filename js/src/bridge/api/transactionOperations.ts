/**
 * Transaction Operations API
 * Handles TON transaction creation and execution
 * Delegates to @ton/walletkit core
 */

import { getWalletKit, ensureWalletKitLoaded } from '../core/initialization';
import { requiredString, required } from '../utils/validators';

/**
 * Send local transaction (queues transaction request)
 * Returns immediately after queueing - actual send happens on approve
 */
export async function sendLocalTransaction(args: {
  walletAddress: string;
  toAddress: string;
  amount: string;
  comment?: string;
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const walletAddress = requiredString(args.walletAddress, 'walletAddress').trim();
  const toAddress = requiredString(args.toAddress, 'toAddress').trim();
  const amount = requiredString(args.amount, 'amount').trim();

  const wallet = walletKit.getWallet?.(walletAddress);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${walletAddress}`);
  }

  const transferParams: Record<string, unknown> = {
    toAddress,
    amount,
  };

  if (args.comment) {
    transferParams.comment = args.comment.trim();
  }

  // Create transaction using wallet adapter
  const transaction = await wallet.createTransferTonTransaction(transferParams);

  // Add comment to messages for UI display (if provided)
  if (args.comment && transaction.messages && Array.isArray(transaction.messages)) {
    transaction.messages = transaction.messages.map((msg: any) => ({
      ...msg,
      comment: args.comment,
    }));
  }

  // Get preview if available
  let preview: unknown = null;
  if (typeof wallet.getTransactionPreview === 'function') {
    try {
      const previewResult = await wallet.getTransactionPreview(transaction);
      preview = previewResult?.preview ?? previewResult;
    } catch (error) {
      console.warn('[transactionOperations] getTransactionPreview failed', error);
    }
  }

  // Queue transaction request (triggers onTransactionRequest event)
  await walletKit.handleNewTransaction(wallet, transaction);

  return {
    success: true,
    transaction,
    preview,
  };
}

/**
 * Send transaction directly to blockchain
 */
export async function sendTransaction(args: {
  walletAddress: string;
  transactionContent: string;
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const walletAddress = requiredString(args.walletAddress, 'walletAddress').trim();
  const transactionContent = requiredString(args.transactionContent, 'transactionContent').trim();

  const wallet = walletKit.getWallet?.(walletAddress);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${walletAddress}`);
  }

  // Parse transaction JSON
  let transaction: any;
  try {
    transaction = JSON.parse(transactionContent);
  } catch (error) {
    throw new Error(`Invalid transaction content JSON: ${error}`);
  }

  // Send transaction directly
  const result = await wallet.sendTransaction(transaction);

  return {
    signedBoc: result.signedBoc,
  };
}

/**
 * Get recent transactions for a wallet
 */
export async function getRecentTransactions(args: {
  address: string;
  limit?: number;
  offset?: number;
}) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  const address = requiredString(args.address, 'address').trim();

  const wallet = walletKit.getWallet?.(address);
  if (!wallet) {
    throw new Error(`Wallet not found for address ${address}`);
  }

  const limit = typeof args.limit === 'number' && args.limit > 0 ? Math.floor(args.limit) : 100;
  const offset = typeof args.offset === 'number' && args.offset >= 0 ? Math.floor(args.offset) : 0;

  // Delegate to wallet's client
  const transactions = await wallet.client.getAccountTransactions(address, {
    limit,
    offset,
  });

  return { transactions };
}

/**
 * Approve transaction request
 */
export async function approveTransactionRequest(args: { event: any }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  required(args.event, 'event');
  const result = await walletKit.approveTransactionRequest(args.event);
  
  return result;
}

/**
 * Reject transaction request
 */
export async function rejectTransactionRequest(args: { event: any; reason?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  required(args.event, 'event');
  const result = await walletKit.rejectTransactionRequest(args.event, args.reason);

  // rejectTransactionRequest returns Promise<void>
  if (result == null) {
    return { success: true };
  }
  
  if (!result?.success) {
    const message = result?.message || 'Failed to reject transaction request';
    throw new Error(message);
  }
  
  return result;
}
