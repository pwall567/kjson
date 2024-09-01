/*
 * @(#) DeserializationException.kt
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

import io.kjson.JSON.displayValue
import io.kjson.JSONDeserializerFunctions.displayName
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer
import io.kjson.pointer.find

class DeserializationException(
    val text: String = "dynamicException",
    val pointer: JSONPointer = JSONPointer.root,
    val underlying: Throwable? = null,
    val messageFunction: (KType, JSONValue?) -> String = { _: KType, _: JSONValue? -> text }
) : Exception() {

    constructor(text: String, index: Int) : this(text, JSONPointer.root.child(index))

    constructor(text: String, propertyName: String) : this(text, JSONPointer.root.child(propertyName))

    fun nested(pointer: JSONPointer): DeserializationException =
        DeserializationException(text, this.pointer.withParent(pointer), underlying) {
            resultType: KType, json: JSONValue? -> messageFunction(resultType, pointer.find(json))
        }

    fun nested(name: String): DeserializationException = nested(JSONPointer.root.child(name))

}

val cantDeserializeException = DeserializationException { resultType: KType, _: JSONValue? ->
    "Can't deserialize ${resultType.displayName()}"
}

fun typeException(expected: String, pointer: JSONPointer = JSONPointer.root) =
    DeserializationException(pointer = pointer) {
        _: KType, json: JSONValue? -> "Incorrect type, expected $expected but was ${pointer.find(json).displayValue()}"
    }
