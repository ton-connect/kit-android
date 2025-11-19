// Mock JavaScript bridge scenarios for storage operations
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #80: Storage quota exceeded
export function storageQuotaExceeded() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'saveData') {
            sendEvent('storage_warning', { 
                usage: 95, 
                limit: 100,
                unit: 'MB' 
            });
            sendRpcResponse(callId, { 
                success: false, 
                error: 'quota_exceeded',
                available: 5 * 1024 * 1024 // 5MB remaining
            });
        }
    };
}

// Scenario #81: Corrupted storage data
export function corruptedStorageData() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'loadData') {
            sendRpcResponse(callId, { 
                success: false, 
                error: 'corrupted_data',
                recovered: false,
                suggestion: 'clear_storage'
            });
        }
    };
}

// Scenario #82: Storage read/write race
export function storageRaceCondition() {
    sendReadyEvent();
    
    let pendingWrites = 0;
    window.dispatchRpc = function(method, params, callId) {
        if (method === 'saveData') {
            pendingWrites++;
            if (pendingWrites > 1) {
                sendEvent('storage_conflict', { pendingWrites });
            }
            setTimeout(() => {
                sendRpcResponse(callId, { 
                    success: true, 
                    conflicted: pendingWrites > 1 
                });
                pendingWrites--;
            }, 100);
        }
    };
}

// Scenario #83: Storage migration failure
export function storageMigrationFailure() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('migration_started', { version: '1.0 -> 2.0' });
        setTimeout(() => {
            sendEvent('migration_failed', { 
                reason: 'schema_mismatch',
                rollback: true 
            });
            sendRpcResponse(callId, { 
                success: false, 
                error: 'migration_failed' 
            });
        }, 500);
    };
}

// Scenario #84: Cache invalidation
export function cacheInvalidation() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendEvent('cache_invalidated', { 
            reason: 'version_mismatch',
            cleared_items: 42 
        });
        sendRpcResponse(callId, { 
            success: true, 
            cache_miss: true,
            reload_required: true 
        });
    };
}
