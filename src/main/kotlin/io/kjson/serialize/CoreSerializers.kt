/*
 * @(#) CoreSerializers.kt
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
import java.math.BigInteger
import java.util.BitSet
import java.util.UUID

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONConfig
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONNumber
import io.kjson.JSONSerializerFunctions.appendUUID
import io.kjson.JSONSerializerFunctions.outputUUID
import io.kjson.JSONString
import io.kjson.JSONValue
import net.pwall.json.JSONCoFunctions.outputChar
import net.pwall.json.JSONCoFunctions.outputString
import net.pwall.json.JSONFunctions
import net.pwall.util.CoIntOutput.outputInt
import net.pwall.util.CoIntOutput.outputLong
import net.pwall.util.CoIntOutput.outputUnsignedInt
import net.pwall.util.CoIntOutput.outputUnsignedLong
import net.pwall.util.CoOutput
import net.pwall.util.IntOutput
import net.pwall.util.output

data object IntSerializer : Serializer<Int> {

    override fun serialize(value: Int, config: JSONConfig, references: MutableList<Any>): JSONInt = JSONInt(value)

    override fun appendTo(a: Appendable, value: Int, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendInt(a, value)
    }

    override suspend fun output(out: CoOutput, value: Int, config: JSONConfig, references: MutableList<Any>) {
        out.outputInt(value)
    }

}

data object ShortSerializer : Serializer<Short> {

    override fun serialize(value: Short, config: JSONConfig, references: MutableList<Any>): JSONInt =
        JSONInt((value).toInt())

    override fun appendTo(a: Appendable, value: Short, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendInt(a, (value).toInt())
    }

    override suspend fun output(out: CoOutput, value: Short, config: JSONConfig, references: MutableList<Any>) {
        out.outputInt((value).toInt())
    }

}

data object ByteSerializer : Serializer<Byte> {

    override fun serialize(value: Byte, config: JSONConfig, references: MutableList<Any>): JSONInt =
        JSONInt(value.toInt())

    override fun appendTo(a: Appendable, value: Byte, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendInt(a, value.toInt())
    }

    override suspend fun output(out: CoOutput, value: Byte, config: JSONConfig, references: MutableList<Any>) {
        out.outputInt(value.toInt())
    }

}

data object LongSerializer : Serializer<Long> {

    override fun serialize(value: Long, config: JSONConfig, references: MutableList<Any>): JSONLong = JSONLong(value)

    override fun appendTo(a: Appendable, value: Long, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendLong(a, value)
    }

    override suspend fun output(out: CoOutput, value: Long, config: JSONConfig, references: MutableList<Any>) {
        out.outputLong(value)
    }

}

data object UIntSerializer : Serializer<UInt> {

    override fun serialize(value: UInt, config: JSONConfig, references: MutableList<Any>): JSONNumber {
        val int = value.toInt()
        return if (int >= 0)
            JSONInt(int)
        else
            JSONLong(int.toLong() and 0xFFFFFFFFL)
    }

    override fun appendTo(a: Appendable, value: UInt, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendUnsignedInt(a, value.toInt())
    }

    override suspend fun output(out: CoOutput, value: UInt, config: JSONConfig, references: MutableList<Any>) {
        out.outputUnsignedInt(value.toInt())
    }

}

data object UShortSerializer : Serializer<UShort> {

    override fun serialize(value: UShort, config: JSONConfig, references: MutableList<Any>): JSONInt =
        JSONInt(value.toInt())

    override fun appendTo(a: Appendable, value: UShort, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendUnsignedInt(a, value.toInt())
    }

    override suspend fun output(out: CoOutput, value: UShort, config: JSONConfig, references: MutableList<Any>) {
        out.outputUnsignedInt(value.toInt())
    }

}

data object UByteSerializer : Serializer<UByte> {

    override fun serialize(value: UByte, config: JSONConfig, references: MutableList<Any>): JSONInt =
        JSONInt(value.toInt())

    override fun appendTo(a: Appendable, value: UByte, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendUnsignedInt(a, value.toInt())
    }

    override suspend fun output(out: CoOutput, value: UByte, config: JSONConfig, references: MutableList<Any>) {
        out.outputUnsignedInt(value.toInt())
    }

}

data object ULongSerializer : Serializer<ULong> {

    override fun serialize(value: ULong, config: JSONConfig, references: MutableList<Any>): JSONNumber {
        val long = value.toLong()
        return if (long >= 0)
            JSONLong(long)
        else
            JSONDecimal(value.toString())
    }

    override fun appendTo(a: Appendable, value: ULong, config: JSONConfig, references: MutableList<Any>) {
        IntOutput.appendUnsignedLong(a, value.toLong())
    }

    override suspend fun output(out: CoOutput, value: ULong, config: JSONConfig, references: MutableList<Any>) {
        out.outputUnsignedLong(value.toLong())
    }

}

data object BigIntegerSerializer : Serializer<BigInteger> {

    override fun serialize(value: BigInteger, config: JSONConfig, references: MutableList<Any>): JSONValue {
        val string = value.toString()
        return if (config.bigIntegerString)
            JSONString(string)
        else
            JSONDecimal(string)
    }

    override fun appendTo(a: Appendable, value: BigInteger, config: JSONConfig, references: MutableList<Any>) {
        if (config.bigIntegerString) {
            a.append('"')
            a.append(value.toString())
            a.append('"')
        }
        else
            a.append(value.toString())
    }

    override suspend fun output(out: CoOutput, value: BigInteger, config: JSONConfig, references: MutableList<Any>) {
        if (config.bigIntegerString) {
            out.output('"')
            out.output(value.toString())
            out.output('"')
        }
        else
            out.output(value.toString())
    }

}

data object BigDecimalSerializer : Serializer<BigDecimal> {

    override fun serialize(value: BigDecimal, config: JSONConfig, references: MutableList<Any>): JSONValue {
        val string = value.toString()
        return if (config.bigDecimalString)
            JSONString(string)
        else
            JSONDecimal(string)
    }

    override fun appendTo(a: Appendable, value: BigDecimal, config: JSONConfig, references: MutableList<Any>) {
        if (config.bigDecimalString) {
            a.append('"')
            a.append(value.toString())
            a.append('"')
        }
        else
            a.append(value.toString())
    }

    override suspend fun output(out: CoOutput, value: BigDecimal, config: JSONConfig, references: MutableList<Any>) {
        if (config.bigDecimalString) {
            out.output('"')
            out.output(value.toString())
            out.output('"')
        }
        else
            out.output(value.toString())
    }

}

data object BooleanSerializer : Serializer<Boolean> {

    override fun serialize(value: Boolean, config: JSONConfig, references: MutableList<Any>): JSONBoolean =
        JSONBoolean.of(value)

    override fun appendTo(a: Appendable, value: Boolean, config: JSONConfig, references: MutableList<Any>) {
        a.append(value.toString())
    }

    override suspend fun output(out: CoOutput, value: Boolean, config: JSONConfig, references: MutableList<Any>) {
        out.output(value.toString())
    }

}

data object FloatingPointSerializer : Serializer<Any> {

    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONDecimal =
        JSONDecimal(value.toString())

    override fun appendTo(a: Appendable, value: Any, config: JSONConfig, references: MutableList<Any>) {
        a.append(value.toString())
    }

    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        out.output(value.toString())
    }

}

/**
 * A [Serializer] to output objects for which the appropriate representation is a [String] obtained by `toString()`,
 * where the content of the content of the string does not require escaping (for example [Enum] classes).
 */
