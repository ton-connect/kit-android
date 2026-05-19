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
package io.ton.walletkit.bridge

import io.ton.walletkit.model.TONBase64
import io.ton.walletkit.model.TONHex
import io.ton.walletkit.model.TONRawAddress
import io.ton.walletkit.model.TONTokenAmount
import io.ton.walletkit.model.TONUserFriendlyAddress
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

inline fun <reified T : Any> Json.decodeFromBridge(raw: JsonElement?): T {
    val klass = T::class
    if (raw == null || raw is JsonNull) {
        throw BridgeConversionError.UnableToConvertNull(klass)
    }

    decodeDomainType(klass, raw)?.let {
        @Suppress("UNCHECKED_CAST")
        return it as T
    }

    val primitive = decodePrimitive(klass, raw)
    if (primitive != null) {
        @Suppress("UNCHECKED_CAST")
        return primitive as T
    }

    return try {
        @Suppress("UNCHECKED_CAST")
        val ks = serializersModule.serializer<T>() as KSerializer<T>
        decodeFromJsonElement(ks, raw)
    } catch (e: BridgeConversionError) {
        throw e
    } catch (e: Throwable) {
        throw BridgeConversionError.UnableToDecode(klass, e)
    }
}

@PublishedApi
internal fun decodeDomainType(klass: KClass<*>, raw: JsonElement): Any? {
    val string = raw.asStringOrNull()
    return when (klass) {
        TONHex::class -> string?.let(::TONHex)
            ?: throw BridgeConversionError.UnableToConvert(klass, raw)
        TONBase64::class -> string?.let(::TONBase64)
            ?: throw BridgeConversionError.UnableToConvert(klass, raw)
        TONTokenAmount::class -> string?.let(::TONTokenAmount)
            ?: throw BridgeConversionError.UnableToConvert(klass, raw)
        TONUserFriendlyAddress::class ->
            string?.let { runCatching { TONUserFriendlyAddress.parse(it) }.getOrNull() }
                ?: throw BridgeConversionError.UnableToConvert(klass, raw)
        TONRawAddress::class ->
            string?.let { runCatching { TONRawAddress.parse(it) }.getOrNull() }
                ?: throw BridgeConversionError.UnableToConvert(klass, raw)
        else -> null
    }
}

inline fun <reified T : Any> Json.decodeFromBridgeOrNull(raw: JsonElement?): T? =
    if (raw == null || raw is JsonNull) null else decodeFromBridge<T>(raw)

@PublishedApi
internal fun decodePrimitive(klass: KClass<*>, raw: JsonElement): Any? {
    val primitive = raw as? JsonPrimitive ?: return null
    return when (klass) {
        String::class -> primitive.contentOrNull()
        Boolean::class -> primitive.booleanOrNull
        Int::class -> primitive.intOrNull
        Long::class -> primitive.longOrNull
        Short::class -> primitive.intOrNull?.toShort()
        Byte::class -> primitive.intOrNull?.toByte()
        Float::class -> primitive.doubleOrNull?.toFloat()
        Double::class -> primitive.doubleOrNull
        else -> null
    }
}

private fun JsonPrimitive.contentOrNull(): String? = if (this is JsonNull) null else content
