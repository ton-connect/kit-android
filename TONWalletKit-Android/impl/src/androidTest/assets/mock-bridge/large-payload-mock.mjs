/**
 * Mock scenario: Large payload
 * Tests SDK handling of large JSON payloads (simulates wallet with many NFTs/jettons).
 */

import { sendReadyEvent, sendRpcResponse } from './base-mock.mjs';

setTimeout(() => {
    sendReadyEvent('testnet');
}, 100);

window.__walletkitCall = function(method, params, callId) {
    if (method === 'getWallets') {
        // Create large wallet response with 100 NFTs
        const nfts = [];
        for (let i = 0; i < 100; i++) {
            nfts.push({
                address: `EQDKbjIcfM6ezt8KjKJJLshZJJSqX7XOA4ff-W72r5gqPrH${i}`,
                name: `NFT Item ${i}`,
                description: `This is a test NFT item number ${i} with a long description to increase payload size`,
                image: `https://example.com/nft/${i}.png`,
                collection: 'Test Collection'
            });
        }
        
        setTimeout(() => {
            sendRpcResponse(callId, {
                wallets: [{
                    address: 'EQDKbjIcfM6ezt8KjKJJLshZJJSqX7XOA4ff-W72r5gqPrHF',
                    nfts: nfts
                }]
            });
        }, 100);
    }
};
