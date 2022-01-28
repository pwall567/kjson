/*
 * @(#) JSONDeserializerTest.kt
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
import kotlin.reflect.full.starProjectedType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail
import kotlin.time.Duration.Companion.hours

import java.lang.reflect.Type
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
import java.util.Arrays
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.LinkedList
import java.util.TimeZone
import java.util.UUID
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

import io.kjson.JSON.asLong
import io.kjson.testclasses.Const
import io.kjson.testclasses.Const2
import io.kjson.testclasses.Const3
import io.kjson.testclasses.CustomAllowExtraProperties
import io.kjson.testclasses.CustomIgnore
import io.kjson.testclasses.CustomName
import io.kjson.testclasses.Derived
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.Dummy2
import io.kjson.testclasses.Dummy3
import io.kjson.testclasses.Dummy4
import io.kjson.testclasses.Dummy5
import io.kjson.testclasses.DummyEnum
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.DummyList
import io.kjson.testclasses.DummyMap
import io.kjson.testclasses.DummyMultipleFromJSON
import io.kjson.testclasses.DummyObject
import io.kjson.testclasses.DummyWithAllowExtra
import io.kjson.testclasses.DummyWithCustomAllowExtra
import io.kjson.testclasses.DummyWithCustomIgnore
import io.kjson.testclasses.DummyWithCustomNameAnnotation
import io.kjson.testclasses.DummyWithIgnore
import io.kjson.testclasses.DummyWithNameAnnotation
import io.kjson.testclasses.DummyWithParamNameAnnotation
import io.kjson.testclasses.DummyWithVal
import io.kjson.testclasses.Expr
import io.kjson.testclasses.Expr2
import io.kjson.testclasses.Expr3
import io.kjson.testclasses.JavaClass1
import io.kjson.testclasses.JavaClass2
import io.kjson.testclasses.MultiConstructor
import io.kjson.testclasses.NotANumber
import io.kjson.testclasses.Organization
import io.kjson.testclasses.Party
import io.kjson.testclasses.Person
import io.kjson.testclasses.SealedClassContainer
import io.kjson.testclasses.Super

class JSONDeserializerTest {

    @Test fun `should return null from null input`() {
        assertNull(JSONDeserializer.deserialize(String::class, null))
    }

    @Test fun `should throw exception wen non-null function called with null`() {
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserializeNonNull(String::class, null) }
        expect("Can't deserialize null as String") { e.message }
    }

    @Test fun `should pass JSONValue through unchanged`() {
        val json = JSONDecimal("0.1")
        assertSame(json, JSONDeserializer.deserialize(JSONValue::class,  json))
        assertSame(json, JSONDeserializer.deserialize(JSONDecimal::class,  json))
        val json2 = JSONString("abc")
        assertSame(json2, JSONDeserializer.deserialize(JSONValue::class,  json2))
        assertSame(json2, JSONDeserializer.deserialize(JSONString::class,  json2))
    }

    @Test fun `should use companion object fromJSON function`() {
        val json = JSONObject.build {
            add("dec", "17")
            add("hex", "11")
        }
        val expected = DummyFromJSON(17)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should select correct companion object fromJSON function`() {
        val json1 = JSONObject.build {
            add("dec", "17")
            add("hex", "11")
        }
        val expected1 = DummyMultipleFromJSON(17)
        expect(expected1) { JSONDeserializer.deserialize(json1) }
        val json2 = JSONInt(300)
        val expected2 = DummyMultipleFromJSON(300)
        expect(expected2) { JSONDeserializer.deserialize(json2) }
        val json3 = JSONString("FF")
        val expected3 = DummyMultipleFromJSON(255)
        expect(expected3) { JSONDeserializer.deserialize(json3) }
    }

    @Test fun `should return string from JSONString`() {
        val json = JSONString("abc")
        val expected = "abc"
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return character from single character JSONString`() {
        val json = JSONString("Q")
        val expected = 'Q'
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return character array from JSONString`() {
        val json = JSONString("abcdef")
        val expected = arrayOf('a', 'b', 'c', 'd', 'e', 'f').toCharArray()
        assertTrue(Arrays.equals(expected, JSONDeserializer.deserialize(json)))
    }

    @Test fun `should return array of Char from JSONString`() {
        val json = JSONString("abcdef")
        val expected = arrayOf('a', 'b', 'c', 'd', 'e', 'f')
        assertTrue(Arrays.equals(expected, JSONDeserializer.deserialize(Array<Char>::class, json)))
    }

    @Test fun `should return Calendar JSONString`() {
        val json = JSONString("2019-04-19T15:34:02.234+10:00")
        val cal = Calendar.getInstance()
        cal.set(2019, 3, 19, 15, 34, 2) // month value is month - 1
        cal.set(Calendar.MILLISECOND, 234)
        cal.set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        assertTrue(calendarEquals(cal, JSONDeserializer.deserialize(json)!!))
    }

    @Test fun `should return Date from JSONString`() {
        val json = JSONString("2019-03-10T15:34:02.234+11:00")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        cal.set(2019, 2, 10, 15, 34, 2) // month value is month - 1
        cal.set(Calendar.MILLISECOND, 234)
        cal.set(Calendar.ZONE_OFFSET, 11 * 60 * 60 * 1000)
        val expected: Date? = cal.time
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return java-sql-Date from JSONString`() {
        val json = JSONString("2019-03-10")
        val expected: java.sql.Date? = java.sql.Date.valueOf("2019-03-10")
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return java-sql-Time from JSONString`() {
        val json = JSONString("22:45:41")
        val expected: java.sql.Time? = java.sql.Time.valueOf("22:45:41")
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return java-sql-Timestamp from JSONString`() {
        val json = JSONString("2019-03-10 22:45:41.5")
        val expected: java.sql.Timestamp? = java.sql.Timestamp.valueOf("2019-03-10 22:45:41.5")
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Instant from JSONString`() {
        val json = JSONString("2019-03-10T15:34:02.234Z")
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("GMT")
        cal.set(2019, 2, 10, 15, 34, 2) // month value is month - 1
        cal.set(Calendar.MILLISECOND, 234)
        cal.set(Calendar.ZONE_OFFSET, 0)
        val expected = Instant.ofEpochMilli(cal.timeInMillis)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return LocalDate from JSONString`() {
        val json = JSONString("2019-03-10")
        val expected = LocalDate.of(2019, 3, 10)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return LocalDateTime from JSONString`() {
        val json = JSONString("2019-03-10T16:43:33")
        val expected = LocalDateTime.of(2019, 3, 10, 16, 43, 33)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return LocalTime from JSONString`() {
        val json = JSONString("16:43:33")
        val expected = LocalTime.of(16, 43, 33)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return OffsetTime from JSONString`() {
        val json = JSONString("16:46:11.234+10:00")
        val expected = OffsetTime.of(16, 46, 11, 234000000, ZoneOffset.ofHours(10))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return OffsetDateTime from JSONString`() {
        val json = JSONString("2019-03-10T16:46:11.234+10:00")
        val expected = OffsetDateTime.of(2019, 3, 10, 16, 46, 11, 234000000, ZoneOffset.ofHours(10))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return ZonedDateTime from JSONString`() {
        val json = JSONString("2019-01-10T16:46:11.234+11:00[Australia/Sydney]")
        val expected = ZonedDateTime.of(2019, 1, 10, 16, 46, 11, 234000000, ZoneId.of("Australia/Sydney"))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Year from JSONString`() {
        val json = JSONString("2019")
        val expected = Year.of(2019)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return YearMonth from JSONString`() {
        val json = JSONString("2019-03")
        val expected = YearMonth.of(2019, 3)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return MonthDay from JSONString`() {
        val json = JSONString("--03-10")
        val expected = MonthDay.of(3, 10)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Java Duration from JSONString`() {
        val json = JSONString("PT2H")
        val expected = JavaDuration.ofHours(2)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Period from JSONString`() {
        val json = JSONString("P3M")
        val expected = Period.ofMonths(3)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Duration from JSONString`() {
        val json = JSONString("PT2H")
        val expected = 2.hours
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return UUID from JSONString`() {
        val uuid = "b082b046-ac9b-11eb-8ea7-5fc81989f104"
        val json = JSONString(uuid)
        val expected = UUID.fromString("b082b046-ac9b-11eb-8ea7-5fc81989f104")
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should fail on invalid UUID`() {
        val json = JSONString("b082b046-ac9b-11eb-8ea7-5fc81989f1") // 2 bytes too short
        assertFailsWith<JSONException> { JSONDeserializer.deserialize<UUID>(json) }.let {
            expect("Error deserializing \"b082b046-ac9b-11eb-8ea7-5fc81989f1\" as UUID") { it.message }
        }
    }

    @Test fun `should return URI from JSONString`() {
        val uriString = "http://kjson.io"
        val json = JSONString(uriString)
        val expected = URI(uriString)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return URL from JSONString`() {
        val urlString = "https://kjson.io"
        val json = JSONString(urlString)
        val expected = URL(urlString)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

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

    @Test fun `should return enum from JSONString`() {
        val json = JSONString("ALPHA")
        val expected = DummyEnum.ALPHA
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BigInteger from JSONString`() {
        val str = "123456789"
        val json = JSONString(str)
        val expected = BigInteger(str)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BigInteger from JSONLong`() {
        val value = 123456789012345678
        val json = JSONLong(value)
        val expected: BigInteger? = BigInteger.valueOf(value)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BigInteger from JSONInt`() {
        val value = 12345678
        val json = JSONInt(value)
        val expected: BigInteger? = BigInteger.valueOf(value.toLong())
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BigInteger from JSONDecimal`() {
        val json = JSONDecimal("123456789.00")
        val expected: BigInteger? = BigInteger.valueOf(json.asLong)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BigDecimal from JSONString`() {
        val str = "123456789.77777"
        val json = JSONString(str)
        val expected = BigDecimal(str)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return BigDecimal from JSONDecimal`() {
        val str = "123456789.77777"
        val json = JSONDecimal(str)
        val expected = BigDecimal(str)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Int from JSONInt`() {
        val json = JSONInt(1234)
        val expected = 1234
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Long from JSONLong`() {
        val json = JSONLong(123456789012345)
        val expected = 123456789012345
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Short from JSONInt`() {
        val json = JSONInt(1234)
        val expected: Short = 1234
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return Byte from JSONInt`() {
        val json = JSONInt(123)
        val expected: Byte = 123
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return ULong from JSONLong`() {
        val json = JSONLong(123456789123456789)
        val expected: ULong = 123456789123456789U
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return ULong from JSONDecimal`() {
        val json = JSONDecimal("9223372036854775808")
        val expected: ULong = 9223372036854775808U // Long.MAX_VALUE + 1
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return UInt from JSONInt`() {
        val json = JSONInt(1234567)
        val expected: UInt = 1234567U
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return UShort from JSONInt`() {
        val json = JSONInt(40000)
        val expected: UShort = 40000U
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return UByte from JSONInt`() {
        val json = JSONInt(200)
        val expected: UByte = 200U
        expect(expected) { JSONDeserializer.deserialize(json) }
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
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(BooleanArray::class, json) }
        expect("Can't deserialize 123 as Boolean at /0") { e.message }
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
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(IntArray::class, json) }
        expect("Can't deserialize \"12345\" as Int at /0") { e.message }
    }

    @Test fun `should fail deserializing JSONArray to IntArray if entries not integer`() {
        val json = JSONArray.build {
            add(12345)
            add(BigDecimal("0.125"))
            add(321321)
        }
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(IntArray::class, json) }
        expect("Can't deserialize 0.125 as Int at /1") { e.message }
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

    private val stringType = String::class.starProjectedType
    private val stringTypeProjection = KTypeProjection.invariant(stringType)
    private val intType = Int::class.starProjectedType
    private val intTypeProjection = KTypeProjection.invariant(intType)
    private val listStringType = List::class.createType(listOf(stringTypeProjection))
    private val arrayListStringType = ArrayList::class.createType(listOf(stringTypeProjection))
    private val linkedListStringType = LinkedList::class.createType(listOf(stringTypeProjection))
    private val setStringType = Set::class.createType(listOf(stringTypeProjection))
    private val hashSetStringType = HashSet::class.createType(listOf(stringTypeProjection))
    private val linkedHashSetStringType = LinkedHashSet::class.createType(listOf(stringTypeProjection))
    private val mapStringIntType = Map::class.createType(listOf(stringTypeProjection, intTypeProjection))
    private val linkedHashMapStringIntType = LinkedHashMap::class.createType(listOf(stringTypeProjection,
        intTypeProjection))

    private val listStrings = listOf("abc", "def")
    private val jsonArrayString = JSONArray.build {
        add("abc")
        add("def")
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
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(setStringType, jsonArrayDuplicate) }
        expect("Duplicate not allowed at /2") { e.message }
    }

    @Test fun `should return HashSet of String from JSONArray of JSONString`() {
        expect(HashSet(listStrings)) { JSONDeserializer.deserialize(hashSetStringType, jsonArrayString) }
    }

    @Test fun `should return LinkedHashSet of String from JSONArray of JSONString`() {
        expect(LinkedHashSet(listStrings)) { JSONDeserializer.deserialize(linkedHashSetStringType, jsonArrayString) }
    }

    private val mapStringInt = mapOf("abc" to 123, "def" to 456, "ghi" to 789)
    private val jsonObjectInt = JSONObject.build {
        add("abc", 123)
        add("def", 456)
        add("ghi", 789)
    }

    @Test fun `should return map of String to Int from JSONObject`() {
        expect(mapStringInt) { JSONDeserializer.deserialize(mapStringIntType, jsonObjectInt)}
    }

    @Test fun `should return LinkedHashMap of String to Int from JSONObject`() {
        val linkedHashMapStringInt = LinkedHashMap(mapStringInt)
        val result = JSONDeserializer.deserialize(linkedHashMapStringIntType, jsonObjectInt)
        assertEquals(linkedHashMapStringInt, result)
        assertTrue(result is LinkedHashMap<*, *>)
    }

    @Test fun `should return simple data class from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 12345)
        }
        val expected = Dummy1("Hello", 12345)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return simple data class with default parameter from JSONObject`() {
        val json = JSONObject.build { add("field1", "Hello") }
        val expected = Dummy1("Hello")
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should return data class with extra values from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 12345)
            add("extra", "XXX")
        }
        val expected = Dummy2("Hello", 12345)
        expected.extra = "XXX"
        val result = JSONDeserializer.deserialize<Dummy2>(json)
        assertEquals(expected, result)
        assertEquals("XXX", result?.extra)
    }

    @Test fun `should return nested data class from JSONObject`() {
        val json1 = JSONObject.build {
            add("field1", "Whoa")
            add("field2", 98765)
        }
        val json2 = JSONObject.build {
            add("dummy1", json1)
            add("text", "special")
        }
        val expected = Dummy3(Dummy1("Whoa", 98765), "special")
        expect(expected) { JSONDeserializer.deserialize(json2) }
    }

    @Test fun `should return nested data class with list from JSONObject`() {
        val json1 = JSONObject.build {
            add("field1", "Whoa")
            add("field2", 98765)
        }
        val json2 = JSONObject.build {
            add("field1", "Hi!")
            add("field2", 333)
        }
        val json3 = JSONArray.build {
            add(json1)
            add(json2)
        }
        val json4 = JSONObject.build {
            add("listDummy1", json3)
            add("text", "special")
        }
        val expected = Dummy4(listOf(Dummy1("Whoa", 98765), Dummy1("Hi!", 333)), "special")
        expect(expected) { JSONDeserializer.deserialize(json4) }
    }

    @Test fun `should return simple class with properties fromJSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("field2", 888)
        }
        val expected = Super()
        expected.field1 = "qqq"
        expected.field2 = 888
        expect(expected) { JSONDeserializer.deserialize(Super::class, json) }
    }

    @Test fun `should return derived class with properties from JSONObject`() {
        // also test parsing from String
        val str = """{"field1":"qqq","field2":888,"field3":12345.0}"""
        val expected = Derived()
        expected.field1 = "qqq"
        expected.field2 = 888
        expected.field3 = 12345.0
        expect(expected) { JSONDeserializer.deserialize(Derived::class, JSON.parse(str)) }
    }

    @Test fun `should return simple class with properties using name annotation from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("fieldX", 888)
        }
        val expected = DummyWithNameAnnotation()
        expected.field1 = "qqq"
        expected.field2 = 888
        expect(expected) { JSONDeserializer.deserialize(DummyWithNameAnnotation::class, json) }
    }

    @Test fun `should return data class using name annotation from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("fieldX", 888)
        }
        expect(DummyWithParamNameAnnotation("qqq", 888)) {
            JSONDeserializer.deserialize(DummyWithParamNameAnnotation::class, json)
        }
    }

    @Test fun `should return data class using custom name annotation from JSONObject`() {
        val json = JSONObject.build {
            add("field1", "qqq")
            add("fieldX", 888)
        }
        val expected = DummyWithCustomNameAnnotation("qqq", 888)
        val config = JSONConfig().apply {
            addNameAnnotation(CustomName::class, "symbol")
        }
        expect(expected) { JSONDeserializer.deserialize(DummyWithCustomNameAnnotation::class, json, config) }
    }

    private val pairStringStringType = Pair::class.createType(listOf(stringTypeProjection, stringTypeProjection))
    private val pairStringIntType = Pair::class.createType(listOf(stringTypeProjection, intTypeProjection))
    private val tripleStringStringStringType = Triple::class.createType(listOf(stringTypeProjection,
        stringTypeProjection, stringTypeProjection))
    private val tripleStringIntStringType = Triple::class.createType(listOf(stringTypeProjection,
        intTypeProjection, stringTypeProjection))

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

    @Test fun `should return null for nullable String from null`() {
        val json: JSONValue? = null
        assertNull(JSONDeserializer.deserialize(String::class.createType(emptyList(), true), json))
    }

    @Test fun `should fail deserializing null for non-nullable String`() {
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize(stringType, null) }
        expect("Can't deserialize null as String") { e.message }
    }

    @Test fun `should deserialize to object from JSONObject`() {
        val json = JSONObject.build { add("field1", "abc") }
        expect(DummyObject) { JSONDeserializer.deserialize(DummyObject::class, json) }
    }

    @Test fun `should deserialize class with constant val correctly`() {
        val json = JSONObject.build { add("field8", "blert") }
        expect(DummyWithVal()) { JSONDeserializer.deserialize(DummyWithVal::class, json) }
    }

    @Test fun `should deserialize java class correctly`() {
        val json = JSONObject.build {
            add("field1", 1234)
            add("field2", "Hello!")
        }
        expect(JavaClass1(1234, "Hello!")) { JSONDeserializer.deserialize(JavaClass1::class, json) }
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

    @Test fun `should deserialize JSONObject into Map derived type`() {
        val json = JSONObject.build {
            add("aaa", "2019-10-06")
            add("bbb", "2019-10-05")
        }
        val expected = DummyMap(emptyMap()).apply {
            put("aaa", LocalDate.of(2019, 10, 6))
            put("bbb", LocalDate.of(2019, 10, 5))
        }
        expect(expected) { JSONDeserializer.deserialize(DummyMap::class, json) }
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

    @Test fun `should deserialize JSONString to Any`() {
        val json = JSONString("Hello!")
        expect("Hello!") { JSONDeserializer.deserializeAny(json) }
    }

    @Test fun `should deserialize JSONBoolean to Any`() {
        val json1 = JSONBoolean.TRUE
        val result1 = JSONDeserializer.deserializeAny(json1)
        assertTrue(result1 is Boolean && result1)
        val json2 = JSONBoolean.FALSE
        val result2 = JSONDeserializer.deserializeAny(json2)
        assertTrue(result2 is Boolean && !result2)
    }

    @Test fun `should deserialize JSONInt to Any`() {
        val json = JSONInt(123456)
        expect(123456) { JSONDeserializer.deserializeAny(json) }
    }

    @Test fun `should deserialize JSONLong to Any`() {
        val json = JSONLong(1234567890123456L)
        expect(1234567890123456L) { JSONDeserializer.deserializeAny(json) }
    }

    @Test fun `should deserialize JSONArray to Any`() {
        val json = JSONArray.build {
            add("abcde")
            add("fghij")
        }
        expect(listOf("abcde", "fghij")) { JSONDeserializer.deserializeAny(json) }
    }

    @Test fun `should deserialize JSONObject to Any`() {
        val json = JSONObject.build {
            add("aaa", 1234)
            add("ccc", 9999)
            add("bbb", 5678)
            add("abc", 8888)
        }
        val result = JSONDeserializer.deserializeAny(json)
        // check that the result is a map in the correct order
        if (result is Map<*, *>) {
            val iterator = result.keys.iterator()
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("aaa") { it }
                expect(1234) { result[it] }
            }
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("ccc") { it }
                expect(9999) { result[it] }
            }
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("bbb") { it }
                expect(5678) { result[it] }
            }
            assertTrue(iterator.hasNext())
            iterator.next().let {
                expect("abc") { it }
                expect(8888) { result[it] }
            }
            assertFalse(iterator.hasNext())
        }
        else
            fail("Not a Map - $result")
    }

    @Test fun `should deserialize sealed class to correct subclass`() {
        val json = JSONObject.build {
            add("class", "Const")
            add("number", BigDecimal("2.0"))
        }
        expect(Const(2.0)) { JSONDeserializer.deserialize<Expr>(json) }
    }

    @Test fun `should deserialize sealed class to correct object subclass`() {
        val json = JSONObject.build {
            add("class", "NotANumber")
        }
        expect(NotANumber) { JSONDeserializer.deserialize<Expr>(json) }
    }

    @Test fun `should deserialize sealed class with custom discriminator`() {
        val config = JSONConfig().apply {
            sealedClassDiscriminator = "?"
        }
        val json = JSONObject.build {
            add("?", "Const")
            add("number", BigDecimal("2.0"))
        }
        expect(Const(2.0)) { JSONDeserializer.deserialize<Expr>(json, config) }
    }

    @Test fun `should deserialize sealed class with class-specific discriminator`() {
        val json = JSONObject.build {
            add("type", "Const2")
            add("number", BigDecimal("2.0"))
        }
        expect(Const2(2.0)) { JSONDeserializer.deserialize<Expr2>(json) }
    }

    @Test fun `should deserialize sealed class with class-specific discriminator and identifiers`() {
        val json = JSONObject.build {
            add("type", "CONST")
            add("number", BigDecimal("2.0"))
        }
        expect(Const3(2.0)) { JSONDeserializer.deserialize<Expr3>(json) }
    }

    @Test fun `should deserialize sealed class with class-specific discriminator and identifier within class`() {
        val org = JSONObject.build {
            add("type", "ORGANIZATION")
            add("id", 123456)
            add("name", "Funny Company")
        }
        expect(Organization("ORGANIZATION", 123456, "Funny Company")) { JSONDeserializer.deserialize<Party>(org) }
        val person = JSONObject.build {
            add("type", "PERSON")
            add("firstName", "William")
            add("lastName", "Wordsworth")
        }
        expect(Person("PERSON", "William", "Wordsworth")) { JSONDeserializer.deserialize<Party>(person) }
    }

    @Test fun `should ignore additional fields when allowExtra set in config`() {
        val config = JSONConfig().apply {
            allowExtra = true
        }
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 123)
            add("extra", "allow")
        }
        expect(Dummy1("Hello", 123)) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `should ignore additional fields when class annotated with @JSONAllowExtra`() {
        val json = JSONObject.build {
            add("field1", "Hello")
            add("field2", 123)
            add("extra", "allow")
        }
        expect(DummyWithAllowExtra("Hello", 123)) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should ignore additional fields when class annotated with custom allow extra`() {
        val config = JSONConfig().apply {
            addAllowExtraPropertiesAnnotation(CustomAllowExtraProperties::class)
        }
        val json = JSONObject.build {
            add("field1", "Hi")
            add("field2", 123)
            add("extra", "allow")
        }
        expect(DummyWithCustomAllowExtra("Hi", 123)) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `field annotated with @JSONIgnore should be ignored on deserialization`() {
        val json = JSONObject.build {
            add("field1", "one")
            add("field2", "two")
            add("field3", "three")
        }
        expect(DummyWithIgnore(field1 = "one", field3 = "three")) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `field annotated with custom ignore annotation should be ignored on deserialization`() {
        val config = JSONConfig().apply {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        val json = JSONObject.build {
            add("field1", "one")
            add("field2", "two")
            add("field3", "three")
        }
        expect(DummyWithCustomIgnore(field1 = "one", field3 = "three")) { JSONDeserializer.deserialize(json, config) }
    }

    @Test fun `should deserialize missing members as null where allowed`() {
        val json = JSONObject.of("field2" to JSONInt(123))
        expect(Dummy5(null, 123)) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize custom parameterised type`() {
        val json = JSONObject.build {
            add("lines", JSONArray.of(JSONString("abc"), JSONString("def")))
        }
        val expected = TestPage(lines = listOf("abc", "def"))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize nested custom parameterised type`() {
        val json1 = JSONObject.build {
            add("lines", JSONArray.of(JSONString("abc"), JSONString("def")))
        }
        val json2 = JSONArray.of(json1, JSONString("xyz"))
        val expected = TestPage(lines = listOf("abc", "def")) to "xyz"
        expect(expected) { JSONDeserializer.deserialize(json2) }
    }

    @Test fun `should deserialize differently nested custom parameterised type`() {
        val json = JSONObject.build {
            add("lines", JSONArray.of(JSONArray.of(JSONString("abc"), JSONString("ABC")),
                    JSONArray.of(JSONString("def"), JSONString("DEF"))))
        }
        val expected = TestPage(lines = listOf("abc" to "ABC", "def" to "DEF"))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize complex custom parameterised type`() {
        val obj1 = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        val obj2 = JSONObject.build {
            add("field1", "def")
            add("field2", 456)
        }
        val json = JSONObject.build {
            add("lines", JSONArray.of(obj1, obj2))
        }
        val expected = TestPage(lines = listOf(Dummy1("abc", 123), Dummy1("def", 456)))
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize another form of custom parameterised type`() {
        val obj1 = JSONObject.build {
            add("field1", "abc")
            add("field2", 123)
        }
        val json = JSONObject.build {
            add("description", "testing")
            add("data", obj1)
        }
        val dummy1 = Dummy1("abc", 123)
        val expected = TestDataHolder("testing", dummy1)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize yet another form of custom parameterised type`() {
        val json = JSONObject.build {
            add("lineLists", JSONArray.of(JSONArray.of(JSONString("lineA1"), JSONString("lineA2")),
                    JSONArray.of(JSONString("lineB1"), JSONString("lineB2"))))
        }
        val expected = TestPage2(lineLists = listOf(listOf("lineA1", "lineA2"), listOf("lineB1", "lineB2")))
        expect(expected) { JSONDeserializer.deserialize(json)}
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

    @Test fun `should give error message with pointer`() {
        val json = JSON.parse("""{"field1":"abc","field2":"def"}""")
        val e1 = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<Dummy1>(json) }
        expect("Can't deserialize \"def\" as Int at /field2") { e1.message }
        val e2 = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<List<Dummy1>>(JSONArray.of(json)) }
        expect("Can't deserialize \"def\" as Int at /0/field2") { e2.message }
    }

    @Test fun `should give expanded error message with pointer`() {
        val json = JSON.parse("""{"field2":1}""")
        val e1 = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<Dummy1>(json) }
        expect("Can't create Dummy1; missing: field1") { e1.message }
        val e2 = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<List<Dummy1>>(JSONArray.of(json)) }
        expect("Can't create Dummy1; missing: field1 at /0") { e2.message }
    }

    @Test fun `should give expanded error message for multiple constructors`() {
        val json = JSON.parse("""[{"aaa":"X"},{"bbb":1},{"ccc":true,"ddd":0}]""")
        val e = assertFailsWith<JSONKotlinException> { JSONDeserializer.deserialize<List<MultiConstructor>>(json) }
        expect("Can't locate constructor for MultiConstructor; properties: ccc, ddd at /2") { e.message }
    }

    @Test fun `should use type projection upperBounds`() {
        val json = JSON.parse("""{"expr":{"class":"Const","number":20.0}}""")
        val expr = JSONDeserializer.deserialize<SealedClassContainer<*>>(json)?.expr
        assertTrue(expr is Const)
        expect(20.0) { expr.number }
    }

    private val calendarFields = arrayOf(Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
        Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, Calendar.ZONE_OFFSET)

    private fun calendarEquals(a: Calendar, b: Calendar): Boolean {
        for (field in calendarFields)
            if (a.get(field) != b.get(field))
                return false
        return true
    }

    data class TestPage<T>(val header: String? = null, val lines: List<T>)

    data class TestDataHolder<T>(val description: String, val data: T)

    data class TestPage2<T>(val header: String? = null, val lineLists: List<List<T>>)

}
