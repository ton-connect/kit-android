// Mock JavaScript bridge scenarios for network conditions
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #100: Network connectivity lost
export function networkConnectivityLost() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('network_status_changed', { 
            connected: false,
            type: 'none' 
        });
        
        sendRpcResponse(callId, { 
            success: false, 
            error: 'network_unavailable',
            retry_after: 5000
        });
    };
}

// Scenario #101: Network type switch (WiFi to cellular)
export function networkTypeSwitch() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('network_status_changed', { 
            connected: true,
            type: 'cellular',
            previous: 'wifi',
            quality: 'poor'
        });
        
        // Continue with degraded performance
        setTimeout(() => {
            sendRpcResponse(callId, { 
                success: true, 
                network: 'cellular',
                slow_connection: true 
            });
        }, 1000);
    };
}

// Scenario #102: Intermittent connectivity
export function intermittentConnectivity() {
    sendReadyEvent();
    
    let attempts = 0;
    window.dispatchRpc = function(method, params, callId) {
        attempts++;
        
        if (attempts % 2 === 0) {
            // Connection fails every other attempt
            sendEvent('network_status_changed', { connected: false });
            sendRpcResponse(callId, { 
                success: false, 
                error: 'connection_lost',
                attempt: attempts
            });
        } else {
            sendEvent('network_status_changed', { connected: true });
            sendRpcResponse(callId, { 
                success: true, 
                attempt: attempts 
            });
        }
    };
}

// Scenario #103: High latency network
export function highLatencyNetwork() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('network_quality_changed', { 
            latency_ms: 5000,
            quality: 'poor' 
        });
        
        // Respond after significant delay
        setTimeout(() => {
            sendRpcResponse(callId, { 
                success: true, 
                latency: 5000 
            });
        }, 5000);
    };
}

// Scenario #104: DNS resolution failure
export function dnsResolutionFailure() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, { 
            success: false, 
            error: 'dns_resolution_failed',
            host: 'tonapi.io',
            dns_error: 'NXDOMAIN'
        });
    };
}

// Scenario #105: SSL/TLS error
export function sslTlsError() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, { 
            success: false, 
            error: 'ssl_handshake_failed',
            certificate_invalid: true,
            details: 'Certificate expired'
        });
    };
}
