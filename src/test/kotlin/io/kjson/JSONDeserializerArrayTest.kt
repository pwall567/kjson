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

import kotlin.test.Test

import java.math.BigDecimal
import java.time.LocalDate
import java.util.BitSet
import java.util.LinkedList
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldContain
import io.kstuff.test.shouldThrow

import io.jstuff.util.ImmutableCollection
import io.jstuff.util.ImmutableList
import io.jstuff.util.ImmutableSet

import io.kjson.Constants.jsonArrayString
import io.kjson.Constants.listStrings
import io.kjson.util.SizedSequence
import io.kjson.testclasses.DummyList
import io.kjson.testclasses.DummyList2
import io.kjson.testclasses.DummyList3

class JSONDeserializerArrayTest {

    @Test fun `should return BitSet from JSONArray`() {
        val bitset = BitSet()
        bitset.set(2)
        bitset.set(7)
        val json = JSONArray.build {
            add(2)
            add(7)
        }
        shouldBeEqual(bitset, json.deserialize())
    }

    @Test fun `should return BooleanArray from JSONArray of boolean`() {
        val json = JSONArray.build {
            add(true)
            add(false)
            add(false)
        }
        shouldBeEqual(booleanArrayOf(true, false, false), json.deserialize())
    }

    @Test fun `should fail deserializing BooleanArray if entries not boolean`() {
        val json = JSONArray.build {
            add(123)
            add("ABC")
            add(false)
        }
        shouldThrow<JSONKotlinException>("Incorrect type, expected Boolean but was 123, at /0") {
            json.deserialize<BooleanArray>()
        }
    }

