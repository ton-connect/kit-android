package io.ton.walletkit.demo.model

enum class TonNetwork {
    MAINNET,
    TESTNET,
    ;

    fun asBridgeValue(): String = when (this) {
        MAINNET -> "mainnet"
        TESTNET -> "testnet"
    }

    companion object {
        fun fromBridge(value: String?): TonNetwork = when (value?.lowercase()) {
            "mainnet" -> MAINNET
            else -> TESTNET
        }
    }
}
