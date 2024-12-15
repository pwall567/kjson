/*
 * @(#) JSONStringifyTest.kt
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
import kotlin.time.Duration

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
import io.kstuff.test.shouldThrow

import io.kjson.JSONStringify.appendJSON
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
import io.kjson.testclasses.GenericCreator
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
import io.kstuff.test.shouldEndWith
import io.kstuff.test.shouldStartWith

class JSONStringifyTest {

    @Test fun `should stringify null`() {
        (null as String?).stringifyJSON() shouldBe "null"
    }

    @Test fun `should use toJSON if specified in JSONConfig`() {
        val config = JSONConfig {
            toJSON<Dummy1> {
                JSONObject.build {
                    add("a", it.field1)
                    add("b", it.field2)
                }
            }
        }
        Dummy1("xyz", 888).stringifyJSON(config) shouldBe """{"a":"xyz","b":888}"""
    }

    @Test fun `should stringify a JSONValue`() {
        val json = JSONObject.build {
            add("a", "Hello")
            add("b", 27)
        }
        json.stringifyJSON() shouldBe """{"a":"Hello","b":27}"""
    }

    @Test fun `should stringify a simple string`() {
        "abc".stringifyJSON() shouldBe "\"abc\""
    }

    @Test fun `should stringify a string with a newline`() {
        "a\nc".stringifyJSON() shouldBe "\"a\\nc\""
    }

    @Test fun `should stringify a string with a unicode sequence`() {
        "a\u2014c".stringifyJSON() shouldBe "\"a\\u2014c\""
    }

    @Test fun `should stringify a single character`() {
        'X'.stringifyJSON() shouldBe "\"X\""
    }

    @Test fun `should stringify a charArray`() {
        charArrayOf('a', 'b', 'c').stringifyJSON() shouldBe "\"abc\""
    }

    @Test fun `should stringify an integer`() {
        123456789.stringifyJSON() shouldBe "123456789"
    }

    @Test fun `should stringify an integer 0`() {
        0.stringifyJSON() shouldBe "0"
    }

    @Test fun `should stringify a negative integer`() {
        (-888).stringifyJSON() shouldBe "-888"
    }

    @Test fun `should stringify a long`() {
        123456789012345678.stringifyJSON() shouldBe "123456789012345678"
    }

    @Test fun `should stringify a negative long`() {
        (-111222333444555666).stringifyJSON() shouldBe "-111222333444555666"
    }

    @Test fun `should stringify a short`() {
        val x: Short = 12345
        x.stringifyJSON() shouldBe "12345"
    }

    @Test fun `should stringify a negative short`() {
        val x: Short = -4444
        x.stringifyJSON() shouldBe "-4444"
    }

    @Test fun `should stringify a byte`() {
        val x: Byte = 123
        x.stringifyJSON() shouldBe "123"
    }

    @Test fun `should stringify a negative byte`() {
        val x: Byte = -99
        x.stringifyJSON() shouldBe "-99"
    }

    @Test fun `should stringify a float`() {
        val x = 1.2345F
        x.stringifyJSON() shouldBe "1.2345"
    }

    @Test fun `should stringify a negative float`() {
        val x: Float = -567.888F
        x.stringifyJSON() shouldBe "-567.888"
    }

    @Test fun `should stringify a double`() {
        val x = 1.23456789
        x.stringifyJSON() shouldBe "1.23456789"
    }

    @Test fun `should stringify a negative double`() {
        val x = -9.998877665
        x.stringifyJSON() shouldBe "-9.998877665"
    }

    @Test fun `should stringify an unsigned integer`() {
        val x = 2147483648U // Int.MAX_VALUE + 1
        x.stringifyJSON() shouldBe "2147483648"
    }

    @Test fun `should stringify an unsigned long`() {
        val x = 9223372036854775808U // Long.MAX_VALUE + 1
        x.stringifyJSON() shouldBe "9223372036854775808"
    }

    @Test fun `should stringify an unsigned short`() {
        val x: UShort = 32768U // Short.MAX_VALUE + 1
        x.stringifyJSON() shouldBe "32768"
    }

    @Test fun `should stringify an unsigned byte`() {
        val x: UByte = 128U // Byte.MAX_VALUE + 1
        x.stringifyJSON() shouldBe "128"
    }

    @Test fun `should stringify a BigInteger`() {
        val x = BigInteger.valueOf(123456789000)
        x.stringifyJSON() shouldBe "123456789000"
    }

    @Test fun `should stringify a negative BigInteger`() {
        val x = BigInteger.valueOf(-123456789000)
        x.stringifyJSON() shouldBe "-123456789000"
    }

    @Test fun `should stringify a BigInteger as string when specified in JSONConfig`() {
        val config = JSONConfig {
            bigIntegerString = true
        }
        val x = BigInteger.valueOf(123456789000)
        x.stringifyJSON(config) shouldBe "\"123456789000\""
    }

    @Test fun `should stringify a BigDecimal`() {
        val x = BigDecimal("12345.678")
        x.stringifyJSON() shouldBe "12345.678"
    }

    @Test fun `should stringify a BigDecimal as string when specified in JSONConfig`() {
        val config = JSONConfig {
            bigDecimalString = true
        }
        val x = BigDecimal("12345.678")
        x.stringifyJSON(config) shouldBe "\"12345.678\""
    }

    @Test fun `should stringify a Boolean`() {
        var x = true
        x.stringifyJSON() shouldBe "true"
        x = false
        x.stringifyJSON() shouldBe "false"
    }

    @Test fun `should stringify an array of characters`() {
        arrayOf('a', 'b', 'c').stringifyJSON() shouldBe "\"abc\""
    }

    @Test fun `should stringify an array of integers`() {
        arrayOf(123, 456, 789).stringifyJSON() shouldBe "[123,456,789]"
    }

    @Test fun `should stringify an IntArray`() {
        (IntArray(3) { (it + 1) * 111 }).stringifyJSON() shouldBe "[111,222,333]"
    }

    @Test fun `should stringify a LongArray`() {
        (LongArray(3) { (it + 1) * 111111111111 }).stringifyJSON() shouldBe "[111111111111,222222222222,333333333333]"
    }

    @Test fun `should stringify a ByteArray`() {
        (ByteArray(3) { ((it + 1) * 5).toByte() }).stringifyJSON() shouldBe "[5,10,15]"
    }

    @Test fun `should stringify a ShortArray`() {
        (ShortArray(4) { ((it + 1) * 1111).toShort() }).stringifyJSON() shouldBe "[1111,2222,3333,4444]"
    }

    @Test fun `should stringify a FloatArray`() {
        (FloatArray(4) { it + 0.5F }).stringifyJSON() shouldBe "[0.5,1.5,2.5,3.5]"
    }

    @Test fun `should stringify a DoubleArray`() {
        (DoubleArray(4) { it + 0.5 }).stringifyJSON() shouldBe "[0.5,1.5,2.5,3.5]"
    }

    @Test fun `should stringify a BooleanArray`() {
        (BooleanArray(4) { (it and 1) == 0 }).stringifyJSON() shouldBe "[true,false,true,false]"
    }

    @Test fun `should stringify an array of strings`() {
        arrayOf("Hello", "World").stringifyJSON() shouldBe """["Hello","World"]"""
    }

    @Test fun `should stringify an array of arrays`() {
        arrayOf(arrayOf(11, 22), arrayOf(33, 44)).stringifyJSON() shouldBe "[[11,22],[33,44]]"
    }

    @Test fun `should stringify an empty array`() {
        emptyArray<Int>().stringifyJSON() shouldBe "[]"
    }

    @Test fun `should stringify an array of nulls`() {
        arrayOfNulls<String>(3).stringifyJSON() shouldBe "[null,null,null]"
    }

    @Test fun `should stringify a Pair of integers`() {
        (123 to 789).stringifyJSON() shouldBe "[123,789]"
    }

    @Test fun `should stringify a Pair of strings`() {
        ("back" to "front").stringifyJSON() shouldBe """["back","front"]"""
    }

    @Test fun `should stringify a Triple of integers`() {
        Triple(123, 789, 0).stringifyJSON() shouldBe "[123,789,0]"
    }

    @Test fun `should stringify a class with a custom toJson`() {
        DummyFromJSON(49).stringifyJSON() shouldBe """{"dec":"49","hex":"31"}"""
    }

    @Test fun `should stringify a list of integers`() {
        listOf(1234, 4567, 7890).stringifyJSON() shouldBe "[1234,4567,7890]"
    }

    @Test fun `should stringify a list of strings`() {
        listOf("alpha", "beta", "gamma").stringifyJSON() shouldBe """["alpha","beta","gamma"]"""
    }

    @Test fun `should stringify a list of strings including null`() {
        listOf("alpha", "beta", null, "gamma").stringifyJSON() shouldBe """["alpha","beta",null,"gamma"]"""
    }

    @Test fun `should stringify a set of strings`() {
        // unfortunately, we don't know in what order a set iterator will return the entries, so...
        val str = setOf("alpha", "beta", "gamma").stringifyJSON()
        str shouldStartWith "["
        str shouldEndWith "]"
        val items = str.drop(1).dropLast(1).split(',').sorted()
        items.size shouldBe 3
        items[0] shouldBe "\"alpha\""
        items[1] shouldBe "\"beta\""
        items[2] shouldBe "\"gamma\""
    }

    @Test fun `should stringify the results of an iterator`() {
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        list.iterator().stringifyJSON() shouldBe "[1,1,2,3,5,8,13,21]"
    }

    @Test fun `should stringify a sequence`() {
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        list.asSequence().stringifyJSON() shouldBe "[1,1,2,3,5,8,13,21]"
    }

    @Test fun `should stringify the results of an enumeration`() {
        val list = listOf("tahi", "rua", "toru", "wh\u0101")
        ListEnum(list).stringifyJSON() shouldBe """["tahi","rua","toru","wh\u0101"]"""
    }

    @Test fun `should stringify a Java Stream of strings`() {
        val stream = Stream.of("tahi", "rua", "toru", "wh\u0101")
        stream.stringifyJSON() shouldBe """["tahi","rua","toru","wh\u0101"]"""
    }

    @Test fun `should stringify a Java IntStream`() {
        val stream = IntStream.of(1, 1, 2, 3, 5, 8, 13, 21)
        stream.stringifyJSON() shouldBe "[1,1,2,3,5,8,13,21]"
    }

    @Test fun `should stringify a Java LongStream`() {
        val stream = LongStream.of(10_000_000_000, 10_000_000_000, 20_000_000_000, 30_000_000_000, 50_000_000_000)
        stream.stringifyJSON() shouldBe "[10000000000,10000000000,20000000000,30000000000,50000000000]"
    }

    @Test fun `should stringify a map of string to string`() {
        val map = mapOf("tahi" to "one", "rua" to "two", "toru" to "three")
        val str = map.stringifyJSON()
        expectJSON(str) {
            count(3)
            property("tahi", "one")
            property("rua", "two")
            property("toru", "three")
        }
    }

    @Test fun `should stringify a map of string to integer`() {
        val map = mapOf("un" to 1, "deux" to 2, "trois" to 3, "quatre" to 4)
        val str = map.stringifyJSON()
        expectJSON(str) {
            count(4)
            property("un", 1)
            property("deux", 2)
            property("trois", 3)
            property("quatre", 4)
        }
    }

    @Test fun `should stringify an enum`() {
        val value = DummyEnum.ALPHA
        value.stringifyJSON() shouldBe "\"ALPHA\""
    }

    @Test fun `should stringify an SQL date`() {
        val str = "2020-04-10"
        val date = java.sql.Date.valueOf(str)
        date.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify an SQL time`() {
        val str = "00:21:22"
        val time = java.sql.Time.valueOf(str)
        time.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify an SQL date-time`() {
        val str = "2020-04-10 00:21:22.0"
        val timeStamp = java.sql.Timestamp.valueOf(str)
        timeStamp.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify an Instant`() {
        val str = "2020-04-09T14:28:51.234Z"
        val instant = Instant.parse(str)
        instant.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a LocalDate`() {
        val str = "2020-04-10"
        val localDate = LocalDate.parse(str)
        localDate.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a LocalDateTime`() {
        val str = "2020-04-10T00:09:26.123"
        val localDateTime = LocalDateTime.parse(str)
        localDateTime.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a LocalTime`() {
        val str = "00:09:26.123"
        val localTime = LocalTime.parse(str)
        localTime.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify an OffsetTime`() {
        val str = "10:15:06.543+10:00"
        val offsetTime = OffsetTime.parse(str)
        offsetTime.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify an OffsetDateTime`() {
        val str = "2020-04-10T10:15:06.543+10:00"
        val offsetDateTime = OffsetDateTime.parse(str)
        offsetDateTime.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a ZonedDateTime`() {
        val str = "2020-04-10T10:15:06.543+10:00[Australia/Sydney]"
        val zonedDateTime = ZonedDateTime.parse(str)
        zonedDateTime.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Year`() {
        val str = "2020"
        val year = Year.parse(str)
        year.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a YearMonth`() {
        val str = "2020-04"
        val yearMonth = YearMonth.parse(str)
        yearMonth.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a MonthDay`() {
        val str = "--04-23"
        val monthDay = MonthDay.parse(str)
        monthDay.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Java Duration`() {
        val str = "PT2H"
        val duration = JavaDuration.parse(str)
        duration.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Period`() {
        val str = "P3M"
        val period = Period.parse(str)
        period.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Duration`() {
        val str = "PT2H"
        val duration = Duration.parseIsoString(str)
        duration.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a URI`() {
        val str = "http://pwall.net"
        val uri = URI(str)
        uri.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a URL`() {
        val str = "http://pwall.net"
        val url = URL(str)
        url.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a UUID`() {
        val str = "e24b6740-7ac3-11ea-9e47-37640adfe63a"
        val uuid = UUID.fromString(str)
        uuid.stringifyJSON() shouldBe "\"$str\""
    }

    @Test fun `should stringify a Calendar`() {
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
        cal.stringifyJSON() shouldBe "\"2019-04-25T18:52:47.123+10:00\""
    }

    @Test fun `should stringify a Date`() {
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
        // NOTE - Java implementations are inconsistent - some will normalise the time to UTC
        // while others preserve the time zone as supplied.  The test below allows for either.
        val expected1 = "\"2019-04-25T18:52:47.123+10:00\""
        val expected2 = "\"2019-04-25T08:52:47.123Z\""
        date.stringifyJSON() shouldBeOneOf listOf(expected1, expected2)
    }

    @Test fun `should stringify a BitSet`() {
        val bitSet = BitSet().apply {
            set(1)
            set(3)
        }
        bitSet.stringifyJSON() shouldBe "[1,3]"
    }

    @Test fun `should stringify a simple object`() {
        val dummy1 = Dummy1("abcdef", 98765)
        dummy1.stringifyJSON() shouldBe """{"field1":"abcdef","field2":98765}"""
    }

    @Test fun `should stringify a simple object with extra property`() {
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = "extra123"
        dummy2.stringifyJSON() shouldBe """{"field1":"abcdef","field2":98765,"extra":"extra123"}"""
    }

    @Test fun `should stringify a simple object with extra property omitting null field`() {
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        dummy2.stringifyJSON() shouldBe """{"field1":"abcdef","field2":98765}"""
    }

    @Test fun `should stringify a simple object with extra property including null field when config set`() {
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        val config = JSONConfig {
            includeNulls = true
        }
        dummy2.stringifyJSON(config) shouldBe """{"field1":"abcdef","field2":98765,"extra":null}"""
    }

    @Test fun `should stringify an object of a derived class`() {
        val obj = Derived()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        obj.field3 = 0.012
        val json = obj.stringifyJSON()
        expectJSON(json) {
            count(3)
            property("field1", "qwerty")
            property("field2", 98765)
            property("field3", BigDecimal("0.012"))
        }
    }

    @Test fun `should stringify an object using name annotation`() {
        val obj = DummyWithNameAnnotation()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        val json = obj.stringifyJSON()
        expectJSON(json) {
            count(2)
            property("field1", "qwerty")
            property("fieldX", 98765)
        }
    }

    @Test fun `should stringify an object using parameter name annotation`() {
        val obj = DummyWithParamNameAnnotation("abc", 123)
        val json = obj.stringifyJSON()
        expectJSON(json) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify an object using custom parameter name annotation`() {
        val obj = DummyWithCustomNameAnnotation("abc", 123)
        val config = JSONConfig {
            addNameAnnotation(CustomName::class, "symbol")
        }
        val json = obj.stringifyJSON(config)
        expectJSON(json) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify a nested object`() {
        val obj1 = Dummy1("asdfg", 987)
        val obj3 = Dummy3(obj1, "what?")
        val json = obj3.stringifyJSON()
        expectJSON(json) {
            count(2)
            property("text", "what?")
            property("dummy1") {
                count(2)
                property("field1", "asdfg")
                property("field2", 987)
            }
        }
    }

    @Test fun `should stringify an object with @JSONIgnore`() {
        val obj = DummyWithIgnore("alpha", "beta", "gamma")
        val json = obj.stringifyJSON()
        expectJSON(json) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom ignore annotation`() {
        val obj = DummyWithCustomIgnore("alpha", "beta", "gamma")
        val config = JSONConfig().apply {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        val json = obj.stringifyJSON(config)
        expectJSON(json) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeIfNull`() {
        val obj = DummyWithIncludeIfNull("alpha", null, "gamma")
        val json = obj.stringifyJSON()
        expectJSON(json) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom include if null annotation`() {
        val obj = DummyWithCustomIncludeIfNull("alpha", null, "gamma")
        val config = JSONConfig {
            addIncludeIfNullAnnotation(CustomIncludeIfNull::class)
        }
        val json = obj.stringifyJSON(config)
        expectJSON(json) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeAllProperties`() {
        val obj = DummyWithIncludeAllProperties("alpha", null, "gamma")
        val json = obj.stringifyJSON()
        expectJSON(json) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom include all properties annotation`() {
        val obj = DummyWithCustomIncludeAllProperties("alpha", null, "gamma")
        val config = JSONConfig {
            addIncludeAllPropertiesAnnotation(CustomIncludeAllProperties::class)
        }
        val json = obj.stringifyJSON(config)
        expectJSON(json) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify sealed class with extra member to indicate derived class`() {
        val json = Const(2.0).stringifyJSON()
        expectJSON(json) {
            count(2)
            property("class", "Const")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class object correctly`() {
        val json = NotANumber.stringifyJSON()
        expectJSON(json) {
            count(1)
            property("class", "NotANumber")
        }
    }

    @Test fun `should stringify sealed class with custom discriminator`() {
        val config = JSONConfig {
            sealedClassDiscriminator = "?"
        }
        val json = Const(2.0).stringifyJSON(config)
        expectJSON(json) {
            count(2)
            property("?", "Const")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator`() {
        val json = Const2(2.0).stringifyJSON()
        expectJSON(json) {
            count(2)
            property("type", "Const2")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator and identifiers`() {
        val json = Const3(2.0).stringifyJSON()
        expectJSON(json) {
            count(2)
            property("type", "CONST")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator and identifiers within class`() {
        val json = Organization("ORGANIZATION", 123456, "Funny Company").stringifyJSON()
        expectJSON(json) {
            count(3)
            property("type", "ORGANIZATION")
            property("id", 123456)
            property("name", "Funny Company")
        }
    }

    @Test fun `should fail on use of circular reference`() {
        val circular1 = Circular1()
        val circular2 = Circular2()
        circular1.ref2 = circular2
        circular2.ref1 = circular1
        shouldThrow<JSONKotlinException>("Circular reference to Circular1, at /ref2/ref1") {
            circular1.stringifyJSON()
        }
    }

    @Test fun `should fail on use of circular reference in List`() {
        val circularList = mutableListOf<Any>()
        circularList.add(circularList)
        shouldThrow<JSONKotlinException>("Circular reference to ArrayList, at /0") {
            circularList.stringifyJSON()
        }
    }

    @Test fun `should fail on use of circular reference in Map`() {
        val circularMap = mutableMapOf<String, Any>()
        circularMap["test1"] = circularMap
        shouldThrow<JSONKotlinException>("Circular reference to LinkedHashMap, at /test1") {
            circularMap.stringifyJSON()
        }
    }

    @Test fun `should append to existing Appendable`() {
        val sb = StringBuilder()
        sb.appendJSON(Dummy1("Testing..."))
        val json = sb.toString()
        expectJSON(json) {
            count(2)
            property("field1", "Testing...")
            property("field2", 999)
        }
    }

    @Test fun `should stringify value class`() {
        val holder = ValueClassHolder(
            innerValue = ValueClass("xyz"),
            number = 999,
        )
        holder.stringifyJSON() shouldBe """{"innerValue":{"string":"xyz"},"number":999}"""
    }

    @Test fun `should stringify Opt`() {
        val opt = Opt.of(123)
        opt.stringifyJSON() shouldBe "123"
    }

    @Test fun `should stringify Opt missing`() {
        val opt = Opt.unset<Any>()
        opt.stringifyJSON() shouldBe """null"""
    }

    @Test fun `should stringify Opt property`() {
        val optData = OptData(Opt.of(123))
        optData.stringifyJSON() shouldBe """{"aaa":123}"""
    }

    @Test fun `should stringify Opt property missing`() {
        val optData = OptData(Opt.unset())
        optData.stringifyJSON() shouldBe """{}"""
    }

    @Test fun `should stringify a Java class`() {
        val javaClass1 = JavaClass1(98765, "abcdef")
        // Java properties appear to be not necessarily in declaration order
        expectJSON(javaClass1.stringifyJSON()) {
            exhaustive {
                property("field1", 98765)
                property("field2", "abcdef")
            }
        }
    }

    @Test fun `should stringify a derived Java class`() {
        val javaClass3 = JavaClass3(98765, "abcdef", true)
        expectJSON(javaClass3.stringifyJSON()) {
            exhaustive {
                property("field1", 98765)
                property("field2", "abcdef")
                property("flag", true)
            }
        }
    }

    @Test fun `should stringify generic class`() {
        val data = Dummy1("alpha", 1234)
        val generic = TestGenericClass(
            name = "testAlpha",
            data = data,
        )
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

    @Test fun `should stringify generic class with member variables`() {
        val data = Dummy1("alpha", 1234)
        val generic = TestGenericClass2<Dummy1>().apply {
            name = "testAlpha"
            this.data = data
        }
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

    /**
     * This test covers a case where a generic class is parameterized by a type which is itself a parameter to another
     * generic class.  The [JSONDeserializer.applyTypeParameters] function can find the first level parameter, but it
     * does not have access to the information required to find the next level (or any deeper levels if required).
     */
    @Test fun `should stringify generic class created in another generic class`() {
        val creator = GenericCreator<Dummy1>()
        val data = Dummy1("alpha", 1234)
        expectJSON(creator.createAndStringify(data)) {
            exhaustive {
                property("name", "XXX")
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
