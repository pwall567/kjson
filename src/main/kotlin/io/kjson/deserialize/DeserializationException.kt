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

import io.kjson.JSON.displayValue
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.pointer.JSONPointer

class DeserializationException(
    val text: String = "Deserialization error",
    val pointer: JSONPointer = JSONPointer.root,
    val underlying: Throwable? = null,
    val messageFunction: (JSONValue?) -> String = { text }
) : Exception() {

    constructor(text: String, index: Int) : this(text, JSONPointer.root.child(index))

    constructor(text: String, propertyName: String) : this(text, JSONPointer.root.child(propertyName))

    fun nested(pointer: JSONPointer): DeserializationException =
        DeserializationException(text, this.pointer.withParent(pointer), underlying, messageFunction)

    fun nested(name: String): DeserializationException = nested(JSONPointer.root.child(name))

}

fun cantDeserializeException(expected: String) = DeserializationException { json: JSONValue? ->
    "Can't deserialize ${json.errorDisplay()} as $expected"
}

fun typeException(expected: String, pointer: JSONPointer = JSONPointer.root) =
    DeserializationException(pointer = pointer) { json: JSONValue? ->
        "Incorrect type, expected $expected but was ${json.errorDisplay()}"
    }

fun JSONValue?.errorDisplay(maxNames: Int = 5): String = if (this is JSONObject && isNotEmpty()) buildString {
    append('{')
    append(' ')
    var i = 0
    while (true) {
        append(this@errorDisplay[i++].name)
        if (i >= this@errorDisplay.size)
            break
        append(',')
        append(' ')
        if (i >= maxNames) {
            append("...")
            break
        }
    }
    append(' ')
    append('}')
} else displayValue()
