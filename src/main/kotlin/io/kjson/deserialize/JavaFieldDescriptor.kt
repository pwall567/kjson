/*
 * @(#) JavaFieldDescriptor.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2024 Peter Wall
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

package io.kjson.deserialize

import kotlin.reflect.KType

import java.lang.reflect.Method

import io.kjson.toKType

data class JavaFieldDescriptor<T>(
    override val propertyName: String,
    override val ignore: Boolean,
    override val nullable: Boolean,
    override val deserializer: Deserializer<T>,
    private val getter: Method,
    private val setter: Method?
) : FieldDescriptor<T> {

    override val returnType: KType
        get() = getter.genericReturnType.toKType(nullable = true)

    override fun isMutable(): Boolean = setter != null

    override fun getValue(instance: Any): Any? = getter.invoke(instance)

    override fun setValue(instance: Any, value: Any?) { // may be called only if isMutable() returns true
        setter!!.invoke(instance, value)
    }

}
