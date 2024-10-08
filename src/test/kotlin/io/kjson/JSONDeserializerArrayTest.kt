/*
 * @(#) JSONDeserializerArrayTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023, 2024 Peter Wall
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
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.expect

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
import io.kjson.util.SizedSequence
import io.kjson.testclasses.DummyList
import io.kjson.testclasses.DummyList2
import io.kjson.testclasses.DummyList3
import net.pwall.util.ImmutableCollection
import net.pwall.util.ImmutableList
import net.pwall.util.ImmutableSet

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
            expect("Incorrect type, expected Boolean but was 123, at /0") { it.message }
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
            expect("Incorrect type, expected Int but was \"12345\", at /0") { it.message }
        }
    }

    @Test fun `should fail deserializing JSONArray to IntArray if entries not integer`() {
        val json = JSONArray.build {
            add(12345)
            add(BigDecimal("0.125"))
            add(321321)
        }
        assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(IntArray::class, json) }.let {
            expect("Incorrect type, expected Int but was 0.125, at /1") { it.message }
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
            expect("Duplicate not allowed, at /2") { it.message }
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

    @Test fun `should fail deserializing null into non-nullable Pair item`() {
        val json = JSONArray.build {
            add("abc")
            add(null)
        }
        assertFailsWith<JSONKotlinException> {
            JSONDeserializer.deserialize<Pair<String, Int>>(json)
        }.let {
            expect("Pair item may not be null, at /1") { it.message }
        }
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

    @Test fun `should fail deserializing null into non-nullable Triple item`() {
        val json = JSONArray.build {
            add("abc")
            add(null)
            add(1)
        }
        assertFailsWith<JSONKotlinException> {
            JSONDeserializer.deserialize<Triple<String, Int, Int>>(json)
        }.let {
            expect("Triple item may not be null, at /1") { it.message }
        }
    }

    @Test fun `should fail deserializing null into nested non-nullable Triple item`() {
        val json = JSONArray.build {
            add(JSONArray.build {
                add("abc")
                add(null)
                add(1)
            })
            add("a")
        }
        assertFailsWith<JSONKotlinException> {
            JSONDeserializer.deserialize<Pair<Triple<String, Int, Int>, String>>(json)
        }.let {
            expect("Triple item may not be null, at /0/1") { it.message }
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

    @Test fun `should deserialize JSONArray into Sequence`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        val sequence = JSONDeserializer.deserialize<Sequence<String>>(json)
        assertIs<SizedSequence<*>>(sequence)
        expect(2) { sequence.size }
        with(sequence.iterator()) {
            assertTrue(hasNext())
            expect("abcde") { next() }
            assertTrue(hasNext())
            expect("fghij") { next() }
            assertFalse(hasNext())
        }
    }

    @Test fun `should fail deserializing null into non-nullable Sequence item`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
            add(null)
        }
        val sequence = JSONDeserializer.deserialize<Sequence<String>>(json)
        assertIs<SizedSequence<*>>(sequence)
        expect(3) { sequence.size }
        with(sequence.iterator()) {
            assertTrue(hasNext())
            expect("abcde") { next() }
            assertTrue(hasNext())
            expect("fghij") { next() }
            assertFailsWith<JSONKotlinException> { hasNext() }.let {
                expect("Sequence item may not be null, at /2") { it.message }
            }
        }
    }

    @Test fun `should fail deserializing incorrect type into Sequence item`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
            add(12345)
        }
        val sequence = JSONDeserializer.deserialize<Sequence<String>>(json)
        assertIs<SizedSequence<*>>(sequence)
        expect(3) { sequence.size }
        with(sequence.iterator()) {
            assertTrue(hasNext())
            expect("abcde") { next() }
            assertTrue(hasNext())
            expect("fghij") { next() }
            assertFailsWith<JSONKotlinException> { hasNext() }.let {
                expect("Incorrect type, expected string but was 12345, at /2") { it.message }
            }
        }
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

    @Test fun `should report error correctly deserializing JSONArray into nested Array`() {
        val list1 = JSONArray.build {
            add("qwerty")
            add("asdfgh")
            add("zxcvbn")
        }
        val list2 = JSONArray.build {
            add("abcde")
            add("ghijk")
            add(123)
        }
        val json = JSONArray.build {
            add(list1)
            add(list2)
        }
        val stringArrayType = Array<String>::class.createType(listOf(stringTypeProjection))
        val stringArrayArrayType = Array<String>::class.createType(listOf(KTypeProjection.invariant(stringArrayType)))
        assertFailsWith<JSONKotlinException> {
            JSONDeserializer.deserialize(stringArrayArrayType, json)
        }.let {
            expect("Incorrect type, expected string but was 123, at /1/2") { it.message }
        }
    }

    @Test fun `should deserialize Java Stream`() {
        val json = JSONArray.of(JSONString("abc"), JSONString("def"))
        val result: Stream<String> = JSONDeserializer.deserialize(json)
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect("abc") { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect("def") { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize Java IntStream`() {
        val json = JSONArray.of(JSONInt(2345), JSONInt(6789))
        val result: IntStream = JSONDeserializer.deserialize(json)
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect(2345) { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect(6789) { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize Java LongStream`() {
        val json = JSONArray.of(JSONLong(1234567812345678), JSONLong(9876543298765432))
        val result: LongStream = JSONDeserializer.deserialize(json)
        val iterator = result.iterator()
        expect(true) { iterator.hasNext() }
        expect(1234567812345678) { iterator.next() }
        expect(true) { iterator.hasNext() }
        expect(9876543298765432) { iterator.next() }
        expect(false) { iterator.hasNext() }
    }

    @Test fun `should deserialize Java DoubleStream`() {
        val json = JSONArray.of(JSONDecimal("1234.5"), JSONDecimal("1e40"))
        val result: DoubleStream = JSONDeserializer.deserialize(json)
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

    @Test fun `should deserialize List taking Array constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        val immutableList = DummyList2(arrayOf("aaa", "ccc", "bbb", "abc"))
        expect(immutableList) { json.deserialize<DummyList2>() }
    }

    @Test fun `should deserialize List taking List constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        val immutableList = DummyList3(listOf("aaa", "ccc", "bbb", "abc"))
        expect(immutableList) { json.deserialize<DummyList3>() }
    }

    @Test fun `should deserialize ImmutableList`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        val immutableList = ImmutableList.listOf(arrayOf("aaa", "ccc", "bbb", "abc"))
        expect(immutableList) { json.deserialize<ImmutableList<String>>() }
    }

    @Test fun `should deserialize Set taking Set constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        val immutableSet = ImmutableSet.setOf(arrayOf("aaa", "ccc", "bbb", "abc"))
        expect(immutableSet) { json.deserialize<ImmutableSet<String>>() }
    }

    @Test fun `should deserialize Collection taking Collection constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        // there's no equals defined for Collection, so we have to do this the hard way...
        val immutableCollection: ImmutableCollection<String> = json.deserialize()
        expect(4) { immutableCollection.size }
        assertContains(immutableCollection, "aaa")
        assertContains(immutableCollection, "bbb")
        assertContains(immutableCollection, "ccc")
        assertContains(immutableCollection, "abc")
    }

}
