/**
 * Scenario #13: Event arrives before handler registered
 * Scenario #14: Event arrives after handler removed
 */
import { sendReadyEvent, sendEvent } from './base-mock.mjs';

// Send event IMMEDIATELY before ready
sendEvent('accounts_changed', { accounts: [] });

setTimeout(() => {
    sendReadyEvent('testnet');
}, 100);

// Send more events rapid-fire
for (let i = 0; i < 10; i++) {
    setTimeout(() => {
        sendEvent('chain_changed', { chain: 'ton' });
    }, 50 + i * 10);
}

window.__walletkitCall = function(method, params, callId) {
    console.log('[Early Events] Method call:', method);
};
