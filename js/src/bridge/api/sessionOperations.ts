/**
 * Session Operations API
 * Handles TonConnect session management
 * Delegates to @ton/walletkit core
 */

import { getWalletKit, ensureWalletKitLoaded } from '../core/initialization';
import { serializeDate } from '../utils/formatters';

/**
 * List all active TonConnect sessions
 */
export async function listSessions() {
  const walletKit = getWalletKit();

  let sessions: any[] = [];
  
  if (typeof walletKit.listSessions === 'function') {
    try {
      sessions = (await walletKit.listSessions()) ?? [];
      console.log('[sessionOperations] Found sessions:', sessions.length);
    } catch (error) {
      console.error('[sessionOperations] listSessions failed:', error);
      throw error;
    }
  } else {
    console.warn('[sessionOperations] walletKit.listSessions not available');
  }

  const items = sessions.map((session: any) => ({
    sessionId: session.sessionId || session.id,
    dAppName: session.dAppName || session.name || '',
    walletAddress: session.walletAddress,
    dAppUrl: session.dAppUrl || session.url || null,
    manifestUrl: session.manifestUrl || null,
    iconUrl: session.dAppIconUrl || session.iconUrl || null,
    createdAt: serializeDate(session.createdAt),
    lastActivity: serializeDate(session.lastActivity),
  }));

  return { items };
}

/**
 * Disconnect a TonConnect session
 */
export async function disconnectSession(args?: { sessionId?: string }) {
  await ensureWalletKitLoaded();
  const walletKit = getWalletKit();

  if (typeof walletKit.disconnect !== 'function') {
    throw new Error('walletKit.disconnect is not available');
  }

  await walletKit.disconnect(args?.sessionId);
  return { ok: true };
}
