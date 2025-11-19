// Mock WalletKit bridge - WebView Lifecycle Edge Cases
// Scenarios #48-55

function scenario48_pageLoadInterrupted() {
    // Scenario #48: Page load interrupted before bridge ready
    setTimeout(() => {
        window.__walletkitCall('bridgeReady', JSON.stringify({
            ready: false,
            interrupted: true
        }));
    }, 50);
}

function scenario49_reloadDuringRpc() {
    // Scenario #49: Page reload during active RPC call
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        // Simulate reload during RPC
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'connect',
            payload: { device: { appName: 'Test' } }
        }));
        
        // Immediately simulate page reload
        window.__walletkitCall('bridgeReady', JSON.stringify({
            protocolVersion: 1,
            features: ['method:connect']
        }));
    }, 100);
}

function scenario50_navigationDuringFlow() {
    // Scenario #50: WebView navigation during request/response flow
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'connect',
            payload: { device: { appName: 'Test' } }
        }));
        
        // Simulate navigation
        window.location.href = 'about:blank';
    }, 100);
}

function scenario51_backForwardButtons() {
    // Scenario #51: WebView back/forward button usage
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        // Simulate back button
        window.history.back();
        
        setTimeout(() => {
            // Simulate forward button
            window.history.forward();
        }, 50);
    }, 100);
}

function scenario52_jsInjectionTiming() {
    // Scenario #52: JavaScript injection timing issues
    // Send event before bridge might be fully initialized
    window.__walletkitCall('event', JSON.stringify({
        id: 1,
        event: 'connect',
        payload: { device: { appName: 'Early' } }
    }));
    
    setTimeout(() => {
        window.__walletkitCall('bridgeReady', JSON.stringify({
            protocolVersion: 1,
            features: ['method:connect']
        }));
    }, 200);
}

function scenario53_multipleWebViews() {
    // Scenario #53: Multiple WebView instances simultaneously
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect'],
        instanceId: 'webview-1'
    }));
    
    setTimeout(() => {
        // Second instance
        window.__walletkitCall('bridgeReady', JSON.stringify({
            protocolVersion: 1,
            features: ['method:connect'],
            instanceId: 'webview-2'
        }));
    }, 50);
}

function scenario54_webViewConfigChanges() {
    // Scenario #54: WebView configuration changes mid-flow
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        // Simulate config change (e.g., JavaScript disabled then re-enabled)
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'configChanged',
            payload: { jsEnabled: false }
        }));
        
        setTimeout(() => {
            window.__walletkitCall('event', JSON.stringify({
                id: 2,
                event: 'configChanged',
                payload: { jsEnabled: true }
            }));
        }, 50);
    }, 100);
}

function scenario55_detachReattach() {
    // Scenario #55: WebView detached from window and reattached
    window.__walletkitCall('bridgeReady', JSON.stringify({
        protocolVersion: 1,
        features: ['method:connect']
    }));
    
    setTimeout(() => {
        window.__walletkitCall('event', JSON.stringify({
            id: 1,
            event: 'detach',
            payload: {}
        }));
        
        setTimeout(() => {
            window.__walletkitCall('event', JSON.stringify({
                id: 2,
                event: 'reattach',
                payload: {}
            }));
        }, 50);
    }, 100);
}

// Export scenarios
window.webviewLifecycleScenarios = {
    scenario48_pageLoadInterrupted,
    scenario49_reloadDuringRpc,
    scenario50_navigationDuringFlow,
    scenario51_backForwardButtons,
    scenario52_jsInjectionTiming,
    scenario53_multipleWebViews,
    scenario54_webViewConfigChanges,
    scenario55_detachReattach
};
export function jsEngineTimeout() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Simulate JavaScript engine hanging - no response
        console.log('Request received but engine hangs');
        // No response sent
    };
}
