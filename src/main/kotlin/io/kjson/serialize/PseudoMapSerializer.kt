/*
 * @(#) PseudoMapSerializer.kt
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

class PseudoMapSerializer<K : Any, V : Any, M : Map<K?, V?>>(
    private val keySerializer: Serializer<K>,
    private val valueSerializer: Serializer<V>,
) : Serializer<M> {

    override fun serialize(value: M, config: JSONConfig, references: MutableList<Any>): JSONArray = JSONArray.build {
        val iterator = value.entries.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val (key, item) = iterator.next()
            val nestedName = index.toString()
            val pair = JSONArray.build {
                add(keySerializer.serializeNested(key, config, references, nestedName))
                add(valueSerializer.serializeNested(item, config, references, nestedName))
            }
            add(pair)
            index++
        }
    }

    override fun appendTo(a: Appendable, value: M, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        val iterator = value.entries.iterator()
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                a.append('[')
                val (key, item) = iterator.next()
                val nestedName = index.toString()
                keySerializer.appendNestedTo(a, key, config, references, nestedName)
                a.append(',')
                valueSerializer.appendNestedTo(a, item, config, references, nestedName)
                a.append(']')
                if (!iterator.hasNext())
                    break
                index++
                a.append(',')
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: M, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        val iterator = value.entries.iterator()
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                out.output('[')
                val (key, item) = iterator.next()
                val nestedName = index.toString()
                keySerializer.outputNestedTo(out, key, config, references, nestedName)
                out.output(',')
                valueSerializer.outputNestedTo(out, item, config, references, nestedName)
                out.output(']')
                if (!iterator.hasNext())
                    break
                index++
                out.output(',')
            }
        }
        out.output(']')
    }

}