data object ToStringSerializer : StringSerializer<Any> {

    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString(value.toString())

    override fun appendString(a: Appendable, value: Any, config: JSONConfig) {
        a.append(value.toString())
    }

    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.output(value.toString())
        out.output('"')
    }

    override fun getString(value: Any): String = value.toString()

}

/**
 * A [Serializer] to output objects for which the appropriate representation is a [String] obtained by `toString()`,
 * escaping the content as required for JSON.
 */
data object ToStringUnsafeSerializer : StringSerializer<Any> {

    override fun serialize(value: Any, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString(value.toString())

    override fun appendString(a: Appendable, value: Any, config: JSONConfig) {
        for (ch in value.toString())
            JSONFunctions.appendChar(a, ch, config.stringifyNonASCII)
    }

    override suspend fun output(out: CoOutput, value: Any, config: JSONConfig, references: MutableList<Any>) {
        out.outputString(value.toString(), config.stringifyNonASCII)
    }

}

data object CharSequenceSerializer : StringSerializer<CharSequence> {

    override fun serialize(value: CharSequence, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString(value.toString())

    override fun appendString(a: Appendable, value: CharSequence, config: JSONConfig) {
        for (ch in value)
            JSONFunctions.appendChar(a, ch, config.stringifyNonASCII)
    }

    override suspend fun output(out: CoOutput, value: CharSequence, config: JSONConfig, references: MutableList<Any>) {
        out.outputString(value, config.stringifyNonASCII)
    }

}

data object UUIDSerializer : StringSerializer<UUID> {

    override fun serialize(value: UUID, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { appendUUID(value) }

    override fun appendString(a: Appendable, value: UUID, config: JSONConfig) {
        a.appendUUID(value)
    }

    override suspend fun output(out: CoOutput, value: UUID, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputUUID(value)
        out.output('"')
    }

}

data object CharSerializer : StringSerializer<Char> {

    override fun serialize(value: Char, config: JSONConfig, references: MutableList<Any>): JSONString =
        JSONString.build { JSONFunctions.appendChar(this, value, config.stringifyNonASCII) }

    override fun appendString(a: Appendable, value: Char, config: JSONConfig) {
        JSONFunctions.appendChar(a, value, config.stringifyNonASCII)
    }

    override suspend fun output(out: CoOutput, value: Char, config: JSONConfig, references: MutableList<Any>) {
        out.output('"')
        out.outputChar(value, config.stringifyNonASCII)
        out.output('"')
    }

}

data object BitSetSerializer : Serializer<BitSet> {

    override fun serialize(value: BitSet, config: JSONConfig, references: MutableList<Any>): JSONArray =
        JSONArray.build {
            for (i in 0 until value.length())
                if (value.get(i))
                    add(JSONInt.of(i))
        }

    override fun appendTo(a: Appendable, value: BitSet, config: JSONConfig, references: MutableList<Any>) {
        a.append('[')
        var continuation = false
        for (i in 0 until value.length()) {
            if (value.get(i)) {
                if (continuation)
                    a.append(',')
                IntOutput.appendInt(a, i)
                continuation = true
            }
        }
        a.append(']')
    }

    override suspend fun output(out: CoOutput, value: BitSet, config: JSONConfig, references: MutableList<Any>) {
        out.output('[')
        var continuation = false
        for (i in 0 until value.length()) {
            if (value.get(i)) {
                if (continuation)
                    out.output(',')
                out.outputInt(i)
                continuation = true
            }
        }
        out.output(']')
    }

}

data object JSONValueSerializer : Serializer<JSONValue> {

    override fun serialize(value: JSONValue, config: JSONConfig, references: MutableList<Any>): JSONValue = value

    override fun appendTo(a: Appendable, value: JSONValue, config: JSONConfig, references: MutableList<Any>) {
        value.appendTo(a)
    }

    override suspend fun output(out: CoOutput, value: JSONValue, config: JSONConfig, references: MutableList<Any>) {
        value.coOutputTo(out)
    }

}
