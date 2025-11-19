// Mock JavaScript bridge - Type Coercion Edge Cases
import { sendReadyEvent, sendRpcResponse, sendEvent } from './base-mock.mjs';

// Scenario #136: BigInt in JSON (unsupported in org.json)
export function bigIntInJson() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Send response with number too large for safe integer
        sendRpcResponse(callId, {
            bigNumber: 9007199254740992n,  // JavaScript BigInt
            asString: "9007199254740992",
            warning: "Value exceeds safe integer range"
        });
    };
}

// Scenario #137: Null vs undefined from JS
export function nullVsUndefined() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, {
            nullValue: null,
            undefinedValue: undefined,
            explicitUndefined: void 0,
            missingField: undefined
        });
    };
}

// Scenario #138: Empty string vs null
export function emptyStringVsNull() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, {
            emptyString: "",
            nullString: null,
            whitespaceString: "   ",
            zeroLengthString: String()
        });
    };
}

// Scenario #139: Boolean as string ("true" vs true)
export function booleanAsString() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        sendRpcResponse(callId, {
            booleanTrue: true,
            booleanFalse: false,
            stringTrue: "true",
            stringFalse: "false",
            numberOne: 1,
            numberZero: 0
        });
    };
}

// Scenario #140: Array vs single object
export function arrayVsSingleObject() {
    sendReadyEvent();
    
    window.dispatchRpc = function(method, params, callId) {
        // Sometimes send array, sometimes single object
        if (Math.random() > 0.5) {
            sendRpcResponse(callId, {
                items: [{ id: 1, name: "Item 1" }]
            });
        } else {
            sendRpcResponse(callId, {
                items: { id: 1, name: "Item 1" }  // Single object instead of array
            });
        }
    };
}
