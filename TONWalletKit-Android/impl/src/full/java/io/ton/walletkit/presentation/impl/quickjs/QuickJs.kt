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
package io.ton.walletkit.presentation.impl.quickjs

import java.io.Closeable
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

private val IGNORED_METHOD_NAMES = setOf("equals", "hashCode", "toString", "wait", "notify", "notifyAll")

/**
 * QuickJS engine wrapper.
 * @suppress Internal implementation class. Not part of public API.
 */
internal class QuickJs private constructor(private var nativePointer: Long) : Closeable {
    private val closed = AtomicBoolean(false)

    init {
        check(nativePointer != 0L) { "Invalid QuickJs pointer" }
    }

    fun evaluate(script: String, filename: String = "<eval>"): Any? {
        require(script.isNotEmpty()) { "script cannot be empty" }
        val pointer = ensureOpen()
        return nativeEvaluate(pointer, script, filename)
    }

    fun executePendingJob(): Int {
        val pointer = ensureOpen()
        return nativeExecutePendingJob(pointer)
    }

    fun set(name: String, type: Class<*>, instance: Any) {
        require(name.isNotBlank()) { "name cannot be blank" }
        val pointer = ensureOpen()
        require(type.isInstance(instance)) { "instance is not of type ${type.name}" }
        val methods = type.declaredMethods.filter { method ->
            val name = method.name
            if (name in IGNORED_METHOD_NAMES) {
                return@filter false
            }
            !method.isSynthetic && !method.isBridge && method.parameterTypes.all { isSupportedType(it) } &&
                isSupportedReturnType(method.returnType)
        }
        if (methods.isEmpty()) {
            return
        }
        for (method in methods) {
            method.isAccessible = true
            nativeRegister(
                pointer = pointer,
                objectName = name,
                methodName = method.name,
                method = method,
                instance = instance,
                parameterTypes = method.parameterTypes,
                returnType = method.returnType,
            )
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            val pointer = nativePointer
            nativePointer = 0L
            if (pointer != 0L) {
                nativeDestroy(pointer)
            }
        }
    }

    private fun ensureOpen(): Long {
        check(!closed.get()) { "QuickJs instance already closed" }
        return nativePointer
    }

    private fun isSupportedType(type: Class<*>): Boolean = when (type) {
        java.lang.String::class.java,
        java.lang.Boolean::class.java,
        java.lang.Boolean.TYPE,
        java.lang.Integer::class.java,
        java.lang.Integer.TYPE,
        java.lang.Long::class.java,
        java.lang.Long.TYPE,
        java.lang.Double::class.java,
        java.lang.Double.TYPE,
        -> true
        else -> false
    }

    private fun isSupportedReturnType(type: Class<*>): Boolean = type == java.lang.Void.TYPE || type == java.lang.Void::class.java || isSupportedType(type)

    private external fun nativeEvaluate(pointer: Long, script: String, filename: String): Any?

    private external fun nativeExecutePendingJob(pointer: Long): Int

    private external fun nativeRegister(
        pointer: Long,
        objectName: String,
        methodName: String,
        method: Method,
        instance: Any,
        parameterTypes: Array<Class<*>>,
        returnType: Class<*>,
    )

    private external fun nativeDestroy(pointer: Long)

    companion object {
        init {
            QuickJsNativeLoader.load()
        }

        fun create(): QuickJs {
            val pointer = nativeCreate()
            if (pointer == 0L) {
                throw QuickJsException("Failed to create QuickJs runtime")
            }
            return QuickJs(pointer)
        }

        @JvmStatic
        private external fun nativeCreate(): Long
    }
}
