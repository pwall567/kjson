/*
 * @(#) ArraySerializer.kt
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

package io.kjson.serialize

import kotlin.reflect.KClass

import io.jstuff.json.JSONFunctions
import io.kstuff.json.JSONCoFunctions.outputChar
import io.kstuff.util.CoOutput
import io.kstuff.util.output

import io.kjson.JSONConfig
import io.kjson.JSONString
import io.kjson.JSONValue

class ArraySerializer<I : Any>(
    private val itemClass: KClass<I>,
    itemSerializer: Serializer<I>,
) : CommonIterableSerializer<I>(itemSerializer), Serializer<Array<*>> {

    @Suppress("unchecked_cast")
    override fun serialize(value: Array<*>, config: JSONConfig, references: MutableList<Any>): JSONValue =
        if (itemClass == Char::class)
            JSONString(String(CharArray(value.size) { value[it] as Char }))
        else
            serializeIterator(value.iterator() as Iterator<I>, config, references)

    @Suppress("unchecked_cast")
    override fun appendTo(a: Appendable, value: Array<*>, config: JSONConfig, references: MutableList<Any>) {
        if (itemClass == Char::class) {
            a.append('"')
            for (char in value)
                JSONFunctions.appendChar(a, char as Char, config.stringifyNonASCII)
            a.append('"')
        }
        else
            appendIteratorTo(a, value.iterator() as Iterator<I>, config, references)
    }

    @Suppress("unchecked_cast")
    override suspend fun output(out: CoOutput, value: Array<*>, config: JSONConfig, references: MutableList<Any>) {
        if (itemClass == Char::class) {
            out.output('"')
            for (char in value)
                out.outputChar(char as Char, config.stringifyNonASCII)
            out.output('"')
        }
        else
            outputIterator(out, value.iterator() as Iterator<I>, config, references)
    }

}
