/*
 * @(#) JSONSerializerTest.kt
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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours

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
import java.time.ZoneId
import java.time.ZoneOffset
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
import io.kstuff.test.shouldBeSameInstance
import io.kstuff.test.shouldBeType
import io.kstuff.test.shouldThrow

import io.kjson.optional.Opt
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
import io.kjson.testclasses.JavaClass3
import io.kjson.testclasses.ListEnum
import io.kjson.testclasses.NestedDummy
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.OptData
import io.kjson.testclasses.Organization
import io.kjson.testclasses.TestGenericClass
import io.kjson.testclasses.TestGenericClass2
import io.kjson.testclasses.ValueClass
import io.kjson.testclasses.ValueClassHolder

class JSONSerializerTest {

    @Test fun `should return null for null input`() {
        JSONSerializer.serialize(null as String?) shouldBe null
    }

    @Test
    fun `should return JSONValue as-is`() {
        val json = JSONInt(12345)
        JSONSerializer.serialize(json) shouldBeSameInstance json
    }

    @Test fun `should return String as JSONString`() {
        val str = "Hello JSON!"
        JSONSerializer.serialize(str) shouldBe JSONString(str)
    }

    @Test fun `should return StringBuilder as JSONString`() {
        val str = "Hello JSON!"
        val sb = StringBuilder(str)
        JSONSerializer.serialize(sb) shouldBe JSONString(str)
    }

    @Test fun `should return Char as JSONString`() {
        val char = 'Q'
        JSONSerializer.serialize(char) shouldBe JSONString("Q")
    }

    @Test fun `should return Int as JSONInt`() {
        val i = 123456
        val actual = JSONSerializer.serialize(i)
        actual.shouldBeType<JSONInt>()
        intEquals(i, actual.value) shouldBe true
        // Note - these assertions are complicated because JSONInt.equals() returns true
        // for any comparison with another numeric JSON types where the values are equal
    }

    @Test fun `should return Int (negative) as JSONInt`() {
        val i = -8888
        val actual = JSONSerializer.serialize(i)
        actual.shouldBeType<JSONInt>()
        intEquals(i, actual.value) shouldBe true
    }

    @Test fun `should return Long as JSONLong`() {
        val i = 12345678901234
        val actual = JSONSerializer.serialize(i)
        actual.shouldBeType<JSONLong>()
        longEquals(i, actual.value) shouldBe true
    }

    @Test fun `should return Long (negative) as JSONLong`() {
        val i = -987654321987654321
        val actual = JSONSerializer.serialize(i)
        actual.shouldBeType<JSONLong>()
        longEquals(i, actual.value) shouldBe true
    }

    @Test fun `should return Short as JSONInt`() {
        val i: Short = 1234
        val actual = JSONSerializer.serialize(i)
        actual.shouldBeType<JSONInt>()
        intEquals(i.toInt(), actual.value) shouldBe true
    }

    @Test fun `should return Byte as JSONInt`() {
        val i: Byte = 123
        val actual = JSONSerializer.serialize(i)
        actual.shouldBeType<JSONInt>()
        intEquals(i.toInt(), actual.value) shouldBe true
    }

    @Test fun `should return Float as JSONDecimal`() {
        val f = 0.1234F
        val actual = JSONSerializer.serialize(f)
        actual.shouldBeType<JSONDecimal>()
        floatEquals(f, actual.value.toFloat()) shouldBe true
    }

    @Test fun `should return Float (negative) as JSONDecimal`() {
        val f = -88.987F
        val actual = JSONSerializer.serialize(f)
        actual.shouldBeType<JSONDecimal>()
        floatEquals(f, actual.value.toFloat()) shouldBe true
    }

    @Test fun `should return Double as JSONDecimal`() {
        val d = 987.654321
        val actual = JSONSerializer.serialize(d)
        actual.shouldBeType<JSONDecimal>()
        doubleEquals(d, actual.value.toDouble()) shouldBe true
    }

    @Test fun `should return Double (exponent notation) as JSONDecimal`() {
        val d = 1e40
        val actual = JSONSerializer.serialize(d)
        actual.shouldBeType<JSONDecimal>()
        doubleEquals(d, actual.value.toDouble()) shouldBe true
    }

    @Test fun `should return ULong as JSONLong`() {
        val u = 123456789123456789U
        val actual = JSONSerializer.serialize(u)
        actual.shouldBeType<JSONLong>()
        actual.value shouldBe 123456789123456789
    }

    @Test fun `should return ULong as JSONDecimal if too big for Long`() {
        val u = 9876543210987654321U
        val actual = JSONSerializer.serialize(u)
        actual.shouldBeType<JSONDecimal>()
        actual.value shouldBe BigDecimal("9876543210987654321")
    }

    @Test fun `should return UInt as JSONInt`() {
        val u = 123456789U
        val actual = JSONSerializer.serialize(u)
        actual.shouldBeType<JSONInt>()
        actual.value shouldBe 123456789
    }

    @Test fun `should return UInt as JSONLong if too big for Int`() {
        val u = 2345678901U
        val actual = JSONSerializer.serialize(u)
        actual.shouldBeType<JSONLong>()
        actual.value shouldBe 2345678901
    }

    @Test fun `should return UShort as JSONInt`() {
        val u: UShort = 40000U
        val actual = JSONSerializer.serialize(u)
        actual.shouldBeType<JSONInt>()
        actual.value shouldBe 40000
    }

    @Test fun `should return UByte as JSONInt`() {
        val u: UByte = 200U
        val actual = JSONSerializer.serialize(u)
        actual.shouldBeType<JSONInt>()
        actual.value shouldBe 200
    }

    @Test fun `should return Boolean as JSONBoolean`() {
        JSONSerializer.serialize(true) shouldBeSameInstance JSONBoolean.TRUE
        JSONSerializer.serialize(false) shouldBeSameInstance JSONBoolean.FALSE
    }

    @Test fun `should return CharArray as JSONString`() {
        val ca = charArrayOf('H', 'e', 'l', 'l', 'o', '!')
        JSONSerializer.serialize(ca) shouldBe JSONString("Hello!")
    }

    @Test fun `should return Array of Char as JSONString`() {
        val ca = arrayOf('H', 'e', 'l', 'l', 'o', '!')
        JSONSerializer.serialize(ca) shouldBe JSONString("Hello!")
    }

    @Test fun `should return Array of Int as JSONArray`() {
        val array = arrayOf(123, 2345, 0, 999)
        val expected = JSONArray.build {
            add(123)
            add(2345)
            add(0)
            add(999)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return Array of Int nullable as JSONArray`() {
        val array = arrayOf(123, null, 0, 999)
        val expected = JSONArray.build {
            add(123)
            add(null)
            add(0)
            add(999)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return IntArray as JSONArray`() {
        val array = IntArray(3) { (it + 1) * 111 }
        val expected = JSONArray.build {
            add(111)
            add(222)
            add(333)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return LongArray as JSONArray`() {
        val array = LongArray(3) { (it + 1) * 111111111111 }
        val expected = JSONArray.build {
            add(111111111111)
            add(222222222222)
            add(333333333333)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return ByteArray as JSONArray`() {
        val array = ByteArray(3) { ((it + 1) * 5).toByte() }
        val expected = JSONArray.build {
            add(5)
            add(10)
            add(15)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return ShortArray as JSONArray`() {
        val array = ShortArray(4) { ((it + 1) * 1111).toShort() }
        val expected = JSONArray.build {
            add(1111)
            add(2222)
            add(3333)
            add(4444)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return FloatArray as JSONArray`() {
        val array = FloatArray(4) { it + 0.5F }
        val expected = JSONArray.build {
            add(BigDecimal(0.5))
            add(BigDecimal(1.5))
            add(BigDecimal(2.5))
            add(BigDecimal(3.5))
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return DoubleArray as JSONArray`() {
        val array = DoubleArray(4) { it + 0.5 }
        val expected = JSONArray.build {
            add(BigDecimal(0.5))
            add(BigDecimal(1.5))
            add(BigDecimal(2.5))
            add(BigDecimal(3.5))
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return BooleanArray as JSONArray`() {
        val array = BooleanArray(4) { (it and 1) == 0 }
        val expected = JSONArray.build {
            add(true)
            add(false)
            add(true)
            add(false)
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return Array of String as JSONArray`() {
        val array = arrayOf("Hello", "Kotlin")
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        JSONSerializer.serialize(array) shouldBe expected
    }

    @Test fun `should return Iterator of String as JSONArray`() {
        val iterator = listOf("Hello", "Kotlin").iterator()
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        JSONSerializer.serialize(iterator) shouldBe expected
    }

    @Test fun `should return Enumeration of String as JSONArray`() {
        val list = listOf("Hello", "Kotlin")
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        JSONSerializer.serialize(ListEnum(list)) shouldBe expected
    }

    @Test fun `should return List of String as JSONArray`() {
        val list = listOf("Hello", "Kotlin")
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        JSONSerializer.serialize(list) shouldBe expected
    }

    @Test fun `should return Sequence of String as JSONArray`() {
        val seq = listOf("Hello", "Kotlin").asSequence()
        val expected = JSONArray.build {
            add("Hello")
            add("Kotlin")
        }
        JSONSerializer.serialize(seq) shouldBe expected
    }

    @Test fun `should return Map of String to Int as JSONObject`() {
        val map = mapOf("abc" to 1, "def" to 4, "ghi" to 999)
        val expected = JSONObject.build {
            add("abc", 1)
            add("def", 4)
            add("ghi", 999)
        }
        JSONSerializer.serialize(map) shouldBe expected
    }

    @Test fun `should return Map of String to String nullable as JSONObject`() {
        val map = mapOf("abc" to "hello", "def" to null, "ghi" to "goodbye")
        val expected = JSONObject.build {
            add("abc", "hello")
            add("def", null)
            add("ghi", "goodbye")
        }
        JSONSerializer.serialize(map) shouldBe expected
    }

    @Test fun `should serialize Class with toJSON() as JSONObject`() {
        val obj = DummyFromJSON(23)
        val expected = JSONObject.build {
            add("dec", "23")
            add("hex", "17")
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should serialize Enum as JSONString`() {
        val eee = DummyEnum.GAMMA
        JSONSerializer.serialize(eee) shouldBe JSONString("GAMMA")
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
        JSONSerializer.serialize(cal) shouldBe JSONString("2019-04-25T18:52:47.123+10:00")
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
        JSONSerializer.serialize(date) shouldBeOneOf listOf(expected1, expected2)
    }

    @Test fun `should return java-sql-Date as JSONString`() {
        val str = "2019-04-25"
        val date = java.sql.Date.valueOf(str)
        JSONSerializer.serialize(date) shouldBe JSONString(str)
    }

    @Test fun `should return java-sql-Time as JSONString`() {
        val str = "22:41:19"
        val time = java.sql.Time.valueOf(str)
        JSONSerializer.serialize(time) shouldBe JSONString(str)
    }

    @Test fun `should return java-sql-Timestamp as JSONString`() {
        val str = "2019-04-25 22:41:19.5"
        val timestamp = java.sql.Timestamp.valueOf(str)
        JSONSerializer.serialize(timestamp) shouldBe JSONString(str)
    }

    @Test fun `should return Instant as JSONString`() {
        val str = "2019-04-25T21:01:09.456Z"
        val inst = Instant.parse(str)
        JSONSerializer.serialize(inst) shouldBe JSONString(str)
    }

    @Test fun `should return LocalDate as JSONString`() {
        val date = LocalDate.of(2019, 4, 25)
        JSONSerializer.serialize(date) shouldBe JSONString("2019-04-25")
    }

    @Test fun `should return LocalDateTime as JSONString`() {
        val date = LocalDateTime.of(2019, 4, 25, 21, 6, 5)
        JSONSerializer.serialize(date) shouldBe JSONString("2019-04-25T21:06:05")
    }

    @Test fun `should return LocalTime as JSONString`() {
        val date = LocalTime.of(21, 6, 5)
        JSONSerializer.serialize(date) shouldBe JSONString("21:06:05")
    }

    @Test fun `should return OffsetTime as JSONString`() {
        val time = OffsetTime.of(21, 6, 5, 456000000, ZoneOffset.ofHours(10))
        JSONSerializer.serialize(time) shouldBe JSONString("21:06:05.456+10:00")
    }

    @Test fun `should return OffsetDateTime as JSONString`() {
        val time = OffsetDateTime.of(2019, 4, 25, 21, 6, 5, 456000000, ZoneOffset.ofHours(10))
        JSONSerializer.serialize(time) shouldBe JSONString("2019-04-25T21:06:05.456+10:00")
    }

    @Test fun `should return ZonedDateTime as JSONString`() {
        val zdt = ZonedDateTime.of(2019, 4, 25, 21, 16, 23, 123000000, ZoneId.of("Australia/Sydney"))
        JSONSerializer.serialize(zdt) shouldBe JSONString("2019-04-25T21:16:23.123+10:00[Australia/Sydney]")
    }

    @Test fun `should return Year as JSONString`() {
        val year = Year.of(2019)
        JSONSerializer.serialize(year) shouldBe JSONString("2019")
    }

    @Test fun `should return YearMonth as JSONString`() {
        val yearMonth = YearMonth.of(2019, 4)
        JSONSerializer.serialize(yearMonth) shouldBe JSONString("2019-04")
    }

    @Test fun `should return MonthDay as JSONString`() {
        val month = MonthDay.of(4, 23)
        JSONSerializer.serialize(month) shouldBe JSONString("--04-23")
    }

    @Test fun `should return Java Duration as JSONString`() {
        val javaDuration = JavaDuration.ofHours(2)
        JSONSerializer.serialize(javaDuration) shouldBe JSONString("PT2H")
    }

    @Test fun `should return Period JSONString`() {
        val period = Period.ofMonths(3)
        JSONSerializer.serialize(period) shouldBe JSONString("P3M")
    }

    @Test fun `should return Duration as JSONString`() {
        val duration = 2.hours
        JSONSerializer.serialize(duration) shouldBe JSONString("PT2H")
    }

    @Test fun `should return UUID as JSONString`() {
        val uuidString = "12ce3730-2d97-11e7-aeed-67b0e6bf0ed7"
        val uuid = UUID.fromString(uuidString)
        JSONSerializer.serialize(uuid) shouldBe JSONString(uuidString)
    }

    @Test fun `should return URI as JSONString`() {
        val uriString = "http://pwall.net"
        val uri = URI(uriString)
        JSONSerializer.serialize(uri) shouldBe JSONString(uriString)
    }

    @Test fun `should return URL as JSONString`() {
        val urlString = "http://pwall.net"
        val url = URL(urlString)
        JSONSerializer.serialize(url) shouldBe JSONString(urlString)
    }

    @Test fun `should return BigInteger as JSONDecimal`() {
        val bigIntLong = 123456789012345678L
        val bigInteger = BigInteger.valueOf(bigIntLong)
        val actual = JSONSerializer.serialize(bigInteger)
        actual.shouldBeType<JSONDecimal>()
        actual shouldBe JSONDecimal(BigDecimal(bigIntLong))
    }

    @Test fun `should return BigInteger as JSONString when config option selected`() {
        val bigIntString = "123456789012345678"
        val bigInteger = BigInteger(bigIntString)
        val config = JSONConfig {
            bigIntegerString = true
        }
        JSONSerializer.serialize(bigInteger, config) shouldBe JSONString(bigIntString)
    }

    @Test fun `should return BigDecimal as JSONDecimal`() {
        val bigDecString = "12345678901234567890.88888"
        val bigDecimal = BigDecimal(bigDecString)
        JSONSerializer.serialize(bigDecimal) shouldBe JSONDecimal(bigDecString)
    }

    @Test fun `should return BigDecimal as JSONString when config option selected`() {
        val bigDecString = "12345678901234567890.88888"
        val bigDecimal = BigDecimal(bigDecString)
        val config = JSONConfig {
            bigDecimalString = true
        }
        JSONSerializer.serialize(bigDecimal, config) shouldBe JSONString(bigDecString)
    }

    @Test fun `should return BitSet as JSONArray`() {
        val bitSet = BitSet(4)
        bitSet.set(1)
        bitSet.set(3)
        val expected = JSONArray.build {
            add(1)
            add(3)
        }
        JSONSerializer.serialize(bitSet) shouldBe expected
    }

    @Test fun `should return simple data class as JSONObject`() {
        val obj = Dummy1("abc", 123)
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should return simple data class with extra property as JSONObject`() {
        val obj = Dummy2("abc", 123)
        obj.extra = "qqqqq"
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
            add("extra", "qqqqq")
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should omit null field for simple data class with optional field `() {
        val obj = Dummy2("abc", 123)
        obj.extra = null
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should include null field for simple data class with optional field when config set`() {
        val obj = Dummy2("abc", 123)
        obj.extra = null
        val expected = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
            add("extra", null)
        }
        val config = JSONConfig {
            includeNulls = true
        }
        JSONSerializer.serialize(obj, config) shouldBe expected
    }

    @Test fun `should return derived class as JSONObject`() {
        val obj = Derived()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        obj.field3 = 0.5
        val expected = JSONObject.build {
            add("field1", "qwerty")
            add("field2", 98765)
            add("field3", BigDecimal(0.5))
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should return annotated class as JSONObject using specified name`() {
        val obj = DummyWithNameAnnotation()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        val expected = JSONObject.build {
            add("field1", "qwerty")
            add("fieldX", 98765)
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should return annotated data class as JSONObject using specified name`() {
        val obj = DummyWithParamNameAnnotation("abc", 123)
        val expected = JSONObject.build {
            add("field1", "abc")
            add("fieldX", 123)
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should return annotated data class with custom annotation as JSONObject using specified name`() {
        val obj = DummyWithCustomNameAnnotation("abc", 123)
        val config = JSONConfig {
            addNameAnnotation(CustomName::class, "symbol")
        }
        val expected = JSONObject.build {
            add("field1", "abc")
            add("fieldX", 123)
        }
        JSONSerializer.serialize(obj, config) shouldBe expected
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
        JSONSerializer.serialize(obj3) shouldBe expected
    }

    @Test fun `should return Class with @JSONIgnore as JSONObject skipping field`() {
        val obj = DummyWithIgnore("alpha", "beta", "gamma")
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field3", "gamma")
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should return class with custom ignore annotation as JSONObject skipping field`() {
        val obj = DummyWithCustomIgnore("alpha", "beta", "gamma")
        val config = JSONConfig {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field3", "gamma")
        }
        JSONSerializer.serialize(obj, config) shouldBe expected
    }

    @Test fun `should include null field for class with @JSONIncludeIfNull `() {
        val obj = DummyWithIncludeIfNull("alpha", null, "gamma")
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should include null field for class with custom include if null annotation `() {
        val obj = DummyWithCustomIncludeIfNull("alpha", null, "gamma")
        val config = JSONConfig {
            addIncludeIfNullAnnotation(CustomIncludeIfNull::class)
        }
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        JSONSerializer.serialize(obj, config) shouldBe expected
    }

    @Test fun `should include null field for class with @JSONIncludeAllProperties `() {
        val obj = DummyWithIncludeAllProperties("alpha", null, "gamma")
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should include null field for class with custom include all properties annotation `() {
        val obj = DummyWithCustomIncludeAllProperties("alpha", null, "gamma")
        val config = JSONConfig {
            addIncludeAllPropertiesAnnotation(CustomIncludeAllProperties::class)
        }
        val expected = JSONObject.build {
            add("field1", "alpha")
            add("field2", null)
            add("field3", "gamma")
        }
        JSONSerializer.serialize(obj, config) shouldBe expected
    }

    @Test fun `should return Pair as JSONArray`() {
        val pair = "xyz" to "abc"
        val expected = JSONArray.build {
            add("xyz")
            add("abc")
        }
        JSONSerializer.serialize(pair) shouldBe expected
    }

    @Test fun `should return Triple as JSONArray`() {
        val triple = Triple("xyz", "abc", "def")
        val expected = JSONArray.build {
            add("xyz")
            add("abc")
            add("def")
        }
        JSONSerializer.serialize(triple) shouldBe expected
    }

    @Test fun `should return heterogenous Triple as JSONArray`() {
        val triple = Triple("xyz", 88, "def")
        val expected = JSONArray.build {
            add("xyz")
            add(88)
            add("def")
        }
        JSONSerializer.serialize(triple) shouldBe expected
    }

    @Test fun `should return object as JSONObject`() {
        val obj = DummyObject
        JSONSerializer.serialize(obj) shouldBe JSONObject.build { add("field1", "abc") }
    }

    @Test fun `should return nested object as JSONObject`() {
        val obj = NestedDummy()
        val nested = JSONObject.build { add("field1", "abc") }
        JSONSerializer.serialize(obj) shouldBe JSONObject.build { add("obj", nested) }
    }

    @Test fun `should serialize class with constant val correctly`() {
        val constClass = DummyWithVal()
        JSONSerializer.serialize(constClass) shouldBe JSONObject.build { add("field8", "blert") }
    }

    @Test fun `should serialize java class correctly`() {
        val javaClass1 = JavaClass1(1234, "Hello!")
        val expected = JSONObject.build {
            add("field1", 1234)
            add("field2", "Hello!")
        }
        JSONSerializer.serialize(javaClass1) shouldBe expected
    }

    @Test fun `should serialize list-derived class to JSONArray`() {
        val obj = DummyList(listOf(LocalDate.of(2019, 10, 6), LocalDate.of(2019, 10, 5)))
        val expected = JSONArray.build {
            add("2019-10-06")
            add("2019-10-05")
        }
        JSONSerializer.serialize(obj) shouldBe expected
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
        JSONSerializer.serialize(obj) shouldBe expected
    }

    @Test fun `should serialize sealed class with extra member to indicate derived class`() {
        val expected = JSONObject.build {
            add("class", "Const")
            add("number", BigDecimal(2.0))
        }
        JSONSerializer.serialize(Const(2.0)) shouldBe expected
    }

    @Test fun `should serialize sealed class object correctly`() {
        val expected = JSONObject.build {
            add("class", "NotANumber")
        }
        JSONSerializer.serialize(NotANumber) shouldBe expected
    }

    @Test fun `should serialize sealed class with custom discriminator`() {
        val config = JSONConfig {
            sealedClassDiscriminator = "?"
        }
        val expected = JSONObject.build {
            add("?", "Const")
            add("number", BigDecimal(2.0))
        }
        JSONSerializer.serialize(Const(2.0), config) shouldBe expected
    }

    @Test fun `should serialize sealed class with class-specific discriminator`() {
        val expected = JSONObject.build {
            add("type", "Const2")
            add("number", BigDecimal(2.0))
        }
        JSONSerializer.serialize(Const2(2.0)) shouldBe expected
    }

    @Test fun `should serialize sealed class with class-specific discriminator and identifiers`() {
        val expected = JSONObject.build {
            add("type", "CONST")
            add("number", BigDecimal(2.0))
        }
        JSONSerializer.serialize(Const3(2.0)) shouldBe expected
    }

    @Test fun `should serialize sealed class with class-specific discriminator and identifiers within class`() {
        val expected = JSONObject.build {
            add("type", "ORGANIZATION")
            add("id", 123456)
            add("name", "Funny Company")
        }
        JSONSerializer.serialize(Organization("ORGANIZATION", 123456, "Funny Company")) shouldBe expected
    }

    @Test fun `should fail on use of circular reference`() {
        val circular1 = Circular1()
        val circular2 = Circular2()
        circular1.ref2 = circular2
        circular2.ref1 = circular1
        shouldThrow<JSONKotlinException> { JSONSerializer.serialize(circular1) }.let {
            it.message shouldBe "Circular reference to Circular1, at /ref2/ref1"
        }
    }

    @Test fun `should fail on use of circular reference in List`() {
        val circularList = mutableListOf<Any>()
        circularList.add(circularList)
        shouldThrow<JSONKotlinException>("Circular reference to ArrayList, at /0") {
            JSONSerializer.serialize(circularList)
        }
    }

    @Test fun `should fail on use of circular reference in Map`() {
        val circularMap = mutableMapOf<String, Any>()
        circularMap["test1"] = circularMap
        shouldThrow<JSONKotlinException>("Circular reference to LinkedHashMap, at /test1") {
            JSONSerializer.serialize(circularMap)
        }
    }

    @Test fun `should omit null members`() {
        val dummy5 = Dummy5(null, 123)
        val serialized = JSONSerializer.serialize(dummy5)
        serialized.shouldBeType<JSONObject>()
        serialized.size shouldBe 1
    }

    @Test fun `should serialize Java Stream of strings`() {
        val stream = Stream.of("abc", "def")
        val serialized = JSONSerializer.serialize(stream)
        serialized.shouldBeType<JSONArray>()
        with(serialized) {
            size shouldBe 2
            get(0) shouldBe JSONString("abc")
            get(1) shouldBe JSONString("def")
        }
    }

    @Test fun `should serialize Java IntStream`() {
        val stream = IntStream.of(987, 654, 321)
        val serialized = JSONSerializer.serialize(stream)
        serialized.shouldBeType<JSONArray>()
        with(serialized) {
            size shouldBe 3
            get(0) shouldBe JSONInt(987)
            get(1) shouldBe JSONInt(654)
            get(2) shouldBe JSONInt(321)
        }
    }

    @Test fun `should serialize Java LongStream`() {
        val stream = LongStream.of(987_000_000_000, 654_000_000_000, 321_000_000_000)
        val serialized = JSONSerializer.serialize(stream)
        serialized.shouldBeType<JSONArray>()
        with(serialized) {
            size shouldBe 3
            get(0) shouldBe JSONLong(987_000_000_000)
            get(1) shouldBe JSONLong(654_000_000_000)
            get(2) shouldBe JSONLong(321_000_000_000)
        }
    }

    @Test fun `should serialize value class`() {
        val holder = ValueClassHolder(
            innerValue = ValueClass("xyz"),
            number = 999
        )
        with(JSONSerializer.serialize(holder)) {
            shouldBeType<JSONObject>()
            size shouldBe 2
            with(get("innerValue")) {
                shouldBeType<JSONObject>()
                size shouldBe 1
                get("string") shouldBe JSONString("xyz")
            }
            get("number") shouldBe JSONInt(999)
        }
    }

    @Test fun `should serialize Opt`() {
        val opt = Opt.of(123)
        with(JSONSerializer.serialize(opt)) {
            shouldBeType<JSONInt>()
            value shouldBe 123
        }
    }

    @Test fun `should serialize Opt missing`() {
        val opt = Opt.unset<Any>()
        JSONSerializer.serialize(opt) shouldBe null
    }

    @Test fun `should serialize Opt property`() {
        val optData = OptData(Opt.of(123))
        with(JSONSerializer.serialize(optData)) {
            shouldBeType<JSONObject>()
            size shouldBe 1
            with(this["aaa"]) {
                shouldBeType<JSONInt>()
                value shouldBe 123
            }
        }
    }

    @Test fun `should serialize Opt property missing`() {
        val optData = OptData(Opt.unset())
        with(JSONSerializer.serialize(optData)) {
            shouldBeType<JSONObject>()
            size shouldBe 0
        }
    }

    @Test fun `should serialize a Java class`() {
        val javaClass1 = JavaClass1(98765, "abcdef")
        // Java properties appear to be not necessarily in declaration order
        with(JSONSerializer.serialize(javaClass1)) {
            shouldBeType<JSONObject>()
            size shouldBe 2
            with(this["field1"]) {
                shouldBeType<JSONInt>()
                value shouldBe 98765
            }
            with(this["field2"]) {
                shouldBeType<JSONString>()
                value shouldBe "abcdef"
            }
        }
    }

    @Test fun `should serialize a derived Java class`() {
        val javaClass3 = JavaClass3(98765, "abcdef", true)
        with(JSONSerializer.serialize(javaClass3)) {
            shouldBeType<JSONObject>()
            size shouldBe 3
            with(this["field1"]) {
                shouldBeType<JSONInt>()
                value shouldBe 98765
            }
            with(this["field2"]) {
                shouldBeType<JSONString>()
                value shouldBe "abcdef"
            }
            with(this["flag"]) {
                shouldBeType<JSONBoolean>()
                value shouldBe true
            }
        }
    }

    @Test fun `should serialize generic class`() {
        val data = Dummy1("alpha", 1234)
        val generic = TestGenericClass(
            name = "testAlpha",
            data = data,
        )
        with(JSONSerializer.serialize(generic)) {
            shouldBeType<JSONObject>()
            size shouldBe 2
            with(this["name"]) {
                shouldBeType<JSONString>()
                value shouldBe "testAlpha"
            }
            with(this["data"]) {
                shouldBeType<JSONObject>()
                size shouldBe 2
                with(this["field1"]) {
                    shouldBeType<JSONString>()
                    value shouldBe "alpha"
                }
                with(this["field2"]) {
                    shouldBeType<JSONInt>()
                    value shouldBe 1234
                }
            }
        }
    }

    @Test fun `should serialize generic class with member variables`() {
        val data = Dummy1("alpha", 1234)
        val generic = TestGenericClass2<Dummy1>().apply {
            name = "testAlpha"
            this.data = data
        }
        with(JSONSerializer.serialize(generic)) {
            shouldBeType<JSONObject>()
            size shouldBe 2
            with(this["name"]) {
                shouldBeType<JSONString>()
                value shouldBe "testAlpha"
            }
            with(this["data"]) {
                shouldBeType<JSONObject>()
                size shouldBe 2
                with(this["field1"]) {
                    shouldBeType<JSONString>()
                    value shouldBe "alpha"
                }
                with(this["field2"]) {
                    shouldBeType<JSONInt>()
                    value shouldBe 1234
                }
            }
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
