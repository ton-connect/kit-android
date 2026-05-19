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
package io.ton.walletkit.demo.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.pow

// Bridges SwiftUI's `.spring(response:dampingFraction:)` parameters to Compose's
// stiffness / dampingRatio shape so designers can copy values verbatim from the
// iOS code and get matching motion. Compose stiffness `k = (2π / response)²`.
//
// Example: SwiftUI `.spring(response: 0.3, dampingFraction: 0.8)` →
//          [iosSpring](response = 0.3f, dampingFraction = 0.8f)
fun <T> iosSpring(
    response: Float,
    dampingFraction: Float,
    visibilityThreshold: T? = null,
): SpringSpec<T> = spring(
    stiffness = (2f * PI.toFloat() / response).pow(2f),
    dampingRatio = dampingFraction,
    visibilityThreshold = visibilityThreshold,
)
