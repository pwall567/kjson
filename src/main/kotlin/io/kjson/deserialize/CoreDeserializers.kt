/*
 * @(#) CoreDeserializers.kt
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

import java.math.BigDecimal
import java.math.BigInteger

import io.kjson.JSON.asBooleanOr
import io.kjson.JSON.asByteOr
import io.kjson.JSON.asDecimalOr
import io.kjson.JSON.asIntOr
import io.kjson.JSON.asLongOr
import io.kjson.JSON.asShortOr
import io.kjson.JSON.asStringOr
import io.kjson.JSON.asUByteOr
import io.kjson.JSON.asUIntOr
import io.kjson.JSON.asULongOr
import io.kjson.JSON.asUShortOr
import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONDecimal
import io.kjson.JSONDeserializer.newArray
import io.kjson.JSONDeserializerFunctions.toBigInteger
import io.kjson.JSONInt
import io.kjson.JSONLong
import io.kjson.JSONNumber
import io.kjson.JSONObject
import io.kjson.JSONString
import io.kjson.JSONValue
import net.pwall.util.ImmutableList
import net.pwall.util.ImmutableMap

data object AnyDeserializer : Deserializer<Any> {
    override fun deserialize(json: JSONValue?): Any? = when (json) {
        null -> null
        is JSONInt -> json.value
        is JSONLong -> json.value
        is JSONDecimal -> json.value
        is JSONString -> json.value
        is JSONBoolean -> json.value
        is JSONArray -> ImmutableList(
            Array(json.size) { index -> deserialize(json[index]) }
        )
        is JSONObject -> ImmutableMap(
            Array(json.size) { index ->
                val property = json[index]
                ImmutableMap.entry(property.name, deserialize(property.value))
            }
        )
    }
}

data object ImpossibleSerializer : Deserializer<Nothing> {
    override fun deserialize(json: JSONValue?): Nothing {
        throw cantDeserializeException
    }
}

data object BooleanDeserializer : Deserializer<Boolean> {
    override fun deserialize(json: JSONValue?): Boolean? = json?.asBooleanOr { typeError("Boolean") }
}

data object IntDeserializer : Deserializer<Int> {
    override fun deserialize(json: JSONValue?): Int? = json?.asIntOr { typeError("Int") }
}

data object LongDeserializer : Deserializer<Long> {
    override fun deserialize(json: JSONValue?): Long? = json?.asLongOr { typeError("Long") }
}

data object DoubleDeserializer : Deserializer<Double> {
    override fun deserialize(json: JSONValue?): Double? = json?.asDecimalOr { typeError("Double") }?.toDouble()
}

data object FloatDeserializer : Deserializer<Float> {
    override fun deserialize(json: JSONValue?): Float? = json?.asDecimalOr { typeError("Float") }?.toFloat()
}

data object ShortDeserializer : Deserializer<Short> {
    override fun deserialize(json: JSONValue?): Short? = json?.asShortOr { typeError("Short") }
}

data object ByteDeserializer : Deserializer<Byte> {
    override fun deserialize(json: JSONValue?): Byte? = json?.asByteOr { typeError("Byte") }
}

data object UIntDeserializer : Deserializer<UInt> {
    override fun deserialize(json: JSONValue?): UInt? = json?.asUIntOr { typeError("object") }
}

data object ULongDeserializer : Deserializer<ULong> {
    override fun deserialize(json: JSONValue?): ULong? = json?.asULongOr { typeError("ULong") }
}

data object UShortDeserializer : Deserializer<UShort> {
    override fun deserialize(json: JSONValue?): UShort? = json?.asUShortOr { typeError("UShort") }
}

data object UByteDeserializer : Deserializer<UByte> {
    override fun deserialize(json: JSONValue?): UByte? = json?.asUByteOr { typeError("UByte") }
}

data object BigIntegerDeserializer : Deserializer<BigInteger> {
    override fun deserialize(json: JSONValue?): BigInteger? = when {
        json == null -> null
        json is JSONString -> BigInteger(json.value)
        json is JSONNumber && json.isIntegral() -> json.toBigInteger()
        else -> typeError("integer or string")
    }
}

data object BigDecimalDeserializer : Deserializer<BigDecimal> {
    override fun deserialize(json: JSONValue?): BigDecimal? = when (json) {
        null -> null
        is JSONString -> BigDecimal(json.value)
        is JSONNumber -> json.toDecimal()
        else -> typeError("decimal or string")
    }
}

data object NumberDeserializer : Deserializer<Number> {
    override fun deserialize(json: JSONValue?): Number? = when (json) {
        null -> null
        is JSONNumber -> json.value
        else -> typeError("number")
    }
}

data object StringDeserializer : Deserializer<String> {
    override fun deserialize(json: JSONValue?): String? = json?.asStringOr { typeError("string") }
}

data object StringBuilderDeserializer : Deserializer<StringBuilder> {
    override fun deserialize(json: JSONValue?): StringBuilder? = when (json) {
        null -> null
        is JSONString -> StringBuilder(json.value)
        else -> typeError("string")
    }
}

data object StringBufferDeserializer : Deserializer<StringBuffer> {
    override fun deserialize(json: JSONValue?): StringBuffer? = when (json) {
        null -> null
        is JSONString -> StringBuffer(json.value)
        else -> typeError("string")
    }
}

data object CharDeserializer : Deserializer<Char> {
    override fun deserialize(json: JSONValue?): Char? = when {
        json == null -> null
        json is JSONString && json.value.length == 1 -> json.value[0]
        else -> typeError("string of length 1")
    }
}

data object CharArrayDeserializer : Deserializer<CharArray> {
    override fun deserialize(json: JSONValue?): CharArray? = when (json) {
        null -> null
        is JSONString -> json.value.toCharArray()
        is JSONArray -> CharArray(json.size) { i ->
            val item = json[i]
            if (item is JSONString && item.value.length == 1)
                item.value[0]
            else
                typeError("string of length 1", i)
        }
        else -> typeError("string or array of char")
    }
}

data object ArrayCharDeserializer : Deserializer<Array<Char>> {
    @Suppress("unchecked_cast")
    override fun deserialize(json: JSONValue?): Array<Char>? = when (json) {
        null -> null
        is JSONString -> {
            val string = json.value
            Array(string.length) { index -> string [index] }
        }
        is JSONArray -> {
            val array = newArray(Char::class, json.size)
            for (i in 0 until json.size) {
                val item = json[i]
                if (item is JSONString && item.value.length == 1)
                    array[i] = item.value[0]
                else
                    typeError("string of length 1", i)
            }
            array as Array<Char>
        }
        else -> typeError("string or array of char")
    }
}
