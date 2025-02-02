/*
 * @(#) CommonIterableSerializer.kt
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

import io.kstuff.util.CoOutput
import io.kstuff.util.output

import io.kjson.JSONArray
import io.kjson.JSONConfig

abstract class CommonIterableSerializer<I : Any>( // use this for List, Sequence etc.
    private val itemSerializer: Serializer<I>,
) {

    fun serializeIterator(iterator: Iterator<I?>, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            var i = 0
            while (iterator.hasNext()) {
                val item = iterator.next()
                add(itemSerializer.serializeNested(item, config, references, i.toString()))
                i++
            }
        }

    fun appendIteratorTo(a: Appendable, iterator: Iterator<I?>, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (iterator.hasNext()) {
            var i = 0
            while (true) {
                val item = iterator.next()
                itemSerializer.appendNestedTo(a, item, config, references, i.toString())
                if (!iterator.hasNext())
                    break
                i++
                a.append(',')
            }
        }
        a.append(']')
    }

    suspend fun outputIterator(out: CoOutput, iterator: Iterator<I?>, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                val item = iterator.next()
                itemSerializer.outputNestedTo(out, item, config, references, index.toString())
                if (!iterator.hasNext())
                    break
                index++
                out.output(',')
            }
        }
        out.output(']')
    }

}
