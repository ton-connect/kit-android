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
package io.ton.walletkit.demo.e2e.infrastructure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Client for interacting with Allure TestOps API.
 */
class AllureApiClient(private val config: AllureConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedToken: String? = null
    private var tokenExpiry: Long = 0

    /**
     * Get a valid OAuth token, refreshing if necessary.
     */
    private fun getValidToken(): String {
        val now = System.currentTimeMillis()

        if (cachedToken != null && now < tokenExpiry) {
            return cachedToken!!
        }

        val formBody = FormBody.Builder()
            .add("grant_type", "apitoken")
            .add("scope", "openid")
            .add("token", config.apiToken)
            .build()

        val request = Request.Builder()
            .url("${config.baseUrl}/api/uaa/oauth/token")
            .post(formBody)
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to get Allure token: ${response.code} ${response.message}")
        }

        val tokenResponse = json.decodeFromString<TokenResponse>(response.body?.string() ?: "")
        cachedToken = tokenResponse.accessToken
        // Token expires in 1 hour, refresh 5 minutes early
        tokenExpiry = now + (55 * 60 * 1000)

        return cachedToken!!
    }

    /**
     * Make an authenticated request to the Allure API.
     */
    private fun makeRequest(endpoint: String): String {
        val token = getValidToken()

        val request = Request.Builder()
            .url("${config.baseUrl}$endpoint")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("API request failed: ${response.code} ${response.message}")
        }

        return response.body?.string() ?: ""
    }

    /**
     * Get test case data by Allure ID.
     */
    fun getTestCase(allureId: String): TestCaseResponse {
        val responseBody = makeRequest("/api/rs/testcase/$allureId")
        return json.decodeFromString(responseBody)
    }

    /**
     * Get test case data including precondition and expected result.
     */
    fun getTestCaseData(allureId: String): TestCaseData {
        val testCase = getTestCase(allureId)

        // Extract precondition and expected result from test case
        val precondition = testCase.precondition ?: ""
        val expectedResult = testCase.expectedResult ?: ""
        val isPositiveCase = !testCase.name.contains("[ERROR]")

        return TestCaseData(
            precondition = precondition,
            expectedResult = expectedResult,
            isPositiveCase = isPositiveCase,
        )
    }

    companion object {
        /**
         * Extract Allure ID from test title.
         * Example: "Successful Connect @allureId(2294)" -> "2294"
         */
        fun extractAllureId(title: String): String? {
            val regex = Regex("@allureId\\((\\d+)\\)")
            return regex.find(title)?.groupValues?.getOrNull(1)
        }
    }
}

/**
 * Configuration for Allure TestOps connection.
 */
data class AllureConfig(
    val baseUrl: String = "https://tontech.testops.cloud",
    val apiToken: String,
    val projectId: Int = 100,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    val scope: String,
)

@Serializable
data class TestCaseResponse(
    val id: Long,
    val name: String,
    val precondition: String? = null,
    val expectedResult: String? = null,
)

/**
 * Processed test case data ready for use in tests.
 */
data class TestCaseData(
    val precondition: String,
    val expectedResult: String,
    val isPositiveCase: Boolean,
)
