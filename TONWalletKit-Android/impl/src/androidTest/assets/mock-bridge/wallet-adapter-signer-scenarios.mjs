// Scenarios #74-80
function scenario74_adapterNotInitialized() {
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'wallet-1',
            kind: 'method',
            result: { error: 'Adapter not initialized' }
        }));
    }, 100);
}

function scenario75_multipleAdaptersSameChain() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:getAdapters']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'adapters-1',
            kind: 'method',
            result: { adapters: [
                { chain: 'TON', id: 'adapter1' },
                { chain: 'TON', id: 'adapter2', conflict: true }
            ]}
        }));
    }, 100);
}

function scenario76_customSignerException() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:sign']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'sign-1',
            kind: 'method',
            result: { error: 'Custom signer threw exception', signerError: true }
        }));
    }, 100);
}

function scenario77_hardwareWalletTimeout() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:sign']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'hw-1',
            kind: 'method',
            result: { error: 'Hardware wallet timeout', timeout: true }
        }));
    }, 5000);
}

function scenario78_ledgerAppNotOpen() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:sign']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'ledger-1',
            kind: 'method',
            result: { error: 'TON app not open on Ledger device' }
        }));
    }, 100);
}

function scenario79_walletVersionMismatch() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'connect',
            payload: { walletVersion: 'v3r2', expectedVersion: 'v4r2', mismatch: true }
        }));
    }, 100);
}

function scenario80_addressFormatIncompatibility() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:getAddress']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'addr-1',
            kind: 'method',
            result: { error: 'Address format incompatible', format: 'bounceable', required: 'non-bounceable' }
        }));
    }, 100);
}

window.walletAdapterSignerScenarios = {
    scenario74_adapterNotInitialized,
    scenario75_multipleAdaptersSameChain,
    scenario76_customSignerException,
    scenario77_hardwareWalletTimeout,
    scenario78_ledgerAppNotOpen,
    scenario79_walletVersionMismatch,
    scenario80_addressFormatIncompatibility
};
