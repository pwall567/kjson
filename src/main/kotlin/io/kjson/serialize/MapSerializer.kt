/*
 * @(#) MapSerializer.kt
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

import io.kjson.JSONConfig
import io.kjson.JSONKotlinException
import io.kjson.JSONObject
import io.kjson.pointer.JSONPointer
import net.pwall.json.JSONCoFunctions.outputString
import net.pwall.json.JSONFunctions
import net.pwall.util.CoOutput
import net.pwall.util.output

class MapSerializer<K : Any, V : Any, M : Map<K, V?>>(
    private val keyClass: KClass<K>,
    private val keySerializer: StringSerializer<K>,
    private val valueSerializer: Serializer<V>,
) : Serializer<M> {

    override fun serialize(value: M, config: JSONConfig, references: MutableList<Any>): JSONObject = JSONObject.build {
        val iterator = value.entries.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val (key, item) = iterator.next()
            val keyString = if (key::class == keyClass)
                keySerializer.getString(key)
            else
                getKeyString(key, index, config)
            add(keyString, valueSerializer.serializeNested(item, config, references, keyString))
            index++
        }
    }

    override fun appendTo(a: Appendable, value: M, config: JSONConfig, references: MutableList<Any>) {
        a.append('{')
        val iterator = value.entries.iterator()
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                val (key, item) = iterator.next()
                val keyString = if (key::class == keyClass)
                    keySerializer.getString(key)
                else
                    getKeyString(key, index, config)
                JSONFunctions.appendString(a, keyString, config.stringifyNonASCII)
                a.append(':')
                valueSerializer.appendNestedTo(a, item, config, references, keyString)
                if (!iterator.hasNext())
                    break
                index++
                a.append(',')
            }
        }
        a.append('}')
    }

    override suspend fun output(out: CoOutput, value: M, config: JSONConfig, references: MutableList<Any>) {
        out.output('{')
        val iterator = value.entries.iterator()
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                val (key, item) = iterator.next()
                val keyString = if (key::class == keyClass)
                    keySerializer.getString(key)
                else
                    getKeyString(key, index, config)
                out.outputString(keyString, config.stringifyNonASCII)
                out.output(':')
                valueSerializer.outputNestedTo(out, item, config, references, keyString)
                if (!iterator.hasNext())
                    break
                index++
                out.output(',')
            }
        }
        out.output('}')
    }

    @Suppress("unchecked_cast")
    private fun getKeyString(key: Any, index: Int, config: JSONConfig): String {
        val serializer = config.findSerializer(key::class)
        if (serializer !is StringSerializer)
            throw JSONKotlinException("Invalid key in Map", JSONPointer("/$index"))
        return (serializer as StringSerializer<Any>).getString(key)
    }

}
