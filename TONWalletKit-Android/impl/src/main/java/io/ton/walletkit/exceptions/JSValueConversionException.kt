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
package io.ton.walletkit.exceptions

/**
 * Exception thrown when conversion between JS values and Kotlin types fails.
 *
 * This sealed class represents various type conversion failures that can occur when
 * bridging between JavaScript and Kotlin/Android native code. It handles scenarios like:
 * - Type mismatches in JS-Kotlin bridge
 * - Null/undefined JS value conversions
 * - JSON encoding/decoding failures
 * - BigInt and complex type conversions
 *
 * This class mirrors the iOS SDK's `JSValueConversionError` enum for cross-platform consistency.
 *
 * @see JSException
 */
internal sealed class JSValueConversionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Unable to convert a JS value to the target Kotlin type.
     *
     * Example:
     * ```kotlin
     * throw JSValueConversionException.UnableToConvertJSValue(
     *     targetType = "Int",
     *     jsValueDescription = "string 'hello'"
     * )
     * // Error: "Unable to cast JS value 'string 'hello'' to Int"
     * ```
     *
     * @property targetType The Kotlin type that conversion was attempted to
     * @property jsValueDescription String description of the JS value that failed to convert
     */
    class UnableToConvertJSValue(
        val targetType: String,
        val jsValueDescription: String,
    ) : JSValueConversionException(
        "Unable to cast JS value '$jsValueDescription' to $targetType",
    )

    /**
     * Attempted to convert an undefined JS value to a non-nullable Kotlin type.
     *
     * Example:
     * ```kotlin
     * throw JSValueConversionException.UndefinedValue("String")
     * // Error: "Unable to cast undefined JS value to String"
     * ```
     *
     * @property targetType The Kotlin type that conversion was attempted to
     */
    class UndefinedValue(
        val targetType: String,
    ) : JSValueConversionException(
        "Unable to cast undefined JS value to $targetType",
    )

    /**
     * Attempted to convert a null JS value to a non-nullable Kotlin type.
     *
     * Example:
     * ```kotlin
     * throw JSValueConversionException.NullValue("Int")
     * // Error: "Unable to cast null JS value to Int"
     * ```
     *
     * @property targetType The Kotlin type that conversion was attempted to
     */
    class NullValue(
        val targetType: String,
    ) : JSValueConversionException(
        "Unable to cast null JS value to $targetType",
    )

    /**
     * JSON decoding failed during JS to Kotlin conversion.
     *
     * Example:
     * ```kotlin
     * throw JSValueConversionException.DecodingError(
     *     message = "Expected START_OBJECT but was STRING at path $.wallet",
     *     cause = jsonException
     * )
     * ```
     *
     * @param message Detailed error message about the decoding failure
     * @param cause The underlying exception that caused the decoding failure
     */
    class DecodingError(
        message: String,
        cause: Throwable,
    ) : JSValueConversionException("Decoding error: $message", cause)

    /**
     * JSON encoding failed during Kotlin to JS conversion.
     *
     * Example:
     * ```kotlin
     * throw JSValueConversionException.EncodingError(
     *     message = "Serializer for class 'Wallet' is not found",
     *     cause = serializationException
     * )
     * ```
     *
     * @param message Detailed error message about the encoding failure
     * @param cause The underlying exception that caused the encoding failure
     */
    class EncodingError(
        message: String,
        cause: Throwable,
    ) : JSValueConversionException("Encoding error: $message", cause)

    /**
     * Unknown or unclassified conversion error.
     *
     * Example:
     * ```kotlin
     * throw JSValueConversionException.Unknown(
     *     message = "Unable to convert JS value BigInt to Kotlin type",
     *     cause = originalException
     * )
     * ```
     *
     * @param message Description of the unknown conversion error
     * @param cause Optional underlying cause of the error
     */
    class Unknown(
        message: String,
        cause: Throwable? = null,
    ) : JSValueConversionException(message, cause)
}
