/*
 * @(#) CollectionSerializers.kt
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

import java.math.BigDecimal
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream

import io.jstuff.json.JSONFunctions
import io.jstuff.util.IntOutput.appendInt
import io.jstuff.util.IntOutput.appendLong
import io.kstuff.json.JSONCoFunctions.outputChar
import io.kstuff.util.CoIntOutput.outputInt
import io.kstuff.util.CoIntOutput.outputLong
import io.kstuff.util.CoOutput
import io.kstuff.util.output

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONConfig
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONString

object CharArraySerializer : StringSerializer<CharArray> {

    override fun serialize(value: CharArray, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build {
            for (ch in value)
                JSONFunctions.appendChar(this, ch, config.stringifyNonASCII)
        }

    override fun appendString(a: Appendable, value: CharArray, config: JSONConfig) {
        for (ch in value)
            JSONFunctions.appendChar(a, ch, config.stringifyNonASCII)
    }

    override suspend fun output(out: CoOutput, value: CharArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        for (ch in value)
            out.outputChar(ch, config.stringifyNonASCII)
        out.output('"')
    }

}

data object IntArraySerializer : Serializer<IntArray> {

    override fun serialize(value: IntArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONInt(item))
        }

    override fun appendTo(a: Appendable, value: IntArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                appendInt(a, value[i])
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: IntArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.outputInt(value[i])
            }
        }
        out.output(']')
    }

}

data object LongArraySerializer : Serializer<LongArray> {

    override fun serialize(value: LongArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONLong(item))
        }

    override fun appendTo(a: Appendable, value: LongArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                appendLong(a, value[i])
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: LongArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.outputLong(value[i])
            }
        }
        out.output(']')
    }

}

data object ByteArraySerializer : Serializer<ByteArray> {

    override fun serialize(value: ByteArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONInt(item.toInt()))
        }

    override fun appendTo(a: Appendable, value: ByteArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                appendInt(a, value[i].toInt())
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: ByteArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.outputInt(value[i].toInt())
            }
        }
        out.output(']')
    }

}

data object ShortArraySerializer : Serializer<ShortArray> {

    override fun serialize(value: ShortArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONInt(item.toInt()))
        }

    override fun appendTo(a: Appendable, value: ShortArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                appendInt(a, value[i].toInt())
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: ShortArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.outputInt(value[i].toInt())
            }
        }
        out.output(']')
    }

}

data object FloatArraySerializer : Serializer<FloatArray> {

    override fun serialize(value: FloatArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONDecimal(item.toString()))
        }

    override fun appendTo(a: Appendable, value: FloatArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                a.append(value[i].toString())
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: FloatArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.output(value[i].toString())
            }
        }
        out.output(']')
    }

}

data object DoubleArraySerializer : Serializer<DoubleArray> {

    override fun serialize(value: DoubleArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONDecimal(item.toString()))
        }

    override fun appendTo(a: Appendable, value: DoubleArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                a.append(value[i].toString())
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: DoubleArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.output(value[i].toString())
            }
        }
        out.output(']')
    }

}

data object BooleanArraySerializer : Serializer<BooleanArray> {

    override fun serialize(value: BooleanArray, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (item in value)
                add(JSONBoolean.of(item))
        }

    override fun appendTo(a: Appendable, value: BooleanArray, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    a.append(',')
                a.append(value[i].toString())
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: BooleanArray, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        if (value.isNotEmpty()) {
            for (i in value.indices) {
                if (i > 0)
                    out.output(',')
                out.output(value[i].toString())
            }
        }
        out.output(']')
    }

}

data object IntStreamSerializer : Serializer<IntStream> {

    override fun serialize(value: IntStream, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            val iterator = value.iterator()
            while (iterator.hasNext())
                add(JSONInt(iterator.next()))
        }

    override fun appendTo(a: Appendable, value: IntStream, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            while (true) {
                appendInt(a, iterator.next())
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: IntStream, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            while (true) {
                out.outputInt(iterator.next())
                if (!iterator.hasNext())
                    break
                out.output(',')
            }
        }
        out.output(']')
    }

}

data object LongStreamSerializer : Serializer<LongStream> {

    override fun serialize(value: LongStream, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            val iterator = value.iterator()
            while (iterator.hasNext())
                add(JSONLong(iterator.next()))
        }

    override fun appendTo(a: Appendable, value: LongStream, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            while (true) {
                appendLong(a, iterator.next())
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: LongStream, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            while (true) {
                out.outputLong(iterator.next())
                if (!iterator.hasNext())
                    break
                out.output(',')
            }
        }
        out.output(']')
    }

}

data object DoubleStreamSerializer : Serializer<DoubleStream> {

    override fun serialize(value: DoubleStream, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            val iterator = value.iterator()
            while (iterator.hasNext())
                add(JSONDecimal(BigDecimal(iterator.next())))
        }

    override fun appendTo(a: Appendable, value: DoubleStream, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            while (true) {
                a.append(iterator.next().toString())
                if (!iterator.hasNext())
                    break
                a.append(',')
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: DoubleStream, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        val iterator = value.iterator()
        if (iterator.hasNext()) {
            while (true) {
                out.output(iterator.next().toString())
                if (!iterator.hasNext())
                    break
                out.output(',')
            }
        }
        out.output(']')
    }

}
