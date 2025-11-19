// Mock WalletKit bridge - Cryptographic Operations Edge Cases
// Scenarios #69-73

function scenario69_invalidPrivateKey() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:sign']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'sign-1',
            kind: 'method',
            method: 'sign',
            result: { error: 'Invalid private key format' }
        }));
    }, 100);
}

function scenario70_wrongKeyLength() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:sign']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'sign-2',
            kind: 'method',
            method: 'sign',
            result: { error: 'Key length must be 32 bytes', actualLength: 16 }
        }));
    }, 100);
}

function scenario71_signEmptyData() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:sign']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'sign-3',
            kind: 'method',
            method: 'sign',
            result: { error: 'Cannot sign empty data' }
        }));
    }, 100);
}

function scenario72_verifyWrongPublicKey() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:verify']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'verify-1',
            kind: 'method',
            method: 'verify',
            result: { valid: false, error: 'Public key mismatch' }
        }));
    }, 100);
}

function scenario73_entropySourceFailure() {
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:generateKey']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'generate-1',
            kind: 'method',
            method: 'generateKey',
            result: { error: 'Entropy source failure', insufficientEntropy: true }
        }));
    }, 100);
}

window.cryptographicOperationsScenarios = {
    scenario69_invalidPrivateKey,
    scenario70_wrongKeyLength,
    scenario71_signEmptyData,
    scenario72_verifyWrongPublicKey,
    scenario73_entropySourceFailure
};
