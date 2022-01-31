/*
 * @(#) JSONDeserializerArrayTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022 Peter Wall
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

package io.kjson

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail

import java.lang.reflect.Type
import java.math.BigDecimal
import java.time.LocalDate
import java.util.BitSet
import java.util.LinkedList
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import io.kjson.Constants.arrayListStringType
import io.kjson.Constants.hashSetStringType
import io.kjson.Constants.jsonArrayString
import io.kjson.Constants.linkedHashSetStringType
import io.kjson.Constants.linkedListStringType
import io.kjson.Constants.listStringType
import io.kjson.Constants.listStrings
import io.kjson.Constants.pairStringIntType
import io.kjson.Constants.pairStringStringType
import io.kjson.Constants.setStringType
import io.kjson.Constants.stringTypeProjection
import io.kjson.Constants.tripleStringIntStringType
import io.kjson.Constants.tripleStringStringStringType

import io.kjson.testclasses.DummyList
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass2

class JSONDeserializerArrayTest {

    @Test fun `should return BitSet from JSONArray`() {
        val bitset = BitSet()
        bitset.set(2)
        bitset.set(7)
        val json = JSONArray.build {
            add(2)
            add(7)
        }
        expect(bitset) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BooleanArray from JSONArray of boolean`() {
        val json = JSONArray.build {
            add(true)
            add(false)
            add(false)
        }
        val expected = booleanArrayOf(true, false, false)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(BooleanArray::class, json)))
    }

    @Test fun `should fail deserializing BooleanArray if entries not boolean`() {
        val json = JSONArray.build {
            add(123)
            add("ABC")
            add(false)
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(BooleanArray::class, json) }.let {
            expect("Can't deserialize 123 as Boolean at /0") { it.message }
        }
    }

    @Test fun `should return ByteArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(1)
            add(2)
            add(3)
        }
        val expected = byteArrayOf(1, 2, 3)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(ByteArray::class, json)))
    }

    @Test fun `should return CharArray from JSONArray of character`() {
        val json = JSONArray.build {
            add("a")
            add("b")
            add("c")
        }
        val expected = charArrayOf('a', 'b', 'c')
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(CharArray::class, json)))
    }

    @Test fun `should return DoubleArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(123)
            add(0)
            add(BigDecimal("0.012"))
        }
        val expected = doubleArrayOf(123.0, 0.0, 0.012)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(DoubleArray::class, json)))
    }

    @Test fun `should return FloatArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(123)
            add(0)
            add(BigDecimal("0.012"))
        }
        val expected = floatArrayOf(123.0F, 0.0F, 0.012F)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(FloatArray::class, json)))
    }

    @Test fun `should return IntArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(12345)
            add(2468)
            add(321321)
        }
        val expected = intArrayOf(12345, 2468, 321321)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(IntArray::class, json)))
    }

    @Test fun `should fail deserializing JSONArray of number if entries not number`() {
        val json = JSONArray.build {
            add("12345")
            add(true)
            add(321321)
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(IntArray::class, json) }.let {
            expect("Can't deserialize \"12345\" as Int at /0") { it.message }
        }
    }

    @Test fun `should fail deserializing JSONArray to IntArray if entries not integer`() {
        val json = JSONArray.build {
            add(12345)
            add(BigDecimal("0.125"))
            add(321321)
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(IntArray::class, json) }.let {
            expect("Can't deserialize 0.125 as Int at /1") { it.message }
        }
    }

    @Test fun `should return LongArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(123456789123456)
            add(0)
            add(321321L)
        }
        val expected = longArrayOf(123456789123456, 0, 321321)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(LongArray::class, json)))
    }

    @Test fun `should return ShortArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(1234)
            add(0)
            add(321)
        }
        val expected = shortArrayOf(1234, 0, 321)
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(ShortArray::class, json)))
    }

    @Test fun `should return List of String from JSONArray of JSONString`() {
        expect(listStrings) { JSONDeserializer.deserialize(listStringType, jsonArrayString) }
    }

    @Test fun `should return ArrayList of String from JSONArray of JSONString`() {
        expect(ArrayList(listStrings)) { JSONDeserializer.deserialize(arrayListStringType, jsonArrayString) }
    }

    @Test fun `should return LinkedList of String from JSONArray of JSONString`() {
        expect(LinkedList(listStrings)) { JSONDeserializer.deserialize(linkedListStringType, jsonArrayString) }
    }

    @Test fun `should return Set of String from JSONArray of JSONString`() {
        expect(LinkedHashSet(listStrings)) { JSONDeserializer.deserialize(setStringType, jsonArrayString) }
    }

    @Test fun `should reject duplicate when deserializing Set from JSONArray of JSONString`() {
        val jsonArrayDuplicate = JSONArray.build {
            add("abc")
            add("def")
            add("abc")
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(setStringType, jsonArrayDuplicate) }.let {
            expect("Duplicate not allowed at /2") { it.message }
        }
    }

    @Test fun `should return HashSet of String from JSONArray of JSONString`() {
        expect(HashSet(listStrings)) { JSONDeserializer.deserialize(hashSetStringType, jsonArrayString) }
    }

    @Test fun `should return LinkedHashSet of String from JSONArray of JSONString`() {
        expect(LinkedHashSet(listStrings)) { JSONDeserializer.deserialize(linkedHashSetStringType, jsonArrayString) }
    }

    @Test fun `should return Pair from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add("def")
        }
        expect("abc" to "def") { JSONDeserializer.deserialize(pairStringStringType, json) }
    }

    @Test fun `should return heterogenous Pair from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add(88)
        }
        expect("abc" to 88) { JSONDeserializer.deserialize(pairStringIntType, json) }
    }

    @Test fun `should return Triple from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add("def")
            add("xyz")
        }
        expect(Triple("abc", "def", "xyz")) { JSONDeserializer.deserialize(tripleStringStringStringType, json) }
    }

    @Test fun `should return heterogenous Triple from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add(66)
            add("xyz")
        }
        expect(Triple("abc", 66, "xyz")) { JSONDeserializer.deserialize(tripleStringIntStringType, json) }
    }

    @Test fun `should deserialize List using Java Type correctly`() {
        val json = JSONArray.build {
            add(JSONObject.build {
                add("field1", 567)
                add("field2", "abcdef")
            })
            add(JSONObject.build {
                add("field1", 9999)
                add("field2", "qwerty")
            })
        }
        val type: Type = JavaClass2::class.java.getField("field1").genericType
        expect(listOf(JavaClass1(567, "abcdef"), JavaClass1(9999, "qwerty"))) {
            JSONDeserializer.deserialize(type, json)
        }
    }

    @Test fun `should deserialize JSONArray into List derived type`() {
        val json = JSONArray.build {
            add("2019-10-06")
            add("2019-10-05")
        }
        expect(DummyList(listOf(LocalDate.of(2019, 10, 6), LocalDate.of(2019, 10, 5)))) {
            JSONDeserializer.deserialize(DummyList::class, json)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun `should deserialize JSONArray into Sequence`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        val expected = sequenceOf("abcde", "fghij").toList()
        val stringSequenceType = Sequence::class.createType(listOf(stringTypeProjection))
        expect(expected) { (JSONDeserializer.deserialize(stringSequenceType, json) as Sequence<String>).toList() }
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun `should deserialize JSONArray into Array`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        val expected = arrayOf("abcde", "fghij")
        val stringArrayType = Array<String>::class.createType(listOf(stringTypeProjection))
        assertTrue(expected.contentEquals(JSONDeserializer.deserialize(stringArrayType, json) as Array<String>))
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun `should deserialize JSONArray into nested Array`() {
        val list1 = JSONArray.build {
            add("qwerty")
            add("asdfgh")
            add("zxcvbn")
        }
        val list2 = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        val json = JSONArray.build {
            add(list1)
            add(list2)
        }
        val array1 = arrayOf("qwerty", "asdfgh", "zxcvbn")
        val array2 = arrayOf("abcde", "fghij")
        val expected = arrayOf(array1, array2)
        val stringArrayType = Array<String>::class.createType(listOf(stringTypeProjection))
        val stringArrayArrayType = Array<String>::class.createType(listOf(KTypeProjection.invariant(stringArrayType)))
        val actual = JSONDeserializer.deserialize(stringArrayArrayType, json) as Array<Array<String>>
        assertTrue(expected.contentDeepEquals(actual))
    }

    @Test fun `should deserialize Java Stream`() {
        val json = JSONArray.of(JSONString("abc"), JSONString("def"))
        val result: Stream<String> = JSONDeserializer.deserialize(json) ?: fail("result was null")
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect("abc") { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect("def") { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize Java IntStream`() {
        val json = JSONArray.of(JSONInt(2345), JSONInt(6789))
        val result: IntStream = JSONDeserializer.deserialize(json) ?: fail("result was null")
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect(2345) { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect(6789) { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize Java LongStream`() {
        val json = JSONArray.of(JSONLong(1234567812345678), JSONLong(9876543298765432))
        val result: LongStream = JSONDeserializer.deserialize(json) ?: fail("result was null")
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect(1234567812345678) { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect(9876543298765432) { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize Java DoubleStream`() {
        val json = JSONArray.of(JSONDecimal("1234.5"), JSONDecimal("1e40"))
        val result: DoubleStream = JSONDeserializer.deserialize(json) ?: fail("result was null")
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect(1234.5) { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect(1e40) { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize JSONArray to Any`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        expect(listOf("abcde", "fghij")) { JSONDeserializer.deserializeAny(json) }
    }

}
