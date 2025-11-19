// Scenarios #81-89
function scenario81_invalidBoc() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-1', kind: 'method', result: { error: 'Invalid BOC format' }})); }, 100);
}
function scenario82_transactionTooLarge() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-2', kind: 'method', result: { error: 'Transaction too large', size: 65536, maxSize: 65535 }})); }, 100);
}
function scenario83_gasLimitExceeded() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-3', kind: 'method', result: { error: 'Gas limit exceeded' }})); }, 100);
}
function scenario84_insufficientBalance() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-4', kind: 'method', result: { error: 'Insufficient balance', balance: 100, required: 1000 }})); }, 100);
}
function scenario85_expiredTransaction() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-5', kind: 'method', result: { error: 'Transaction expired' }})); }, 100);
}
function scenario86_duplicateTransaction() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-6', kind: 'method', result: { error: 'Duplicate transaction hash' }})); }, 100);
}
function scenario87_invalidDestination() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-7', kind: 'method', result: { error: 'Invalid destination address' }})); }, 100);
}
function scenario88_invalidStateInit() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-8', kind: 'method', result: { error: 'Invalid state init' }})); }, 100);
}
function scenario89_malformedProof() {
    window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1, features: ['method:sendTransaction'] }));
    setTimeout(() => { window.__walletkitCall('response', JSON.stringify({ id: 'tx-9', kind: 'method', result: { error: 'Malformed transaction proof' }})); }, 100);
}
window.transactionOperationsScenarios = { scenario81_invalidBoc, scenario82_transactionTooLarge, scenario83_gasLimitExceeded, scenario84_insufficientBalance, scenario85_expiredTransaction, scenario86_duplicateTransaction, scenario87_invalidDestination, scenario88_invalidStateInit, scenario89_malformedProof };
