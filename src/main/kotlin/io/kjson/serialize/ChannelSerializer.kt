/*
 * @(#) ChannelSerializer.kt
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

import kotlinx.coroutines.channels.Channel

import io.kjson.JSONConfig
import io.kjson.JSONKotlinException
import io.kjson.JSONValue
import net.pwall.util.CoOutput
import net.pwall.util.CoOutputFlushable
import net.pwall.util.output

class ChannelSerializer<I : Any>(
    private val itemSerializer: Serializer<I>,
) : Serializer<Channel<I>> {

    override fun serialize(value: Channel<I>, config: JSONConfig, references: MutableList<Any>): JSONValue? {
        throw JSONKotlinException("Can't serialize Channel")
    }

    override fun appendTo(a: Appendable, value: Channel<I>, config: JSONConfig, references: MutableList<Any>) {
        throw JSONKotlinException("Can't append Channel")
    }

    override suspend fun output(out: CoOutput, value: Channel<I>, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            var index = 0
            while (true) {
                val item = iterator.next()
                itemSerializer.outputNestedTo(out, item, config, references, index.toString())
                if (out is CoOutputFlushable)
                    out.flush()
                if (!iterator.hasNext())
                    break
                out.output(',')
                index++
            }
        }
        out.output(']')
    }

}
