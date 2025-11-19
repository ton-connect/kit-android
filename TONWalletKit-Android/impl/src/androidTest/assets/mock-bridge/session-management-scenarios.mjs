// Mock WalletKit bridge - Session Management Edge Cases
// Scenarios #31-32, #35-38

function scenario31_disconnectNonExistent() {
    // Scenario #31: Disconnect for non-existent session
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:disconnect']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'disconnect-1',
            kind: 'method',
            method: 'disconnect',
            result: { error: 'Session not found', sessionId: 'non-existent-123' }
        }));
    }, 100);
}

function scenario32_disconnectMissingId() {
    // Scenario #32: Disconnect missing session ID
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:disconnect']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'disconnect-2',
            kind: 'method',
            method: 'disconnect',
            result: { error: 'Missing session ID' }
        }));
    }, 100);
}

function scenario35_concurrentSessionCreation() {
    // Scenario #35: Concurrent session creation
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        // Send two connect events nearly simultaneously
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'connect',
            payload: { device: { appName: 'App1' }, sessionId: 'session-1' }
        }));
        
        window.__walletkitCall('event', JSON.stringify({
            id: 2,
            event: 'connect',
            payload: { device: { appName: 'App2' }, sessionId: 'session-2' }
        }));
    }, 100);
}

function scenario36_sessionListIteration() {
    // Scenario #36: Session list modified during iteration
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:getSessions']
    }));
    
    setTimeout(() => {
        // Send sessions list
        window.__walletkitCall('response', JSON.stringify({
            id: 'getSessions-1',
            kind: 'method',
            method: 'getSessions',
            result: [
                { sessionId: 's1', appName: 'App1' },
                { sessionId: 's2', appName: 'App2' }
            ]
        }));
        
        // Immediately disconnect one session
        setTimeout(() => {
            window.__walletkitCall('event', JSON.stringify({
                id: 3,
                event: 'disconnect',
                payload: { sessionId: 's1' }
            }));
        }, 10);
    }, 100);
}

function scenario37_sessionStorageJsMismatch() {
    // Scenario #37: Session in storage, not in JS
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:getSessions']
    }));
    
    setTimeout(() => {
        // JS reports different sessions than what storage might have
        window.__walletkitCall('response', JSON.stringify({
            id: 'getSessions-2',
            kind: 'method',
            method: 'getSessions',
            result: { 
                jsSessions: [{ sessionId: 'js-only' }],
                storageSessions: [{ sessionId: 'storage-only' }],
                mismatch: true
            }
        }));
    }, 100);
}

function scenario38_sessionJsStorageMismatch() {
    // Scenario #38: Session in JS, not in storage
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'connect',
            payload: { 
                device: { appName: 'Ephemeral' },
                sessionId: 'temp-session',
                notInStorage: true
            }
        }));
    }, 100);
}

// Export scenarios
window.sessionManagementScenarios = {
    scenario31_disconnectNonExistent,
    scenario32_disconnectMissingId,
    scenario35_concurrentSessionCreation,
    scenario36_sessionListIteration,
    scenario37_sessionStorageJsMismatch,
    scenario38_sessionJsStorageMismatch
};
