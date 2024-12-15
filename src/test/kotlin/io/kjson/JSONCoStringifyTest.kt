/*
 * @(#) JSONCoStringifyTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2022, 2023, 2024 Peter Wall
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
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.BitSet
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeOneOf
import io.kstuff.test.shouldEndWith
import io.kstuff.test.shouldStartWith
import io.kstuff.test.shouldThrow

import io.kjson.JSONCoStringify.outputJSON
import io.kjson.optional.Opt
import io.kjson.test.JSONExpect.Companion.expectJSON
import io.kjson.testclasses.Circular1
import io.kjson.testclasses.Circular2
import io.kjson.testclasses.Const
import io.kjson.testclasses.Const2
import io.kjson.testclasses.Const3
import io.kjson.testclasses.CustomIgnore
import io.kjson.testclasses.CustomIncludeAllProperties
import io.kjson.testclasses.CustomIncludeIfNull
import io.kjson.testclasses.CustomName
import io.kjson.testclasses.Derived
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy2
import io.kjson.testclasses.Dummy3
import io.kjson.testclasses.DummyEnum
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.DummyWithCustomIgnore
import io.kjson.testclasses.DummyWithCustomIncludeAllProperties
import io.kjson.testclasses.DummyWithCustomIncludeIfNull
import io.kjson.testclasses.DummyWithCustomNameAnnotation
import io.kjson.testclasses.DummyWithIgnore
import io.kjson.testclasses.DummyWithIncludeAllProperties
import io.kjson.testclasses.DummyWithIncludeIfNull
import io.kjson.testclasses.DummyWithNameAnnotation
import io.kjson.testclasses.DummyWithParamNameAnnotation
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass3
import io.kjson.testclasses.ListEnum
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.OptData
import io.kjson.testclasses.Organization
import io.kjson.testclasses.TestGenericClass
import io.kjson.testclasses.TestGenericClass2
import io.kjson.testclasses.ValueClass
import io.kjson.testclasses.ValueClassHolder
import io.kjson.util.CoCapture
import io.kjson.util.OutputCapture
import net.pwall.util.output

class JSONCoStringifyTest {

    @Test fun `should stringify null`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(null as String?) { capture.accept(it) }
        capture.toString() shouldBe "null"
    }

    @Test fun `should use toJSON if specified in JSONConfig`() = runBlocking {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    add("a", it.field1)
                    add("b", it.field2)
                }
            }
        }
        val capture = OutputCapture()
        JSONCoStringify.coStringify(Dummy1("xyz", 888), config) { capture.accept(it) }
        capture.toString() shouldBe """{"a":"xyz","b":888}"""
    }

    @Test fun `should stringify a JSONValue`() = runBlocking {
        val json = JSONObject.build {
            add("a", "Hello")
            add("b", 27)
        }
        val capture = OutputCapture()
        JSONCoStringify.coStringify(json) { capture.accept(it) }
        capture.toString() shouldBe """{"a":"Hello","b":27}"""
    }

    @Test fun `should stringify a simple string`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify("abc") { capture.accept(it) }
        capture.toString() shouldBe "\"abc\""
    }

    @Test fun `should stringify a string with a newline`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify("a\nc") { capture.accept(it) }
        capture.toString() shouldBe "\"a\\nc\""
    }

    @Test fun `should stringify a string with a unicode sequence`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify("a\u2014c") { capture.accept(it) }
        capture.toString() shouldBe "\"a\\u2014c\""
    }

    @Test fun `should stringify a single character`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify('X') { capture.accept(it) }
        capture.toString() shouldBe "\"X\""
    }

    @Test fun `should stringify a charArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(charArrayOf('a', 'b', 'c')) { capture.accept(it) }
        capture.toString() shouldBe "\"abc\""
    }

    @Test fun `should stringify an integer`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(123456789) { capture.accept(it) }
        capture.toString() shouldBe "123456789"
    }

    @Test fun `should stringify an integer 0`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(0) { capture.accept(it) }
        capture.toString() shouldBe "0"
    }

    @Test fun `should stringify a negative integer`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(-888) { capture.accept(it) }
        capture.toString() shouldBe "-888"
    }

    @Test fun `should stringify a long`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(123456789012345678) { capture.accept(it) }
        capture.toString() shouldBe "123456789012345678"
    }

    @Test fun `should stringify a negative long`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(-111222333444555666) { capture.accept(it) }
        capture.toString() shouldBe "-111222333444555666"
    }

    @Test fun `should stringify a short`() = runBlocking {
        val capture = OutputCapture()
        val x: Short = 12345
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "12345"
    }

    @Test fun `should stringify a negative short`() = runBlocking {
        val capture = OutputCapture()
        val x: Short = -4444
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "-4444"
    }

    @Test fun `should stringify a byte`() = runBlocking {
        val capture = OutputCapture()
        val x: Byte = 123
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "123"
    }

    @Test fun `should stringify a negative byte`() = runBlocking {
        val capture = OutputCapture()
        val x: Byte = -99
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "-99"
    }

    @Test fun `should stringify a float`() = runBlocking {
        val capture = OutputCapture()
        val x = 1.2345F
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "1.2345"
    }

    @Test fun `should stringify a negative float`() = runBlocking {
        val capture = OutputCapture()
        val x: Float = -567.888F
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "-567.888"
    }

    @Test fun `should stringify a double`() = runBlocking {
        val capture = OutputCapture()
        val x = 1.23456789
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "1.23456789"
    }

    @Test fun `should stringify a negative double`() = runBlocking {
        val capture = OutputCapture()
        val x = -9.998877665
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "-9.998877665"
    }

    @Test fun `should stringify an unsigned integer`() = runBlocking {
        val capture = OutputCapture()
        val x = 2147483648U // Int.MAX_VALUE + 1
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "2147483648"
    }

    @Test fun `should stringify an unsigned long`() = runBlocking {
        val capture = OutputCapture()
        val x = 9223372036854775808U // Long.MAX_VALUE + 1
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "9223372036854775808"
    }

    @Test fun `should stringify an unsigned short`() = runBlocking {
        val capture = OutputCapture()
        val x: UShort = 32768U // Short.MAX_VALUE + 1
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "32768"
    }

    @Test fun `should stringify an unsigned byte`() = runBlocking {
        val capture = OutputCapture()
        val x: UByte = 128U // Byte.MAX_VALUE + 1
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "128"
    }

    @Test fun `should stringify a BigInteger`() = runBlocking {
        val capture = OutputCapture()
        val x = BigInteger.valueOf(123456789000)
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "123456789000"
    }

    @Test fun `should stringify a negative BigInteger`() = runBlocking {
        val capture = OutputCapture()
        val x = BigInteger.valueOf(-123456789000)
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "-123456789000"
    }

    @Test fun `should stringify a BigInteger as string when specified in JSONConfig`() = runBlocking {
        val capture = OutputCapture()
        val config = JSONConfig {
            bigIntegerString = true
        }
        val x = BigInteger.valueOf(123456789000)
        JSONCoStringify.coStringify(x, config) { capture.accept(it) }
        capture.toString() shouldBe "\"123456789000\""
    }

    @Test fun `should stringify a BigDecimal`() = runBlocking {
        val capture = OutputCapture()
        val x = BigDecimal("12345.678")
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "12345.678"
    }

    @Test fun `should stringify a BigDecimal as string when specified in JSONConfig`() = runBlocking {
        val capture = OutputCapture()
        val config = JSONConfig {
            bigDecimalString = true
        }
        val x = BigDecimal("12345.678")
        JSONCoStringify.coStringify(x, config) { capture.accept(it) }
        capture.toString() shouldBe "\"12345.678\""
    }

    @Test fun `should stringify a Boolean`() = runBlocking {
        val capture = OutputCapture()
        var x = true
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "true"
        capture.reset()
        x = false
        JSONCoStringify.coStringify(x) { capture.accept(it) }
        capture.toString() shouldBe "false"
    }

    @Test fun `should stringify an array of characters`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(arrayOf('a', 'b', 'c')) { capture.accept(it) }
        capture.toString() shouldBe "\"abc\""
    }

    @Test fun `should stringify an array of integers`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(arrayOf(123, 456, 789)) { capture.accept(it) }
        capture.toString() shouldBe "[123,456,789]"
    }

    @Test fun `should stringify an IntArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(IntArray(3) { (it + 1) * 111 }) { capture.accept(it) }
        capture.toString() shouldBe "[111,222,333]"
    }

    @Test fun `should stringify a LongArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(LongArray(3) { (it + 1) * 111111111111 }) { capture.accept(it) }
        capture.toString() shouldBe "[111111111111,222222222222,333333333333]"
    }

    @Test fun `should stringify a ByteArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(ByteArray(3) { ((it + 1) * 5).toByte() }) { capture.accept(it) }
        capture.toString() shouldBe "[5,10,15]"
    }

    @Test fun `should stringify a ShortArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(ShortArray(4) { ((it + 1) * 1111).toShort() }) { capture.accept(it) }
        capture.toString() shouldBe "[1111,2222,3333,4444]"
    }

    @Test fun `should stringify a FloatArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(FloatArray(4) { it + 0.5F }) { capture.accept(it) }
        capture.toString() shouldBe "[0.5,1.5,2.5,3.5]"
    }

    @Test fun `should stringify a DoubleArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(DoubleArray(4) { it + 0.5 }) { capture.accept(it) }
        capture.toString() shouldBe "[0.5,1.5,2.5,3.5]"
    }

    @Test fun `should stringify a BooleanArray`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(BooleanArray(4) { (it and 1) == 0 }) { capture.accept(it) }
        capture.toString() shouldBe "[true,false,true,false]"
    }

    @Test fun `should stringify an array of strings`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(arrayOf("Hello", "World")) { capture.accept(it) }
        capture.toString() shouldBe """["Hello","World"]"""
    }

    @Test fun `should stringify an array of arrays`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(arrayOf(arrayOf(11, 22), arrayOf(33, 44))) { capture.accept(it) }
        capture.toString() shouldBe "[[11,22],[33,44]]"
    }

    @Test fun `should stringify an empty array`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(emptyArray<Int>()) { capture.accept(it) }
        capture.toString() shouldBe "[]"
    }

    @Test fun `should stringify an array of nulls`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(arrayOfNulls<String>(3)) { capture.accept(it) }
        capture.toString() shouldBe "[null,null,null]"
    }

    @Test fun `should stringify a Pair of integers`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(123 to 789) { capture.accept(it) }
        capture.toString() shouldBe "[123,789]"
    }

    @Test fun `should stringify a Pair of strings`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify("back" to "front") { capture.accept(it) }
        capture.toString() shouldBe """["back","front"]"""
    }

    @Test fun `should stringify a Triple of integers`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(Triple(123, 789, 0)) { capture.accept(it) }
        capture.toString() shouldBe "[123,789,0]"
    }

    @Test fun `should stringify a class with a custom toJSON`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(DummyFromJSON(49)) { capture.accept(it) }
        capture.toString() shouldBe """{"dec":"49","hex":"31"}"""
    }

    @Test fun `should stringify a list of integers`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(listOf(1234, 4567, 7890)) { capture.accept(it) }
        capture.toString() shouldBe "[1234,4567,7890]"
    }

    @Test fun `should stringify a list of strings`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(listOf("alpha", "beta", "gamma")) { capture.accept(it) }
        capture.toString() shouldBe """["alpha","beta","gamma"]"""
    }

    @Test fun `should stringify a list of strings including null`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(listOf("alpha", "beta", null, "gamma")) { capture.accept(it) }
        capture.toString() shouldBe """["alpha","beta",null,"gamma"]"""
    }

    @Test fun `should stringify a set of strings`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(listOf("alpha", "beta", "gamma")) { capture.accept(it) }
        // unfortunately, we don't know in what order a set iterator will return the entries, so...
        val str = capture.toString()
        str shouldStartWith "["
        str shouldEndWith "]"
        val items = str.drop(1).dropLast(1).split(',').sorted()
        items.size shouldBe 3
        items[0] shouldBe "\"alpha\""
        items[1] shouldBe "\"beta\""
        items[2] shouldBe "\"gamma\""
    }

    @Test fun `should stringify the results of an iterator`() = runBlocking {
        val capture = OutputCapture()
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        JSONCoStringify.coStringify(list.iterator()) { capture.accept(it) }
        capture.toString() shouldBe "[1,1,2,3,5,8,13,21]"
    }

    @Test fun `should stringify a sequence`() = runBlocking {
        val capture = OutputCapture()
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        JSONCoStringify.coStringify(list.asSequence()) { capture.accept(it) }
        capture.toString() shouldBe "[1,1,2,3,5,8,13,21]"
    }

    @Test fun `should stringify the results of an enumeration`() = runBlocking {
        val capture = OutputCapture()
        val list = listOf("tahi", "rua", "toru", "wh\u0101")
        JSONCoStringify.coStringify(ListEnum(list)) { capture.accept(it) }
        capture.toString() shouldBe """["tahi","rua","toru","wh\u0101"]"""
    }

    @Test fun `should stringify a Java Stream of strings`() = runBlocking {
        val capture = OutputCapture()
        val stream = Stream.of("tahi", "rua", "toru", "wh\u0101")
        JSONCoStringify.coStringify(stream) { capture.accept(it) }
        capture.toString() shouldBe """["tahi","rua","toru","wh\u0101"]"""
    }

    @Test fun `should stringify a Java IntStream`() = runBlocking {
        val capture = OutputCapture()
        val stream = IntStream.of(1, 1, 2, 3, 5, 8, 13, 21)
        JSONCoStringify.coStringify(stream) { capture.accept(it) }
        capture.toString() shouldBe "[1,1,2,3,5,8,13,21]"
    }

    @Test fun `should stringify a Java LongStream`() = runBlocking {
        val capture = OutputCapture()
        val stream = LongStream.of(10_000_000_000, 10_000_000_000, 20_000_000_000, 30_000_000_000, 50_000_000_000)
        JSONCoStringify.coStringify(stream) { capture.accept(it) }
        capture.toString() shouldBe "[10000000000,10000000000,20000000000,30000000000,50000000000]"
    }

    @Test fun `should stringify a map of string to string`() = runBlocking {
        val capture = OutputCapture()
        val map = mapOf("tahi" to "one", "rua" to "two", "toru" to "three")
        JSONCoStringify.coStringify(map) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("tahi", "one")
            property("rua", "two")
            property("toru", "three")
        }
    }

    @Test fun `should stringify a map of string to integer`() = runBlocking {
        val capture = OutputCapture()
        val map = mapOf("un" to 1, "deux" to 2, "trois" to 3, "quatre" to 4)
        JSONCoStringify.coStringify(map) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(4)
            property("un", 1)
            property("deux", 2)
            property("trois", 3)
            property("quatre", 4)
        }
    }

    @Test fun `should stringify an enum`() = runBlocking {
        val capture = OutputCapture()
        val value = DummyEnum.ALPHA
        JSONCoStringify.coStringify(value) { capture.accept(it) }
        capture.toString() shouldBe "\"ALPHA\""
    }

    @Test fun `should stringify an SQL date`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-10"
        val date = java.sql.Date.valueOf(str)
        JSONCoStringify.coStringify(date) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify an SQL time`() = runBlocking {
        val capture = OutputCapture()
        val str = "00:21:22"
        val time = java.sql.Time.valueOf(str)
        JSONCoStringify.coStringify(time) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify an SQL date-time`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-10 00:21:22.0"
        val timeStamp = java.sql.Timestamp.valueOf(str)
        JSONCoStringify.coStringify(timeStamp) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify an Instant`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-09T14:28:51.234Z"
        val instant = Instant.parse(str)
        JSONCoStringify.coStringify(instant) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a LocalDate`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-10"
        val localDate = LocalDate.parse(str)
        JSONCoStringify.coStringify(localDate) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a LocalDateTime`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-10T00:09:26.123"
        val localDateTime = LocalDateTime.parse(str)
        JSONCoStringify.coStringify(localDateTime) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a LocalTime`() = runBlocking {
        val capture = OutputCapture()
        val str = "00:09:26.123"
        val localTime = LocalTime.parse(str)
        JSONCoStringify.coStringify(localTime) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify an OffsetTime`() = runBlocking {
        val capture = OutputCapture()
        val str = "10:15:06.543+10:00"
        val offsetTime = OffsetTime.parse(str)
        JSONCoStringify.coStringify(offsetTime) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify an OffsetDateTime`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-10T10:15:06.543+10:00"
        val offsetDateTime = OffsetDateTime.parse(str)
        JSONCoStringify.coStringify(offsetDateTime) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a ZonedDateTime`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04-10T10:15:06.543+10:00[Australia/Sydney]"
        val zonedDateTime = ZonedDateTime.parse(str)
        JSONCoStringify.coStringify(zonedDateTime) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Year`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020"
        val year = Year.parse(str)
        JSONCoStringify.coStringify(year) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a YearMonth`() = runBlocking {
        val capture = OutputCapture()
        val str = "2020-04"
        val yearMonth = YearMonth.parse(str)
        JSONCoStringify.coStringify(yearMonth) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a MonthDay`() = runBlocking {
        val capture = OutputCapture()
        val str = "--04-23"
        val monthDay = MonthDay.parse(str)
        JSONCoStringify.coStringify(monthDay) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Java Duration`() = runBlocking {
        val capture = OutputCapture()
        val str = "PT2H"
        val duration = JavaDuration.parse(str)
        JSONCoStringify.coStringify(duration) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Period`() = runBlocking {
        val capture = OutputCapture()
        val str = "P3M"
        val period = Period.parse(str)
        JSONCoStringify.coStringify(period) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Duration`() = runBlocking {
        val capture = OutputCapture()
        val str = "PT2H"
        val duration = Duration.parseIsoString(str)
        JSONCoStringify.coStringify(duration) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a URI`() = runBlocking {
        val capture = OutputCapture()
        val str = "http://kjson.io"
        val uri = URI(str)
        JSONCoStringify.coStringify(uri) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a URL`() = runBlocking {
        val capture = OutputCapture()
        val str = "http://kjson.io"
        val url = URL(str)
        JSONCoStringify.coStringify(url) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a UUID`() = runBlocking {
        val capture = OutputCapture()
        val str = "e24b6740-7ac3-11ea-9e47-37640adfe63a"
        val uuid = UUID.fromString(str)
        JSONCoStringify.coStringify(uuid) { capture.accept(it) }
        capture.toString() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Calendar`() = runBlocking {
        val capture = OutputCapture()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")).apply {
            set(Calendar.YEAR, 2019)
            set(Calendar.MONTH, 3)
            set(Calendar.DAY_OF_MONTH, 25)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 52)
            set(Calendar.SECOND, 47)
            set(Calendar.MILLISECOND, 123)
            set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        }
        JSONCoStringify.coStringify(cal) { capture.accept(it) }
        capture.toString() shouldBe "\"2019-04-25T18:52:47.123+10:00\""
    }

    @Test fun `should stringify a Date`() = runBlocking {
        val capture = OutputCapture()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2019)
            set(Calendar.MONTH, 3)
            set(Calendar.DAY_OF_MONTH, 25)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 52)
            set(Calendar.SECOND, 47)
            set(Calendar.MILLISECOND, 123)
            set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        }
        val date = cal.time
        JSONCoStringify.coStringify(date) { capture.accept(it) }
        // NOTE - Java implementations are inconsistent - some will normalise the time to UTC
        // while others preserve the time zone as supplied.  The test below allows for either.
        capture.toString() shouldBeOneOf listOf("\"2019-04-25T18:52:47.123+10:00\"", "\"2019-04-25T08:52:47.123Z\"")
    }

    @Test fun `should stringify a BitSet`() = runBlocking {
        val capture = OutputCapture()
        val bitSet = BitSet().apply {
            set(1)
            set(3)
        }
        JSONCoStringify.coStringify(bitSet) { capture.accept(it) }
        capture.toString() shouldBe "[1,3]"
    }

    @Test fun `should stringify a simple object`() = runBlocking {
        val capture = OutputCapture()
        val dummy1 = Dummy1("abcdef", 98765)
        JSONCoStringify.coStringify(dummy1) { capture.accept(it) }
        capture.toString() shouldBe """{"field1":"abcdef","field2":98765}"""
    }

    @Test fun `should stringify a simple object with extra property`() = runBlocking {
        val capture = OutputCapture()
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = "extra123"
        JSONCoStringify.coStringify(dummy2) { capture.accept(it) }
        capture.toString() shouldBe """{"field1":"abcdef","field2":98765,"extra":"extra123"}"""
    }

    @Test fun `should stringify a simple object with extra property omitting null field`() = runBlocking {
        val capture = OutputCapture()
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        JSONCoStringify.coStringify(dummy2) { capture.accept(it) }
        capture.toString() shouldBe """{"field1":"abcdef","field2":98765}"""
    }

    @Test fun `should stringify a simple object with extra property including null when config set`() = runBlocking {
        val capture = OutputCapture()
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        val config = JSONConfig {
            includeNulls = true
        }
        JSONCoStringify.coStringify(dummy2, config) { capture.accept(it) }
        capture.toString() shouldBe """{"field1":"abcdef","field2":98765,"extra":null}"""
    }

    @Test fun `should stringify an object of a derived class`() = runBlocking {
        val capture = OutputCapture()
        val obj = Derived()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        obj.field3 = 0.012
        JSONCoStringify.coStringify(obj) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("field1", "qwerty")
            property("field2", 98765)
            property("field3", BigDecimal("0.012"))
        }
    }

    @Test fun `should stringify an object using name annotation`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithNameAnnotation()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        JSONCoStringify.coStringify(obj) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("field1", "qwerty")
            property("fieldX", 98765)
        }
    }

    @Test fun `should stringify an object using parameter name annotation`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithParamNameAnnotation("abc", 123)
        JSONCoStringify.coStringify(obj) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify an object using custom parameter name annotation`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithCustomNameAnnotation("abc", 123)
        val config = JSONConfig {
            addNameAnnotation(CustomName::class, "symbol")
        }
        JSONCoStringify.coStringify(obj, config) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify a nested object`() = runBlocking {
        val capture = OutputCapture()
        val obj1 = Dummy1("asdfg", 987)
        val obj3 = Dummy3(obj1, "what?")
        JSONCoStringify.coStringify(obj3) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("text", "what?")
            property("dummy1") {
                count(2)
                property("field1", "asdfg")
                property("field2", 987)
            }
        }
    }

    @Test fun `should stringify an object with @JSONIgnore`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithIgnore("alpha", "beta", "gamma")
        JSONCoStringify.coStringify(obj) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom ignore annotation`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithCustomIgnore("alpha", "beta", "gamma")
        val config = JSONConfig {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        JSONCoStringify.coStringify(obj, config) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeIfNull`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithIncludeIfNull("alpha", null, "gamma")
        JSONCoStringify.coStringify(obj) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom include if null annotation`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithCustomIncludeIfNull("alpha", null, "gamma")
        val config = JSONConfig {
            addIncludeIfNullAnnotation(CustomIncludeIfNull::class)
        }
        JSONCoStringify.coStringify(obj, config) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeAllProperties`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithIncludeAllProperties("alpha", null, "gamma")
        JSONCoStringify.coStringify(obj) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom include all properties annotation`() = runBlocking {
        val capture = OutputCapture()
        val obj = DummyWithCustomIncludeAllProperties("alpha", null, "gamma")
        val config = JSONConfig {
            addIncludeAllPropertiesAnnotation(CustomIncludeAllProperties::class)
        }
        JSONCoStringify.coStringify(obj, config) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify sealed class with extra member to indicate derived class`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(Const(2.0)) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("class", "Const")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class object correctly`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(NotANumber) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(1)
            property("class", "NotANumber")
        }
    }

    @Test fun `should stringify sealed class with custom discriminator`() = runBlocking {
        val capture = OutputCapture()
        val config = JSONConfig {
            sealedClassDiscriminator = "?"
        }
        JSONCoStringify.coStringify(Const(2.0), config) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("?", "Const")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(Const2(2.0)) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("type", "Const2")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator and identifiers`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(Const3(2.0)) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("type", "CONST")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with discriminator and identifiers within class`() = runBlocking {
        val capture = OutputCapture()
        JSONCoStringify.coStringify(Organization("ORGANIZATION", 123456, "Funny Company")) { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(3)
            property("type", "ORGANIZATION")
            property("id", 123456)
            property("name", "Funny Company")
        }
    }

    @Test fun `should output to existing CoOutput`() = runBlocking {
        val coCapture = CoCapture()
        coCapture.output('[')
        coCapture.outputJSON(Dummy1("Testing..."))
        coCapture.output(']')
        expectJSON(coCapture.toString()) {
            count(1)
            item(0) {
                count(2)
                property("field1", "Testing...")
                property("field2", 999)
            }
        }
    }

    @Test fun `should stringify a sequence of objects coming from a Channel`() = runBlocking {
        val coCapture = CoCapture()
        val channel = Channel<Dummy1>()
        val job = launch {
            channel.send(Dummy1("first", 123))
            channel.send(Dummy1("second", 456))
            channel.send(Dummy1("third", 789))
            channel.close()
        }
        coCapture.outputJSON(channel)
        expectJSON(coCapture.toString()) {
            valueIsArray(3) {
                item(0) {
                    property("field1", "first")
                    property("field2", 123)
                }
                item(1) {
                    property("field1", "second")
                    property("field2", 456)
                }
                item(2) {
                    property("field1", "third")
                    property("field2", 789)
                }
            }
        }
        job.join()
    }

    @Test fun `should stringify a sequence of objects coming from a Flow`() = runBlocking {
        val coCapture = CoCapture()
        val flow = flow {
            emit(Dummy1("first", 123))
            emit(Dummy1("second", 456))
            emit(Dummy1("third", 789))
        }
        coCapture.outputJSON(flow)
        expectJSON(coCapture.toString()) {
            count(3)
            item(0) {
                property("field1", "first")
                property("field2", 123)
            }
            item(1) {
                property("field1", "second")
                property("field2", 456)
            }
            item(2) {
                property("field1", "third")
                property("field2", 789)
            }
        }
    }

    @Test fun `should stringify using extension function`() = runBlocking {
        val capture = OutputCapture()
        Dummy1("Extension", 9999).coStringifyJSON { capture.accept(it) }
        expectJSON(capture.toString()) {
            count(2)
            property("field1", "Extension")
            property("field2", 9999)
        }
    }

    @Test fun `should fail on use of circular reference`() = runBlocking {
        val capture = OutputCapture()
        val circular1 = Circular1()
        val circular2 = Circular2()
        circular1.ref2 = circular2
        circular2.ref1 = circular1
        shouldThrow<JSONKotlinException> { JSONCoStringify.coStringify(circular1) { capture.accept(it) } }.let {
            it.message shouldBe "Circular reference to Circular1, at /ref2/ref1"
        }
    }

    @Test fun `should fail on use of circular reference in List`() = runBlocking {
        val capture = OutputCapture()
        val circularList = mutableListOf<Any>()
        circularList.add(circularList)
        shouldThrow<JSONKotlinException> { JSONCoStringify.coStringify(circularList) { capture.accept(it) } }.let {
            it.message shouldBe "Circular reference to ArrayList, at /0"
        }
    }

    @Test fun `should fail on use of circular reference in Map`() = runBlocking {
        val capture = OutputCapture()
        val circularMap = mutableMapOf<String, Any>()
        circularMap["test1"] = circularMap
        shouldThrow<JSONKotlinException> { JSONCoStringify.coStringify(circularMap) { capture.accept(it) } }.let {
            it.message shouldBe "Circular reference to LinkedHashMap, at /test1"
        }
    }

    @Test fun `should stringify value class`() = runBlocking {
        val coCapture = CoCapture()
        val holder = ValueClassHolder(
            innerValue = ValueClass("xyz"),
            number = 999,
        )
        coCapture.outputJSON(holder)
        coCapture.toString() shouldBe """{"innerValue":{"string":"xyz"},"number":999}"""
    }

    @Test fun `should stringify Opt`() = runBlocking {
        val coCapture = CoCapture()
        coCapture.outputJSON(Opt.of(123))
        coCapture.toString() shouldBe "123"
    }

    @Test fun `should stringify Opt missing`() = runBlocking {
        val coCapture = CoCapture()
        coCapture.outputJSON(Opt.unset<Any>())
        coCapture.toString() shouldBe """null"""
    }

    @Test fun `should stringify Opt property`() = runBlocking {
        val coCapture = CoCapture()
        coCapture.outputJSON(OptData(Opt.of(123)))
        coCapture.toString() shouldBe """{"aaa":123}"""
    }

    @Test fun `should stringify Opt property missing`() = runBlocking {
        val coCapture = CoCapture()
        coCapture.outputJSON(OptData(Opt.unset()))
        coCapture.toString() shouldBe """{}"""
    }

    @Test fun `should stringify a Java class`() = runBlocking {
        val coCapture = CoCapture()
        val javaClass1 = JavaClass1(98765, "abcdef")
        coCapture.outputJSON(javaClass1)
        // Java properties appear to be not necessarily in declaration order
        expectJSON(coCapture.toString()) {
            exhaustive {
                property("field1", 98765)
                property("field2", "abcdef")
            }
        }
    }

    @Test fun `should stringify a derived Java class`() = runBlocking {
        val coCapture = CoCapture()
        val javaClass3 = JavaClass3(98765, "abcdef", true)
        coCapture.outputJSON(javaClass3)
        expectJSON(coCapture.toString()) {
            exhaustive {
                property("field1", 98765)
                property("field2", "abcdef")
                property("flag", true)
            }
        }
    }

    @Test fun `should stringify generic class`() = runBlocking {
        val coCapture = CoCapture()
        val data = Dummy1("alpha", 1234)
        val generic = TestGenericClass(
            name = "testAlpha",
            data = data,
        )
        coCapture.outputJSON(generic)
        expectJSON(generic.stringifyJSON()) {
            exhaustive {
                property("name", "testAlpha")
                property("data") {
                    exhaustive {
                        property("field1", "alpha")
                        property("field2", 1234)
                    }
                }
            }
        }
    }

    @Test fun `should stringify generic class with member variables`() = runBlocking {
        val coCapture = CoCapture()
        val data = Dummy1("alpha", 1234)
        val generic = TestGenericClass2<Dummy1>().apply {
            name = "testAlpha"
            this.data = data
        }
        coCapture.outputJSON(generic)
        expectJSON(coCapture.toString()) {
            exhaustive {
                property("name", "testAlpha")
                property("data") {
                    exhaustive {
                        property("field1", "alpha")
                        property("field2", 1234)
                    }
                }
            }
        }
    }

}
