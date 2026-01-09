/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit

import io.ton.walletkit.api.generated.*
import io.ton.walletkit.model.TONBalance
import io.ton.walletkit.model.TONUserFriendlyAddress

/**
 * TON wallet instance for transaction management and dApp interactions.
 *
 * Mirrors the iOS TONWalletProtocol for cross-platform consistency.
 */
interface ITONWallet {
    /**
     * Unique wallet identifier.
     */
    val id: String

    /**
     * Wallet address in user-friendly format.
     */
    val address: TONUserFriendlyAddress

    /**
     * Get wallet balance.
     */
    suspend fun balance(): TONBalance

    /**
     * Create a TON transfer transaction.
     *
     * @param request Transfer request with recipient, amount, and optional comment
     * @return Transaction request ready for sending or preview
     */
    suspend fun transferTONTransaction(request: TONTransferRequest): TONTransactionRequest

    /**
     * Create a multi-recipient TON transfer transaction.
     *
     * @param requests List of transfer requests
     * @return Transaction request ready for sending or preview
     */
    suspend fun transferTONTransaction(requests: List<TONTransferRequest>): TONTransactionRequest

    /**
     * Send a transaction to the blockchain.
     *
     * @param transactionRequest Transaction request from transferTON/NFT/Jetton methods
     */
    suspend fun send(transactionRequest: TONTransactionRequest)

    /**
     * Get transaction preview with fee estimation.
     *
     * @param transactionRequest Transaction request to preview
     * @return Preview with estimated fees and trace
     */
    suspend fun preview(transactionRequest: TONTransactionRequest): TONTransactionEmulatedPreview

    /**
     * Create an NFT transfer transaction.
     *
     * @param request NFT transfer request with recipient and optional comment
     * @return Transaction request ready for sending or preview
     */
    suspend fun transferNFTTransaction(request: TONNFTTransferRequest): TONTransactionRequest

    /**
     * Create an NFT transfer transaction with raw parameters.
     *
     * @param request Raw NFT transfer request with full control
     * @return Transaction request ready for sending or preview
     */
    suspend fun transferNFTTransaction(request: TONNFTRawTransferRequest): TONTransactionRequest

    /**
     * Get NFTs owned by this wallet.
     *
     * @param request Request with pagination and optional filters
     * @return Response with NFTs and address book
     */
    suspend fun nfts(request: TONNFTsRequest): TONNFTsResponse

    /**
     * Get a single NFT by address.
     *
     * @param address NFT contract address
     * @return NFT details
     */
    suspend fun nft(address: TONUserFriendlyAddress): TONNFT

    /**
     * Get balance of a specific jetton.
     *
     * @param jettonAddress Jetton master contract address
     * @return Balance in jetton units
     */
    suspend fun jettonBalance(jettonAddress: TONUserFriendlyAddress): TONBalance

    /**
     * Get jetton wallet address for a specific jetton.
     *
     * Each user has a unique jetton wallet contract for each jetton they hold.
     *
     * @param jettonAddress Jetton master contract address
     * @return Jetton wallet contract address
     */
    suspend fun jettonWalletAddress(jettonAddress: TONUserFriendlyAddress): TONUserFriendlyAddress

    /**
     * Create a jetton transfer transaction.
     *
     * @param request Jetton transfer request
     * @return Transaction request ready for sending or preview
     */
    suspend fun transferJettonTransaction(request: TONJettonsTransferRequest): TONTransactionRequest

    /**
     * Get jettons owned by this wallet.
     *
     * @param request Request with pagination
     * @return Response with jettons and address book
     */
    suspend fun jettons(request: TONJettonsRequest): TONJettonsResponse
}

/**
 * Convenience extensions for ITONWallet.
 *
 * Mirrors iOS TONWalletProtocol extensions.
 */

/**
 * Get NFTs with a simple limit parameter.
 *
 * @param limit Maximum number of NFTs to return
 * @return Response with NFTs and address book
 */
suspend fun ITONWallet.nfts(limit: Int): TONNFTsResponse {
    val request = TONNFTsRequest(pagination = TONPagination(limit = limit))
    return nfts(request)
}

/**
 * Get jettons with a simple limit parameter.
 *
 * @param limit Maximum number of jettons to return
 * @return Response with jettons and address book
 */
suspend fun ITONWallet.jettons(limit: Int): TONJettonsResponse {
    val request = TONJettonsRequest(pagination = TONPagination(limit = limit))
    return jettons(request)
}
