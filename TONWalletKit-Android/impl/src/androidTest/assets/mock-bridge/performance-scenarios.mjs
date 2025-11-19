// Mock JavaScript bridge scenarios for performance issues
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #110: CPU throttling
export function cpuThrottling() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('performance_warning', { 
            type: 'cpu_throttled',
            severity: 'high' 
        });
        
        // Respond with significant delay
        setTimeout(() => {
            sendRpcResponse(callId, { 
                success: true, 
                throttled: true,
                execution_time_ms: 2000 
            });
        }, 2000);
    };
}

// Scenario #111: Memory leak detected
export function memoryLeakDetected() {
    sendReadyEvent();
    
    let allocations = 0;
    window.dispatchRpc = function(method, params, callId) {
        allocations++;
        
        if (allocations > 10) {
            sendEvent('memory_leak_warning', { 
                allocations,
                estimated_leak_mb: allocations * 5 
            });
        }
        
        sendRpcResponse(callId, { 
            success: true, 
            memory_usage_mb: allocations * 5 
        });
    };
}

// Scenario #112: Frame drop during animation
export function frameDropAnimation() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('performance_degradation', { 
            fps: 15,
            target_fps: 60,
            dropped_frames: 450 
        });
        
        sendRpcResponse(callId, { 
            success: true, 
            performance_warning: true 
        });
    };
}

// Scenario #113: Battery saver mode enabled
export function batterySaverMode() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('power_mode_changed', { 
            mode: 'battery_saver',
            restrictions: ['background_sync', 'animations'] 
        });
        
        sendRpcResponse(callId, { 
            success: true, 
            reduced_functionality: true 
        });
    };
}

// Scenario #114: Thermal throttling
export function thermalThrottling() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('thermal_warning', { 
            temperature: 'critical',
            throttle_level: 'severe' 
        });
        
        // Dramatically slowed response
        setTimeout(() => {
            sendRpcResponse(callId, { 
                success: true, 
                thermal_throttled: true 
            });
        }, 3000);
    };
}
