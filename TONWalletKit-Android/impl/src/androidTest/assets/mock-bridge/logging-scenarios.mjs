// Scenarios #145-147
function scenario145_loggerThrowsException() { window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1 })); setTimeout(() => { window.__walletkitCall('event', JSON.stringify({ id: 1, event: 'logError', payload: { loggerException: true }})); }, 100); }
function scenario146_logBufferOverflow() { window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1 })); setTimeout(() => { window.__walletkitCall('event', JSON.stringify({ id: 1, event: 'logBufferOverflow', payload: { bufferSize: 10000 }})); }, 100); }
function scenario147_sensitiveDataInLogs() { window.__walletkitCall('bridgeReady', JSON.stringify({ protocolVersion: 1 })); setTimeout(() => { window.__walletkitCall('event', JSON.stringify({ id: 1, event: 'log', payload: { privateKey: 'REDACTED', warning: true }})); }, 100); }
window.loggingScenarios = { scenario145_loggerThrowsException, scenario146_logBufferOverflow, scenario147_sensitiveDataInLogs };
