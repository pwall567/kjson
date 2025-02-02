/*
 * @(#) AnySerializer.kt
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

import io.kjson.JSONConfig
import io.kjson.JSONObject
import io.kjson.JSONValue
import io.kjson.serialize.Serializer.Companion.findSerializer

data object AnySerializer : Serializer<Any> {

    @Suppress("unchecked_cast")
    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONValue? {
        if (value::class != Any::class) {
            val serializer = findSerializer(value::class, config) as Serializer<Any>
            if (serializer !is AnySerializer)
                return serializer.serialize(value, config, references)
        }
        return JSONObject.EMPTY
    }

    @Suppress("unchecked_cast")
    override fun appendTo(a: Appendable, value: Any, config: JSONConfig, references: MutableList<Any>) {
        if (value::class != Any::class) {
            val serializer = findSerializer(value::class, config) as Serializer<Any>
            if (serializer != AnySerializer)
                serializer.appendTo(a, value, config, references)
        }
        else
            a.append("{}")
    }

    @Suppress("unchecked_cast")
    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        if (value::class != Any::class) {
            val serializer = findSerializer(value::class, config) as Serializer<Any>
            if (serializer !is AnySerializer)
                serializer.output(out, value, config, references)
        }
        else
            out.output("{}")
    }

}
