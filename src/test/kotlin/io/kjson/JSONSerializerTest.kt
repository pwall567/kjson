/*
 * @(#) JSONSerializerTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021 Peter Wall
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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.BitSet
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import java.util.stream.IntStream
import java.util.stream.Stream

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
import io.kjson.testclasses.Dummy5
import io.kjson.testclasses.DummyEnum
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.DummyList
import io.kjson.testclasses.DummyMap
import io.kjson.testclasses.DummyObject
import io.kjson.testclasses.DummyWithCustomIgnore
import io.kjson.testclasses.DummyWithCustomIncludeAllProperties
import io.kjson.testclasses.DummyWithCustomIncludeIfNull
import io.kjson.testclasses.DummyWithCustomNameAnnotation
import io.kjson.testclasses.DummyWithIgnore
import io.kjson.testclasses.DummyWithIncludeAllProperties
import io.kjson.testclasses.DummyWithIncludeIfNull
import io.kjson.testclasses.DummyWithNameAnnotation
import io.kjson.testclasses.DummyWithParamNameAnnotation
import io.kjson.testclasses.DummyWithVal
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.ListEnum
import io.kjson.testclasses.NestedDummy
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.Organization

class JSONSerializerTest {

    @Test fun `should return null for null input`() {
        assertNull(JSONSerializer.serialize(null))
    }

    @Test
    fun `should return JSONValue as-is`() {
        val json = JSONInt(12345)
        assertSame(json, JSONSerializer.serialize(json))
    }

    @Test fun `should return String as JSONString`() {
        val str = "Hello JSON!"
        expect(JSONString(str)) { JSONSerializer.serialize(str) }
    }

    @Test fun `should return StringBuilder as JSONString`() {
        val str = "Hello JSON!"
        val sb = StringBuilder(str)
        expect(JSONString(str)) { JSONSerializer.serialize(sb) }
    }

    @Test fun `should return Char as JSONString`() {
        val char = 'Q'
        expect(JSONString("Q")) { JSONSerializer.serialize(char) }
    }

    @Test fun `should return Int as JSONInt`() {
        val i = 123456
        val actual = JSONSerializer.serialize(i)
        assertTrue(actual is JSONInt)
        assertTrue(intEquals(i, actual.value))
        // Note - these assertions are complicated because JSONInt.equals() returns true
        // for any comparison with another numeric JSON types where the values are equal
    }

    @Test fun `should return Int (negative) as JSONInt`() {
        val i = -8888
        val actual = JSONSerializer.serialize(i)
        assertTrue(actual is JSONInt)
        assertTrue(intEquals(i, actual.value))
    }

    @Test fun `should return Long as JSONLong`() {
        val i = 12345678901234
        val actual = JSONSerializer.serialize(i)
        assertTrue(actual is JSONLong)
        assertTrue(longEquals(i, actual.value))
    }

    @Test fun `should return Long (negative) as JSONLong`() {
        val i = -987654321987654321
        val actual = JSONSerializer.serialize(i)
        assertTrue(actual is JSONLong)
        assertTrue(longEquals(i, actual.value))
    }

    @Test fun `should return Short as JSONInt`() {
        val i: Short = 1234
        val actual = JSONSerializer.serialize(i)
        assertTrue(actual is JSONInt)
        assertTrue(intEquals(i.toInt(), actual.value))
    }

    @Test fun `should return Byte as JSONInt`() {
        val i: Byte = 123
        val actual = JSONSerializer.serialize(i)
        assertTrue(actual is JSONInt)
        assertTrue(intEquals(i.toInt(), actual.value))
    }

    @Test fun `should return Float as JSONDecimal`() {
        val f = 0.1234F
        val actual = JSONSerializer.serialize(f)
        assertTrue(actual is JSONDecimal)
        assertTrue(floatEquals(f, actual.value.toFloat()))
    }

    @Test fun `should return Float (negative) as JSONDecimal`() {
        val f = -88.987F
        val actual = JSONSerializer.serialize(f)
        assertTrue(actual is JSONDecimal)
        assertTrue(floatEquals(f, actual.value.toFloat()))
    }

    @Test fun `should return Double as JSONDecimal`() {
        val d = 987.654321
        val actual = JSONSerializer.serialize(d)
        assertTrue(actual is JSONDecimal)
        assertTrue(doubleEquals(d, actual.value.toDouble()))
    }

    @Test fun `should return Double (exponent notation) as JSONDecimal`() {
        val d = 1e40
        val actual = JSONSerializer.serialize(d)
        assertTrue(actual is JSONDecimal)
        assertTrue(doubleEquals(d, actual.value.toDouble()))
    }

    @Test fun `should return Boolean as JSONBoolean`() {
        assertSame(JSONBoolean.TRUE, JSONSerializer.serialize(true))
        assertSame(JSONBoolean.FALSE, JSONSerializer.serialize(false))
    }

    @Test fun `should return CharArray as JSONString`() {
        val ca = charArrayOf('H', 'e', 'l', 'l', 'o', '!')
        expect(JSONString("Hello!")) { JSONSerializer.serialize(ca) }
    }

    @Test fun `should return Array of Char as JSONString`() {
        val ca = arrayOf('H', 'e', 'l', 'l', 'o', '!')
        expect(JSONString("Hello!")) { JSONSerializer.serialize(ca) }
    }

    @Test fun `should return Array of Int as JSONArray`() {
        val array = arrayOf(123, 2345, 0, 999)
        val expected = JSONArray.build {
            add(123)
            add(2345)
            add(0)
            add(999)
        }
        expect(expected) { JSONSerializer.serialize(array) }
    }

    @Test fun `should return Array of Int nullable as JSONArray`() {
        val array = arrayOf(123, null, 0, 999)
        val expected = JSONArray.build {
            add(123)
            add(null)
            add(0)
            add(999)
        }
        expect(expected) { JSONSerializer.serialize(array) }
    }

    @Test fun `should return Array of String as JSONArray`() {
        val array = arrayOf("Hello", "Kotlin")
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        expect(expected) { JSONSerializer.serialize(array) }
    }

    @Test fun `should return Iterator of String as JSONArray`() {
        val iterator = listOf("Hello", "Kotlin").iterator()
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        expect(expected) { JSONSerializer.serialize(iterator) }
    }

    @Test fun `should return Enumeration of String as JSONArray`() {
        val list = listOf("Hello", "Kotlin")
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        expect(expected) { JSONSerializer.serialize(ListEnum(list)) }
    }

    @Test fun `should return List of String as JSONArray`() {
        val list = listOf("Hello", "Kotlin")
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        expect(expected) { JSONSerializer.serialize(list) }
    }

    @Test fun `should return Sequence of String as JSONArray`() {
        val seq = listOf("Hello", "Kotlin").asSequence()
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        expect(expected) { JSONSerializer.serialize(seq) }
    }

    @Test fun `should return Map of String to Int as JSONObject`() {
        val map = mapOf("abc" to 1, "def" to 4, "ghi" to 999)
        val expected = JSONObject.build {
            add("abc", 1)
            add("def", 4)
            add("ghi", 999)
        }
        expect(expected) { JSONSerializer.serialize(map) }
    }

    @Test fun `should return Map of String to String nullable as JSONObject`() {
        val map = mapOf("abc" to "hello", "def" to null, "ghi" to "goodbye")
        val expected = JSONObject.build {
            add("abc", "hello")
            add("def", null)
            add("ghi", "goodbye")
        }
        expect(expected) { JSONSerializer.serialize(map) }
    }

    @Test fun `should serialize Class with toJSON() as JSONObject`() {
        val obj = DummyFromJSON(23)
        val expected = JSONObject.build {
            add("dec", "23")
            add("hex", "17")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should serialize Enum as JSONString`() {
        val eee = DummyEnum.GAMMA
        expect(JSONString("GAMMA")) { JSONSerializer.serialize(eee) }
    }

    @Test fun `should return Calendar as JSONString`() {
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
        expect(JSONString("2019-04-25T18:52:47.123+10:00")) { JSONSerializer.serialize(cal) }
    }

    @Test fun `should return Date as JSONString`() {
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
        val expected1 = JSONString("2019-04-25T18:52:47.123+10:00")
        val expected2 = JSONString("2019-04-25T08:52:47.123Z")
        val result = JSONSerializer.serialize(date)
        assertTrue(result == expected1 || result == expected2)
    }

    @Test fun `should return java-sql-Date as JSONString`() {
        val str = "2019-04-25"
        val date = java.sql.Date.valueOf(str)
        expect(JSONString(str)) { JSONSerializer.serialize(date) }
    }

    @Test fun `should return java-sql-Time as JSONString`() {
        val str = "22:41:19"
        val time = java.sql.Time.valueOf(str)
        expect(JSONString(str)) { JSONSerializer.serialize(time) }
    }

    @Test fun `should return java-sql-Timestamp as JSONString`() {
        val str = "2019-04-25 22:41:19.5"
        val timestamp = java.sql.Timestamp.valueOf(str)
        expect(JSONString(str)) { JSONSerializer.serialize(timestamp) }
    }

    @Test fun `should return Instant as JSONString`() {
        val str = "2019-04-25T21:01:09.456Z"
        val inst = Instant.parse(str)
        expect(JSONString(str)) { JSONSerializer.serialize(inst) }
    }

    @Test fun `should return LocalDate as JSONString`() {
        val date = LocalDate.of(2019, 4, 25)
        expect(JSONString("2019-04-25")) { JSONSerializer.serialize(date) }
    }

    @Test fun `should return LocalDateTime as JSONString`() {
        val date = LocalDateTime.of(2019, 4, 25, 21, 6, 5)
        expect(JSONString("2019-04-25T21:06:05")) { JSONSerializer.serialize(date) }
    }

    @Test fun `should return LocalTime as JSONString`() {
        val date = LocalTime.of(21, 6, 5)
        expect(JSONString("21:06:05")) { JSONSerializer.serialize(date) }
    }

    @Test fun `should return OffsetTime as JSONString`() {
        val time = OffsetTime.of(21, 6, 5, 456000000, ZoneOffset.ofHours(10))
        expect(JSONString("21:06:05.456+10:00")) { JSONSerializer.serialize(time) }
    }

    @Test fun `should return OffsetDateTime as JSONString`() {
        val time = OffsetDateTime.of(2019, 4, 25, 21, 6, 5, 456000000, ZoneOffset.ofHours(10))
        expect(JSONString("2019-04-25T21:06:05.456+10:00")) { JSONSerializer.serialize(time) }
    }

    @Test fun `should return ZonedDateTime as JSONString`() {
        val zdt = ZonedDateTime.of(2019, 4, 25, 21, 16, 23, 123000000, ZoneId.of("Australia/Sydney"))
        expect(JSONString("2019-04-25T21:16:23.123+10:00[Australia/Sydney]")) { JSONSerializer.serialize(zdt) }
    }

    @Test fun `should return Year as JSONString`() {
        val year = Year.of(2019)
        expect(JSONString("2019")) { JSONSerializer.serialize(year) }
    }

    @Test fun `should return YearMonth as JSONString`() {
        val yearMonth = YearMonth.of(2019, 4)
        expect(JSONString("2019-04")) { JSONSerializer.serialize(yearMonth) }
    }

    @Test fun `should return MonthDay as JSONString`() {
        val month = MonthDay.of(4, 23)
        expect(JSONString("--04-23")) { JSONSerializer.serialize(month) }
    }

    @Test fun `should return Duration as JSONString`() {
        val duration = Duration.ofHours(2)
        expect(JSONString("PT2H")) { JSONSerializer.serialize(duration) }
    }

    @Test fun `should return Period JSONString`() {
        val period = Period.ofMonths(3)
        expect(JSONString("P3M")) { JSONSerializer.serialize(period) }
    }

    @Test fun `should return UUID as JSONString`() {
        val uuidString = "12ce3730-2d97-11e7-aeed-67b0e6bf0ed7"
        val uuid = UUID.fromString(uuidString)
        expect(JSONString(uuidString)) { JSONSerializer.serialize(uuid) }
    }

    @Test fun `should return URI as JSONString`() {
        val uriString = "http://pwall.net"
        val uri = URI(uriString)
        expect(JSONString(uriString)) { JSONSerializer.serialize(uri) }
    }

    @Test fun `should return URL as JSONString`() {
        val urlString = "http://pwall.net"
        val url = URL(urlString)
        expect(JSONString(urlString)) { JSONSerializer.serialize(url) }
    }

    @Test fun `should return BigInteger as JSONDecimal`() {
        val bigIntLong = 123456789012345678L
        val bigInteger = BigInteger.valueOf(bigIntLong)
        val actual = JSONSerializer.serialize(bigInteger)
        assertTrue(actual is JSONDecimal)
        expect(JSONDecimal(BigDecimal(bigIntLong))) { actual }
    }

    @Test fun `should return BigInteger as JSONString when config option selected`() {
        val bigIntString = "123456789012345678"
        val bigInteger = BigInteger(bigIntString)
        val config = JSONConfig().apply {
            bigIntegerString = true
        }
        expect(JSONString(bigIntString)) { JSONSerializer.serialize(bigInteger, config) }
    }

    @Test fun `should return BigDecimal as JSONDecimal`() {
        val bigDecString = "12345678901234567890.88888"
        val bigDecimal = BigDecimal(bigDecString)
        expect(JSONDecimal(bigDecString)) { JSONSerializer.serialize(bigDecimal) }
    }

    @Test fun `should return BigDecimal as JSONString when config option selected`() {
        val bigDecString = "12345678901234567890.88888"
        val bigDecimal = BigDecimal(bigDecString)
        val config = JSONConfig().apply {
            bigDecimalString = true
        }
        expect(JSONString(bigDecString)) { JSONSerializer.serialize(bigDecimal, config) }
    }

    @Test fun `should return BitSet as JSONArray`() {
        val bitSet = BitSet(4)
        bitSet.set(1)
        bitSet.set(3)
        val expected = JSONArray.build {
            add(1)
            add(3)
        }
        expect(expected) { JSONSerializer.serialize(bitSet) }
    }

    @Test fun `should return simple data class as JSONObject`() {
        val obj = Dummy1("abc", 123)
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should return simple data class with extra property as JSONObject`() {
        val obj = Dummy2("abc", 123)
        obj.extra = "qqqqq"
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
            add("extra", "qqqqq")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should omit null field for simple data class with optional field `() {
        val obj = Dummy2("abc", 123)
        obj.extra = null
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should include null field for simple data class with optional field when config set`() {
        val obj = Dummy2("abc", 123)
        obj.extra = null
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
            add("extra", null)
        }
        val config = JSONConfig().apply {
            includeNulls = true
        }
        expect(expected) { JSONSerializer.serialize(obj, config) }
    }

    @Test fun `should return derived class as JSONObject`() {
        val obj = Derived()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        obj.field3 = 0.012
        val expected = JSONObject.build {
            add("field1", "qwerty")
            add("field2", 98765)
            add("field3", BigDecimal(0.012))
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should return annotated class as JSONObject using specified name`() {
        val obj = DummyWithNameAnnotation()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        val expected = JSONObject.build {
            add("field1", "qwerty")
            add("fieldX", 98765)
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should return annotated data class as JSONObject using specified name`() {
        val obj = DummyWithParamNameAnnotation("abc", 123)
        val expected = JSONObject.build {
            add("field1", "abc")
            add("fieldX", 123)
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should return annotated data class with custom annotation as JSONObject using specified name`() {
        val obj = DummyWithCustomNameAnnotation("abc", 123)
        val config = JSONConfig().apply {
            addNameAnnotation(CustomName::class, "symbol")
        }
        val expected = JSONObject.build {
            add("field1", "abc")
            add("fieldX", 123)
        }
        expect(expected) { JSONSerializer.serialize(obj, config) }
    }

    @Test fun `should return nested class as nested JSONObject`() {
        val obj1 = Dummy1("asdfg", 987)
        val obj3 = Dummy3(obj1, "what?")
        val expected1 = JSONObject.build {
            add("field1", "asdfg")
            add("field2", 987)
        }
        val expected = JSONObject.build {
            add("dummy1", expected1)
            add("text", "what?")
        }
        expect(expected) { JSONSerializer.serialize(obj3) }
    }

    @Test fun `should return Class with @JSONIgnore as JSONObject skipping field`() {
        val obj = DummyWithIgnore("alpha", "beta", "gamma")
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field3", "gamma")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should return class with custom ignore annotation as JSONObject skipping field`() {
        val obj = DummyWithCustomIgnore("alpha", "beta", "gamma")
        val config = JSONConfig().apply {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field3", "gamma")
        }
        expect(expected) { JSONSerializer.serialize(obj, config) }
    }

    @Test fun `should include null field for class with @JSONIncludeIfNull `() {
        val obj = DummyWithIncludeIfNull("alpha", null, "gamma")
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should include null field for class with custom include if null annotation `() {
        val obj = DummyWithCustomIncludeIfNull("alpha", null, "gamma")
        val config = JSONConfig().apply {
            addIncludeIfNullAnnotation(CustomIncludeIfNull::class)
        }
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        expect(expected) { JSONSerializer.serialize(obj, config) }
    }

    @Test fun `should include null field for class with @JSONIncludeAllProperties `() {
        val obj = DummyWithIncludeAllProperties("alpha", null, "gamma")
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should include null field for class with custom include all properties annotation `() {
        val obj = DummyWithCustomIncludeAllProperties("alpha", null, "gamma")
        val config = JSONConfig().apply {
            addIncludeAllPropertiesAnnotation(CustomIncludeAllProperties::class)
        }
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        expect(expected) { JSONSerializer.serialize(obj, config) }
    }

    @Test fun `should return Pair as JSONArray`() {
        val pair = "xyz" to "abc"
        val expected = JSONArray.build {
            add("xyz")
            add("abc")
        }
        expect(expected) { JSONSerializer.serialize(pair) }
    }

    @Test fun `should return Triple as JSONArray`() {
        val triple = Triple("xyz", "abc", "def")
        val expected = JSONArray.build {
            add("xyz")
            add("abc")
            add("def")
        }
        expect(expected) { JSONSerializer.serialize(triple) }
    }

    @Test fun `should return heterogenous Triple as JSONArray`() {
        val triple = Triple("xyz", 88, "def")
        val expected = JSONArray.build {
            add("xyz")
            add(88)
            add("def")
        }
        expect(expected) { JSONSerializer.serialize(triple) }
    }

    @Test fun `should return object as JSONObject`() {
        val obj = DummyObject
        expect(JSONObject.build { add("field1", "abc") }) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should return nested object as JSONObject`() {
        val obj = NestedDummy()
        val nested = JSONObject.build { add("field1", "abc") }
        expect(JSONObject.build { add("obj", nested) }) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should serialize class with constant val correctly`() {
        val constClass = DummyWithVal()
        expect(JSONObject.build { add("field8", "blert") }) { JSONSerializer.serialize(constClass) }
    }

    @Test fun `should serialize java class correctly`() {
        val javaClass1 = JavaClass1(1234, "Hello!")
        val expected = JSONObject.build {
            add("field1", 1234)
            add("field2", "Hello!")
        }
        expect(expected) { JSONSerializer.serialize(javaClass1) }
    }

    @Test fun `should serialize list-derived class to JSONArray`() {
        val obj = DummyList(listOf(LocalDate.of(2019, 10, 6), LocalDate.of(2019, 10, 5)))
        val expected = JSONArray.build {
            add("2019-10-06")
            add("2019-10-05")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should serialize map-derived class to JSONObject`() {
        val obj = DummyMap(emptyMap()).apply {
            put("aaa", LocalDate.of(2019, 10, 6))
            put("bbb", LocalDate.of(2019, 10, 5))
        }
        val expected = JSONObject.build {
            add("aaa", "2019-10-06")
            add("bbb", "2019-10-05")
        }
        expect(expected) { JSONSerializer.serialize(obj) }
    }

    @Test fun `should serialize sealed class with extra member to indicate derived class`() {
        val expected = JSONObject.build {
            add("class", "Const")
            add("number", BigDecimal(2.0))
        }
        expect(expected) { JSONSerializer.serialize(Const(2.0)) }
    }

    @Test fun `should serialize sealed class object correctly`() {
        val expected = JSONObject.build {
            add("class", "NotANumber")
        }
        expect(expected) { JSONSerializer.serialize(NotANumber) }
    }

    @Test fun `should serialize sealed class with custom discriminator`() {
        val config = JSONConfig().apply {
            sealedClassDiscriminator = "?"
        }
        val expected = JSONObject.build {
            add("?", "Const")
            add("number", BigDecimal(2.0))
        }
        expect(expected) { JSONSerializer.serialize(Const(2.0), config) }
    }

    @Test fun `should serialize sealed class with class-specific discriminator`() {
        val expected = JSONObject.build {
            add("type", "Const2")
            add("number", BigDecimal(2.0))
        }
        expect(expected) { JSONSerializer.serialize(Const2(2.0)) }
    }

    @Test fun `should serialize sealed class with class-specific discriminator and identifiers`() {
        val expected = JSONObject.build {
            add("type", "CONST")
            add("number", BigDecimal(2.0))
        }
        expect(expected) { JSONSerializer.serialize(Const3(2.0)) }
    }

    @Test fun `should serialize sealed class with class-specific discriminator and identifiers within class`() {
        val expected = JSONObject.build {
            add("type", "ORGANIZATION")
            add("id", 123456)
            add("name", "Funny Company")
        }
        expect(expected) { JSONSerializer.serialize(Organization("ORGANIZATION", 123456, "Funny Company")) }
    }

    @Test fun `should fail on use of circular reference`() {
        val circular1 = Circular1()
        val circular2 = Circular2()
        circular1.ref = circular2
        circular2.ref = circular1
        assertFailsWith<JSONKotlinException> { JSONSerializer.serialize(circular1) }.let {
            expect("Circular reference: field ref in Circular2") { it.message }
        }
    }

    @Test fun `should omit null members`() {
        val dummy5 = Dummy5(null, 123)
        val serialized = JSONSerializer.serialize(dummy5)
        assertTrue(serialized is JSONObject)
        expect(1) { serialized.size }
    }

    @Test fun `should serialize Java Stream of strings`() {
        val stream = Stream.of("abc", "def")
        val serialized = JSONSerializer.serialize(stream)
        assertTrue(serialized is JSONArray)
        with(serialized) {
            expect(2) { size }
            expect(JSONString("abc")) { get(0) }
            expect(JSONString("def")) { get(1) }
        }
    }

    @Test fun `should serialize Java IntStream`() {
        val stream = IntStream.of(987, 654, 321)
        val serialized = JSONSerializer.serialize(stream)
        assertTrue(serialized is JSONArray)
        with(serialized) {
            expect(3) { size }
            expect(JSONInt(987)) { get(0) }
            expect(JSONInt(654)) { get(1) }
            expect(JSONInt(321)) { get(2) }
        }
    }

    private fun intEquals(a: Int, b: Int): Boolean {
        return a == b
    }

    private fun longEquals(a: Long, b: Long): Boolean {
        return a == b
    }

    private fun floatEquals(a: Float, b: Float): Boolean {
        return abs(a - b) < 0.0000001
    }

    private fun doubleEquals(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.000000001
    }

}