    @Test fun `should return ByteArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(1)
            add(2)
            add(3)
        }
        shouldBeEqual(byteArrayOf(1, 2, 3), json.deserialize())
    }

    @Test fun `should return CharArray from JSONArray of character`() {
        val json = JSONArray.build {
            add("a")
            add("b")
            add("c")
        }
        shouldBeEqual(charArrayOf('a', 'b', 'c'), json.deserialize())
    }

    @Test fun `should return DoubleArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(123)
            add(0)
            add(BigDecimal("0.012"))
        }
        shouldBeEqual(doubleArrayOf(123.0, 0.0, 0.012), json.deserialize())
    }

    @Test fun `should return FloatArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(123)
            add(0)
            add(BigDecimal("0.012"))
        }
        shouldBeEqual(floatArrayOf(123.0F, 0.0F, 0.012F), json.deserialize())
    }

    @Test fun `should return IntArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(12345)
            add(2468)
            add(321321)
        }
        shouldBeEqual(intArrayOf(12345, 2468, 321321), json.deserialize())
    }

    @Test fun `should fail deserializing JSONArray of number if entries not number`() {
        val json = JSONArray.build {
            add("12345")
            add(true)
            add(321321)
        }
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was \"12345\", at /0") {
            json.deserialize<IntArray>()
        }
    }

    @Test fun `should fail deserializing JSONArray to IntArray if entries not integer`() {
        val json = JSONArray.build {
            add(12345)
            add(BigDecimal("0.125"))
            add(321321)
        }
        shouldThrow<JSONKotlinException>("Incorrect type, expected Int but was 0.125, at /1") {
            json.deserialize<IntArray>()
        }
    }

    @Test fun `should return LongArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(123456789123456)
            add(0)
            add(321321L)
        }
        shouldBeEqual(longArrayOf(123456789123456, 0, 321321), json.deserialize())
    }

    @Test fun `should return ShortArray from JSONArray of number`() {
        val json = JSONArray.build {
            add(1234)
            add(0)
            add(321)
        }
        shouldBeEqual(shortArrayOf(1234, 0, 321), json.deserialize())
    }

    @Test fun `should return List of String from JSONArray of JSONString`() {
        shouldBeEqual(listStrings, jsonArrayString.deserialize())
    }

    @Test fun `should return ArrayList of String from JSONArray of JSONString`() {
        shouldBeEqual(ArrayList(listStrings), jsonArrayString.deserialize())
    }

    @Test fun `should return LinkedList of String from JSONArray of JSONString`() {
        shouldBeEqual(LinkedList(listStrings), jsonArrayString.deserialize())
    }

    @Test fun `should return Set of String from JSONArray of JSONString`() {
        jsonArrayString.deserialize<Set<String>>() shouldBe LinkedHashSet(listStrings)
    }

    @Test fun `should reject duplicate when deserializing Set from JSONArray of JSONString`() {
        val jsonArrayDuplicate = JSONArray.build {
            add("abc")
            add("def")
            add("abc")
        }
        shouldThrow<JSONKotlinException>("Duplicate not allowed, at /2") {
            jsonArrayDuplicate.deserialize<Set<String>>()
        }
    }

    @Test fun `should return HashSet of String from JSONArray of JSONString`() {
        shouldBeEqual(HashSet(listStrings), jsonArrayString.deserialize())
    }

    @Test fun `should return LinkedHashSet of String from JSONArray of JSONString`() {
        shouldBeEqual(LinkedHashSet(listStrings), jsonArrayString.deserialize())
    }

    @Test fun `should return Pair from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add("def")
        }
        shouldBeEqual("abc" to "def", json.deserialize())
    }

    @Test fun `should return heterogenous Pair from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add(88)
        }
        shouldBeEqual("abc" to 88, json.deserialize())
    }

    @Test fun `should fail deserializing null into non-nullable Pair item`() {
        val json = JSONArray.build {
            add("abc")
            add(null)
        }
        shouldThrow<JSONKotlinException>("Pair item may not be null, at /1") {
            json.deserialize<Pair<String, Int>>()
        }
    }

    @Test fun `should return Triple from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add("def")
            add("xyz")
        }
        shouldBeEqual(Triple("abc", "def", "xyz"), json.deserialize())
    }

    @Test fun `should return heterogenous Triple from JSONArray`() {
        val json = JSONArray.build {
            add("abc")
            add(66)
            add("xyz")
        }
        shouldBeEqual(Triple("abc", 66, "xyz"), json.deserialize())
    }

    @Test fun `should fail deserializing null into non-nullable Triple item`() {
        val json = JSONArray.build {
            add("abc")
            add(null)
            add(1)
        }
        shouldThrow<JSONKotlinException>("Triple item may not be null, at /1") {
            json.deserialize<Triple<String, Int, Int>>()
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
        shouldThrow<JSONKotlinException>("Triple item may not be null, at /0/1") {
            json.deserialize<Pair<Triple<String, Int, Int>, String>>()
        }
    }

    @Test fun `should deserialize JSONArray into List derived type`() {
        val json = JSONArray.build {
            add("2019-10-06")
            add("2019-10-05")
        }
        shouldBeEqual(DummyList(listOf(LocalDate.of(2019, 10, 6), LocalDate.of(2019, 10, 5))), json.deserialize())
    }

    @Test fun `should deserialize JSONArray into Sequence`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        val sequence = json.deserialize<Sequence<String>>()
        sequence.shouldBeType<SizedSequence<*>>()
        sequence.size shouldBe 2
        with(sequence.iterator()) {
            hasNext() shouldBe true
            next() shouldBe "abcde"
            hasNext() shouldBe true
            next() shouldBe "fghij"
            hasNext() shouldBe false
        }
    }

    @Test fun `should fail deserializing null into non-nullable Sequence item`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
            add(null)
        }
        val sequence = json.deserialize<Sequence<String>>()
        sequence.shouldBeType<SizedSequence<*>>()
        sequence.size shouldBe 3
        with(sequence.iterator()) {
            hasNext() shouldBe true
            next() shouldBe "abcde"
            hasNext() shouldBe true
            next() shouldBe "fghij"
            shouldThrow<JSONKotlinException>("Sequence item may not be null, at /2") {
                hasNext()
            }
        }
    }

    @Test fun `should fail deserializing incorrect type into Sequence item`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
            add(12345)
        }
        val sequence = json.deserialize<Sequence<String>>()
        sequence.shouldBeType<SizedSequence<*>>()
        sequence.size shouldBe 3
        with(sequence.iterator()) {
            hasNext() shouldBe true
            next() shouldBe "abcde"
            hasNext() shouldBe true
            next() shouldBe "fghij"
            shouldThrow<JSONKotlinException>("Incorrect type, expected string but was 12345, at /2") {
                hasNext()
            }
        }
    }

    @Test fun `should deserialize JSONArray into Array`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        shouldBeEqual(arrayOf("abcde", "fghij"), json.deserialize())
    }

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
        val actual = json.deserialize<Array<Array<String>>>()
        expected.contentDeepEquals(actual) shouldBe true
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
        shouldThrow<JSONKotlinException>("Incorrect type, expected string but was 123, at /1/2") {
            json.deserialize<Array<Array<String>>>()
        }
    }

    @Test fun `should deserialize Java Stream`() {
        val json = JSONArray.of(JSONString("abc"), JSONString("def"))
        val result: Stream<String> = json.deserialize()
        val iterator = result.iterator()
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe "abc"
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe "def"
        iterator.hasNext() shouldBe false
    }

    @Test fun `should deserialize Java IntStream`() {
        val json = JSONArray.of(JSONInt(2345), JSONInt(6789))
        val result: IntStream = json.deserialize()
        val iterator = result.iterator()
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe 2345
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe 6789
        iterator.hasNext() shouldBe false
    }

    @Test fun `should deserialize Java LongStream`() {
        val json = JSONArray.of(JSONLong(1234567812345678), JSONLong(9876543298765432))
        val result: LongStream = json.deserialize()
        val iterator = result.iterator()
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe 1234567812345678
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe 9876543298765432
        iterator.hasNext() shouldBe false
    }

    @Test fun `should deserialize Java DoubleStream`() {
        val json = JSONArray.of(JSONDecimal("1234.5"), JSONDecimal("1e40"))
        val result: DoubleStream = json.deserialize()
        val iterator = result.iterator()
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe 1234.5
        iterator.hasNext() shouldBe true
        iterator.next() shouldBe 1e40
        iterator.hasNext() shouldBe false
    }

    @Test fun `should deserialize JSONArray to Any`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        json.deserializeAny() shouldBe listOf("abcde", "fghij")
    }

    @Test fun `should deserialize List taking Array constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        json.deserialize<DummyList2>() shouldBe DummyList2(arrayOf("aaa", "ccc", "bbb", "abc"))
    }

    @Test fun `should deserialize List taking List constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        json.deserialize<DummyList3>() shouldBe DummyList3(listOf("aaa", "ccc", "bbb", "abc"))
    }

    @Test fun `should deserialize ImmutableList`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        val immutableList = ImmutableList.listOf(arrayOf("aaa", "ccc", "bbb", "abc"))
        json.deserialize<ImmutableList<String>>() shouldBe immutableList
    }

    @Test fun `should deserialize Set taking Set constructor parameter`() {
        val json = JSONArray.build {
            add("aaa")
            add("ccc")
            add("bbb")
            add("abc")
        }
        val immutableSet = ImmutableSet.setOf(arrayOf("aaa", "ccc", "bbb", "abc"))
        json.deserialize<ImmutableSet<String>>() shouldBe immutableSet
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
        immutableCollection.size shouldBe 4
        immutableCollection shouldContain "aaa"
        immutableCollection shouldContain "bbb"
        immutableCollection shouldContain "ccc"
        immutableCollection shouldContain "abc"
    }

}
