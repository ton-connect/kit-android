export type WalletKitBridgeEvent = {
  type: 'ready'
    | 'connectRequest'
    | 'transactionRequest'
    | 'signDataRequest'
    | 'disconnect'
    | string;
  data?: any;
};

export type WalletKitBridgeInitConfig = {
  network?: 'mainnet' | 'testnet';
  apiUrl?: string;
  apiBaseUrl?: string;
  tonApiUrl?: string;
  tonClientEndpoint?: string;
  bridgeUrl?: string;
  bridgeName?: string;
  allowMemoryStorage?: boolean;
};
