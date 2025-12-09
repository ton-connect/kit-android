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
package io.ton.walletkit.demo.e2e

import io.ton.walletkit.demo.e2e.tests.ConnectE2ETest
import io.ton.walletkit.demo.e2e.tests.MerkleJettonSignDataE2ETest
import io.ton.walletkit.demo.e2e.tests.TransactionAddressAmountE2ETest
import io.ton.walletkit.demo.e2e.tests.TransactionFromMessagesNetworkE2ETest
import io.ton.walletkit.demo.e2e.tests.TransactionPayloadStateInitValidUntilE2ETest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Main test suite for all TonConnect E2E tests.
 *
 * Run this suite to execute all E2E tests:
 * ./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.ton.walletkit.demo.e2e.E2ETestSuite
 *
 * Tests are organized into 5 suites for parallel CI execution:
 * - ConnectE2ETest: Wallet connection tests
 * - TransactionAddressAmountE2ETest: Transaction address & amount validation
 * - TransactionFromMessagesNetworkE2ETest: Transaction from, messages & network validation
 * - TransactionPayloadStateInitValidUntilE2ETest: Transaction payload, stateInit, validUntil validation
 * - MerkleJettonSignDataE2ETest: Merkle, Jetton & Sign Data tests
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    ConnectE2ETest::class,
    TransactionAddressAmountE2ETest::class,
    TransactionFromMessagesNetworkE2ETest::class,
    TransactionPayloadStateInitValidUntilE2ETest::class,
    MerkleJettonSignDataE2ETest::class,
)
class E2ETestSuite
