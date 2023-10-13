/*
 * @(#) JSONStringifyTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2023 Peter Wall
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
import kotlin.test.assertFailsWith
import kotlin.test.expect
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
import java.util.stream.Stream

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
import io.kjson.testclasses.ListEnum
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.OptData
import io.kjson.testclasses.Organization
import io.kjson.testclasses.ValueClass
import io.kjson.testclasses.ValueClassHolder

class JSONStringifyTest {

    @Test fun `should stringify null`() {
        expect("null") { JSONStringify.stringify(null) }
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
        expect("""{"a":"xyz","b":888}""") { JSONStringify.stringify(Dummy1("xyz", 888), config) }
    }

    @Test fun `should stringify a JSONValue`() {
        val json = JSONObject.build {
            add("a", "Hello")
            add("b", 27)
        }
        expect("""{"a":"Hello","b":27}""") { JSONStringify.stringify(json) }
    }

    @Test fun `should stringify a simple string`() {
        expect("\"abc\"") { JSONStringify.stringify("abc") }
    }

    @Test fun `should stringify a string with a newline`() {
        expect("\"a\\nc\"") { JSONStringify.stringify("a\nc") }
    }

    @Test fun `should stringify a string with a unicode sequence`() {
        expect("\"a\\u2014c\"") { JSONStringify.stringify("a\u2014c") }
    }

    @Test fun `should stringify a single character`() {
        expect("\"X\"") { JSONStringify.stringify('X') }
    }

    @Test fun `should stringify a charArray`() {
        expect("\"abc\"") { JSONStringify.stringify(charArrayOf('a', 'b', 'c')) }
    }

    @Test fun `should stringify an integer`() {
        expect("123456789") { JSONStringify.stringify(123456789) }
    }

    @Test fun `should stringify an integer 0`() {
        expect("0") { JSONStringify.stringify(0) }
    }

    @Test fun `should stringify a negative integer`() {
        expect("-888") { JSONStringify.stringify(-888) }
    }

    @Test fun `should stringify a long`() {
        expect("123456789012345678") { JSONStringify.stringify(123456789012345678) }
    }

    @Test fun `should stringify a negative long`() {
        expect("-111222333444555666") { JSONStringify.stringify(-111222333444555666) }
    }

    @Test fun `should stringify a short`() {
        val x: Short = 12345
        expect("12345") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a negative short`() {
        val x: Short = -4444
        expect("-4444") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a byte`() {
        val x: Byte = 123
        expect("123") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a negative byte`() {
        val x: Byte = -99
        expect("-99") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a float`() {
        val x = 1.2345F
        expect("1.2345") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a negative float`() {
        val x: Float = -567.888F
        expect("-567.888") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a double`() {
        val x = 1.23456789
        expect("1.23456789") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a negative double`() {
        val x = -9.998877665
        expect("-9.998877665") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify an unsigned integer`() {
        val x = 2147483648U // Int.MAX_VALUE + 1
        expect("2147483648") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify an unsigned long`() {
        val x = 9223372036854775808U // Long.MAX_VALUE + 1
        expect("9223372036854775808") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify an unsigned short`() {
        val x: UShort = 32768U // Short.MAX_VALUE + 1
        expect("32768") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify an unsigned byte`() {
        val x: UByte = 128U // Byte.MAX_VALUE + 1
        expect("128") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a BigInteger`() {
        val x = BigInteger.valueOf(123456789000)
        expect("123456789000") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a negative BigInteger`() {
        val x = BigInteger.valueOf(-123456789000)
        expect("-123456789000") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a BigInteger as string when specified in JSONConfig`() {
        val config = JSONConfig {
            bigIntegerString = true
        }
        val x = BigInteger.valueOf(123456789000)
        expect("\"123456789000\"") { JSONStringify.stringify(x, config) }
    }

    @Test fun `should stringify a BigDecimal`() {
        val x = BigDecimal("12345.678")
        expect("12345.678") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify a BigDecimal as string when specified in JSONConfig`() {
        val config = JSONConfig {
            bigDecimalString = true
        }
        val x = BigDecimal("12345.678")
        expect("\"12345.678\"") { JSONStringify.stringify(x, config) }
    }

    @Test fun `should stringify a Boolean`() {
        var x = true
        expect("true") { JSONStringify.stringify(x) }
        x = false
        expect("false") { JSONStringify.stringify(x) }
    }

    @Test fun `should stringify an array of characters`() {
        expect("\"abc\"") { JSONStringify.stringify(arrayOf('a', 'b', 'c')) }
    }

    @Test fun `should stringify an array of integers`() {
        expect("[123,456,789]") { JSONStringify.stringify(arrayOf(123, 456, 789)) }
    }

    @Test fun `should stringify an IntArray`() {
        expect("[111,222,333]") { JSONStringify.stringify(IntArray(3) { (it + 1) * 111 }) }
    }

    @Test fun `should stringify a LongArray`() {
        expect("[111111111111,222222222222,333333333333]") {
            JSONStringify.stringify(LongArray(3) { (it + 1) * 111111111111 })
        }
    }

    @Test fun `should stringify a ByteArray`() {
        expect("[5,10,15]") { JSONStringify.stringify(ByteArray(3) { ((it + 1) * 5).toByte() }) }
    }

    @Test fun `should stringify a ShortArray`() {
        expect("[1111,2222,3333,4444]") { JSONStringify.stringify(ShortArray(4) { ((it + 1) * 1111).toShort() }) }
    }

    @Test fun `should stringify a FloatArray`() {
        expect("[0.5,1.5,2.5,3.5]") { JSONStringify.stringify(FloatArray(4) { it + 0.5F }) }
    }

    @Test fun `should stringify a DoubleArray`() {
        expect("[0.5,1.5,2.5,3.5]") { JSONStringify.stringify(DoubleArray(4) { it + 0.5 }) }
    }

    @Test fun `should stringify a BooleanArray`() {
        expect("[true,false,true,false]") { JSONStringify.stringify(BooleanArray(4) { (it and 1) == 0 }) }
    }

    @Test fun `should stringify an array of strings`() {
        expect("""["Hello","World"]""") { JSONStringify.stringify(arrayOf("Hello", "World")) }
    }

    @Test fun `should stringify an array of arrays`() {
        expect("[[11,22],[33,44]]") { JSONStringify.stringify(arrayOf(arrayOf(11, 22), arrayOf(33, 44))) }
    }

    @Test fun `should stringify an empty array`() {
        expect("[]") { JSONStringify.stringify(emptyArray<Int>()) }
    }

    @Test fun `should stringify an array of nulls`() {
        expect("[null,null,null]") { JSONStringify.stringify(arrayOfNulls<String>(3)) }
    }

    @Test fun `should stringify a Pair of integers`() {
        expect("[123,789]") { JSONStringify.stringify(123 to 789) }
    }

    @Test fun `should stringify a Pair of strings`() {
        expect("""["back","front"]""") { JSONStringify.stringify("back" to "front") }
    }

    @Test fun `should stringify a Triple of integers`() {
        expect("[123,789,0]") { JSONStringify.stringify(Triple(123, 789, 0)) }
    }

    @Test fun `should stringify a class with a custom toJson`() {
        expect("""{"dec":"49","hex":"31"}""") { JSONStringify.stringify(DummyFromJSON(49)) }
    }

    @Test fun `should stringify a list of integers`() {
        expect("[1234,4567,7890]") { JSONStringify.stringify(listOf(1234, 4567, 7890)) }
    }

    @Test fun `should stringify a list of strings`() {
        expect("""["alpha","beta","gamma"]""") { JSONStringify.stringify(listOf("alpha", "beta", "gamma")) }
    }

    @Test fun `should stringify a list of strings including null`() {
        expect("""["alpha","beta",null,"gamma"]""") { JSONStringify.stringify(listOf("alpha", "beta", null, "gamma")) }
    }

    @Test fun `should stringify a set of strings`() {
        // unfortunately, we don't know in what order a set iterator will return the entries, so...
        val str = JSONStringify.stringify(setOf("alpha", "beta", "gamma"))
        expect(true) { str.startsWith('[') && str.endsWith(']')}
        val items = str.drop(1).dropLast(1).split(',').sorted()
        expect(3) { items.size }
        expect("\"alpha\"") { items[0] }
        expect("\"beta\"") { items[1] }
        expect("\"gamma\"") { items[2] }
    }

    @Test fun `should stringify the results of an iterator`() {
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        expect("[1,1,2,3,5,8,13,21]") { JSONStringify.stringify(list.iterator()) }
    }

    @Test fun `should stringify a sequence`() {
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        expect("[1,1,2,3,5,8,13,21]") { JSONStringify.stringify(list.asSequence()) }
    }

    @Test fun `should stringify the results of an enumeration`() {
        val list = listOf("tahi", "rua", "toru", "wh\u0101")
        expect("""["tahi","rua","toru","wh\u0101"]""") { JSONStringify.stringify(ListEnum(list)) }
    }

    @Test fun `should stringify a Java Stream of strings`() {
        val stream = Stream.of("tahi", "rua", "toru", "wh\u0101")
        expect("""["tahi","rua","toru","wh\u0101"]""") { JSONStringify.stringify(stream) }
    }

    @Test fun `should stringify a Java IntStream`() {
        val stream = IntStream.of(1, 1, 2, 3, 5, 8, 13, 21)
        expect("[1,1,2,3,5,8,13,21]") { JSONStringify.stringify(stream) }
    }

    @Test fun `should stringify a map of string to string`() {
        val map = mapOf("tahi" to "one", "rua" to "two", "toru" to "three")
        val str = JSONStringify.stringify(map)
        expectJSON(str) {
            count(3)
            property("tahi", "one")
            property("rua", "two")
            property("toru", "three")
        }
    }

    @Test fun `should stringify a map of string to integer`() {
        val map = mapOf("un" to 1, "deux" to 2, "trois" to 3, "quatre" to 4)
        val str = JSONStringify.stringify(map)
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
        expect("\"ALPHA\"") { JSONStringify.stringify(value) }
    }

    @Test fun `should stringify an SQL date`() {
        val str = "2020-04-10"
        val date = java.sql.Date.valueOf(str)
        expect("\"$str\"") { JSONStringify.stringify(date) }
    }

    @Test fun `should stringify an SQL time`() {
        val str = "00:21:22"
        val time = java.sql.Time.valueOf(str)
        expect("\"$str\"") { JSONStringify.stringify(time) }
    }

    @Test fun `should stringify an SQL date-time`() {
        val str = "2020-04-10 00:21:22.0"
        val timeStamp = java.sql.Timestamp.valueOf(str)
        expect("\"$str\"") { JSONStringify.stringify(timeStamp) }
    }

    @Test fun `should stringify an Instant`() {
        val str = "2020-04-09T14:28:51.234Z"
        val instant = Instant.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(instant) }
    }

    @Test fun `should stringify a LocalDate`() {
        val str = "2020-04-10"
        val localDate = LocalDate.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(localDate) }
    }

    @Test fun `should stringify a LocalDateTime`() {
        val str = "2020-04-10T00:09:26.123"
        val localDateTime = LocalDateTime.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(localDateTime) }
    }

    @Test fun `should stringify a LocalTime`() {
        val str = "00:09:26.123"
        val localTime = LocalTime.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(localTime) }
    }

    @Test fun `should stringify an OffsetTime`() {
        val str = "10:15:06.543+10:00"
        val offsetTime = OffsetTime.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(offsetTime) }
    }

    @Test fun `should stringify an OffsetDateTime`() {
        val str = "2020-04-10T10:15:06.543+10:00"
        val offsetDateTime = OffsetDateTime.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(offsetDateTime) }
    }

    @Test fun `should stringify a ZonedDateTime`() {
        val str = "2020-04-10T10:15:06.543+10:00[Australia/Sydney]"
        val zonedDateTime = ZonedDateTime.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(zonedDateTime) }
    }

    @Test fun `should stringify a Year`() {
        val str = "2020"
        val year = Year.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(year) }
    }

    @Test fun `should stringify a YearMonth`() {
        val str = "2020-04"
        val yearMonth = YearMonth.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(yearMonth) }
    }

    @Test fun `should stringify a MonthDay`() {
        val str = "--04-23"
        val monthDay = MonthDay.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(monthDay) }
    }

    @Test fun `should stringify a Java Duration`() {
        val str = "PT2H"
        val duration = JavaDuration.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(duration) }
    }

    @Test fun `should stringify a Period`() {
        val str = "P3M"
        val period = Period.parse(str)
        expect("\"$str\"") { JSONStringify.stringify(period) }
    }

    @Test fun `should stringify a Duration`() {
        val str = "PT2H"
        val duration = Duration.parseIsoString(str)
        expect("\"$str\"") { JSONStringify.stringify(duration) }
    }

    @Test fun `should stringify a URI`() {
        val str = "http://pwall.net"
        val uri = URI(str)
        expect("\"$str\"") { JSONStringify.stringify(uri) }
    }

    @Test fun `should stringify a URL`() {
        val str = "http://pwall.net"
        val url = URL(str)
        expect("\"$str\"") { JSONStringify.stringify(url) }
    }

    @Test fun `should stringify a UUID`() {
        val str = "e24b6740-7ac3-11ea-9e47-37640adfe63a"
        val uuid = UUID.fromString(str)
        expect("\"$str\"") { JSONStringify.stringify(uuid) }
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
        expect("\"2019-04-25T18:52:47.123+10:00\"") { JSONStringify.stringify(cal) }
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
        val result = JSONStringify.stringify(date)
        expect(true) { result == expected1 || result == expected2 }
    }

    @Test fun `should stringify a BitSet`() {
        val bitSet = BitSet().apply {
            set(1)
            set(3)
        }
        expect("[1,3]") { JSONStringify.stringify(bitSet) }
    }

    @Test fun `should stringify a simple object`() {
        val dummy1 = Dummy1("abcdef", 98765)
        expect("""{"field1":"abcdef","field2":98765}""") { JSONStringify.stringify(dummy1) }
    }

    @Test fun `should stringify a simple object with extra property`() {
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = "extra123"
        expect("""{"field1":"abcdef","field2":98765,"extra":"extra123"}""") { JSONStringify.stringify(dummy2) }
    }

    @Test fun `should stringify a simple object with extra property omitting null field`() {
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        expect("""{"field1":"abcdef","field2":98765}""") { JSONStringify.stringify(dummy2) }
    }

    @Test fun `should stringify a simple object with extra property including null field when config set`() {
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        val config = JSONConfig {
            includeNulls = true
        }
        expect("""{"field1":"abcdef","field2":98765,"extra":null}""") { JSONStringify.stringify(dummy2, config) }
    }

    @Test fun `should stringify an object of a derived class`() {
        val obj = Derived()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        obj.field3 = 0.012
        val json = JSONStringify.stringify(obj)
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
        val json = JSONStringify.stringify(obj)
        expectJSON(json) {
            count(2)
            property("field1", "qwerty")
            property("fieldX", 98765)
        }
    }

    @Test fun `should stringify an object using parameter name annotation`() {
        val obj = DummyWithParamNameAnnotation("abc", 123)
        val json = JSONStringify.stringify(obj)
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
        val json = JSONStringify.stringify(obj, config)
        expectJSON(json) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify a nested object`() {
        val obj1 = Dummy1("asdfg", 987)
        val obj3 = Dummy3(obj1, "what?")
        val json = JSONStringify.stringify(obj3)
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
        val json = JSONStringify.stringify(obj)
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
        val json = JSONStringify.stringify(obj, config)
        expectJSON(json) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeIfNull`() {
        val obj = DummyWithIncludeIfNull("alpha", null, "gamma")
        val json = JSONStringify.stringify(obj)
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
        val json = JSONStringify.stringify(obj, config)
        expectJSON(json) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeAllProperties`() {
        val obj = DummyWithIncludeAllProperties("alpha", null, "gamma")
        val json = JSONStringify.stringify(obj)
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
        val json = JSONStringify.stringify(obj, config)
        expectJSON(json) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify sealed class with extra member to indicate derived class`() {
        val json = JSONStringify.stringify(Const(2.0))
        expectJSON(json) {
            count(2)
            property("class", "Const")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class object correctly`() {
        val json = JSONStringify.stringify(NotANumber)
        expectJSON(json) {
            count(1)
            property("class", "NotANumber")
        }
    }

    @Test fun `should stringify sealed class with custom discriminator`() {
        val config = JSONConfig {
            sealedClassDiscriminator = "?"
        }
        val json = JSONStringify.stringify(Const(2.0), config)
        expectJSON(json) {
            count(2)
            property("?", "Const")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator`() {
        val json = JSONStringify.stringify(Const2(2.0))
        expectJSON(json) {
            count(2)
            property("type", "Const2")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator and identifiers`() {
        val json = JSONStringify.stringify(Const3(2.0))
        expectJSON(json) {
            count(2)
            property("type", "CONST")
            property("number", BigDecimal(2.0))
        }
    }

    @Test fun `should stringify sealed class with class-specific discriminator and identifiers within class`() {
        val json = JSONStringify.stringify(Organization("ORGANIZATION", 123456, "Funny Company"))
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
        assertFailsWith<JSONKotlinException> { JSONStringify.stringify(circular1) }.let {
            expect("Circular reference to Circular1 at /ref2/ref1") { it.message }
        }
    }

    @Test fun `should fail on use of circular reference in List`() {
        val circularList = mutableListOf<Any>()
        circularList.add(circularList)
        assertFailsWith<JSONKotlinException> { JSONStringify.stringify(circularList) }.let {
            expect("Circular reference to ArrayList at /0") { it.message }
        }
    }

    @Test fun `should fail on use of circular reference in Map`() {
        val circularMap = mutableMapOf<String, Any>()
        circularMap["test1"] = circularMap
        assertFailsWith<JSONKotlinException> { JSONStringify.stringify(circularMap) }.let {
            expect("Circular reference to LinkedHashMap at /test1") { it.message }
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
        expect("""{"innerValue":{"string":"xyz"},"number":999}""") { holder.stringifyJSON() }
    }

    @Test fun `should stringify Opt`() {
        val opt = Opt.of(123)
        expect("123") { opt.stringifyJSON() }
    }

    @Test fun `should stringify Opt missing`() {
        val opt = Opt.unset<Any>()
        expect("""null""") { opt.stringifyJSON() }
    }

    @Test fun `should stringify Opt property`() {
        val optData = OptData(Opt.of(123))
        expect("""{"aaa":123}""") { optData.stringifyJSON() }
    }

    @Test fun `should stringify Opt property missing`() {
        val optData = OptData(Opt.unset())
        expect("""{}""") { optData.stringifyJSON() }
    }

}
