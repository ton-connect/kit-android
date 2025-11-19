// Mock WalletKit bridge - Storage & Persistence Edge Cases
// Remaining scenarios from #56-62

function scenario60_corruptStorageData() {
    // Scenario #60: Corrupt storage data
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:getStorage']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'storage-1',
            kind: 'method',
            method: 'getStorage',
            result: { data: '}{invalid-json{', corrupt: true }
        }));
    }, 100);
}

function scenario61_storageQuotaExceeded() {
    // Scenario #61: Storage quota exceeded
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:setStorage']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('response', JSON.stringify({
            id: 'storage-2',
            kind: 'method',
            method: 'setStorage',
            result: { error: 'QuotaExceededError', quotaExceeded: true }
        }));
    }, 100);
}

// Export scenarios
window.storagePersistenceScenarios = {
    scenario60_corruptStorageData,
    scenario61_storageQuotaExceeded
};
