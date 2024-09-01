/*
 * @(#) JSONKotlinException.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024 Peter Wall
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

package io.kjson

import io.kjson.pointer.JSONPointer

/**
 * Exception class for errors in serialization and deserialization.
 *
 * @author  Peter Wall
 */
class JSONKotlinException(
    text: String,
    val pointer: JSONPointer? = null,
    cause: Throwable? = null,
) : JSONException(text, pointer) {

    constructor(
        text: String,
        property: String,
        cause: Throwable? = null
    ) : this(text, JSONPointer.root.child(property), cause)

    constructor(
        text: String,
        item: Int,
        cause: Throwable? = null
    ) : this(text, JSONPointer.root.child(item), cause)

    init {
        if (cause != null)
            initCause(cause)
    }

    /**
     * Create a copy of this [JSONKotlinException] with the pointer prefixed with the specified pointer.
     */
    fun nested(pointer: JSONPointer) = JSONKotlinException(text, this.pointer?.withParent(pointer) ?: pointer, cause)

    /**
     * Create a copy of this [JSONKotlinException] with the pointer prefixed with the specified pointer element.
     */
    fun nested(name: String) = JSONKotlinException(text, (pointer ?: JSONPointer.root).withParent(name), cause)

    /**
     * Create a copy of this [JSONKotlinException] with the pointer prefixed with the specified pointer element.
     */
    fun nested(index: Int) = JSONKotlinException(text, (pointer ?: JSONPointer.root).withParent(index), cause)

    companion object {

        /**
         * Throw a [JSONKotlinException] with the specified parameters.
         */
        fun fatal(text: String, pointer: JSONPointer? = null, cause: Throwable? = null): Nothing {
            throw JSONKotlinException(text, pointer, cause)
        }

    }

}
