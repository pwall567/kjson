/*
 * @(#) FlowSerializer.kt
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

import kotlinx.coroutines.flow.Flow

import io.kstuff.util.CoOutput
import io.kstuff.util.CoOutputFlushable
import io.kstuff.util.output

import io.kjson.JSONConfig
import io.kjson.JSONKotlinException
import io.kjson.JSONValue

class FlowSerializer<I : Any>(
    private val itemSerializer: Serializer<I>,
) : Serializer<Flow<I>> {

    override fun serialize(value: Flow<I>, config: JSONConfig, references: MutableList<Any>): JSONValue? {
        throw JSONKotlinException("Can't serialize Flow")
    }

    override fun appendTo(a: Appendable, value: Flow<I>, config: JSONConfig, references: MutableList<Any>) {
        throw JSONKotlinException("Can't append Flow")
    }

    override suspend fun output(out: CoOutput, value: Flow<I>, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        var index = 0
        value.collect { item ->
            if (index != 0)
                out.output(',')
            itemSerializer.outputNestedTo(out, item, config, references, index.toString())
            if (out is CoOutputFlushable)
                out.flush()
            index++
        }
        out.output(']')
    }

}
