/*
 * @(#) JSONKotlinException.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021 Peter Wall
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

class JSONKotlinException(val text: String, val pointer: JSONPointer? = null) : JSONException(text) {

    constructor(text: String, pointer: JSONPointer, nested: Exception) : this(text, pointer) {
        initCause(nested)
    }

    constructor(text: String, nested: Exception) : this(text) {
        initCause(nested)
    }

    override val message: String
        get() = when (pointer) {
            null,
            JSONPointer.root -> text
            else -> "$text at $pointer"
        }

    companion object {

        internal fun fatal(text: String, pointer: JSONPointer? = null): Nothing {
            throw JSONKotlinException(text, pointer)
        }

        internal fun fatal(text: String, pointer: JSONPointer, nested: Exception): Nothing {
            throw JSONKotlinException(text, pointer, nested)
        }

        internal fun fatal(text: String, nested: Exception): Nothing {
            throw JSONKotlinException(text, nested)
        }

    }

}
