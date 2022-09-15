/*
 * @(#) JSONDeserializerStringTest.kt
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

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.time.Duration.Companion.hours

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
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID

import io.kjson.testclasses.DummyEnum

class JSONDeserializerStringTest {

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

    @Test fun `should return Calendar from JSONString`() {
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
        val expected = Duration.ofHours(2)
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
            expect("Error deserializing \"b082b046-ac9b-11eb-8ea7-5fc81989f1\" as java.util.UUID") { it.message }
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

    @Test fun `should return BigDecimal from JSONString`() {
        val str = "123456789.77777"
        val json = JSONString(str)
        val expected = BigDecimal(str)
        expect(expected) { JSONDeserializer.deserialize(json) }
    }

    @Test fun `should deserialize JSONString to Any`() {
        val json = JSONString("Hello!")
        expect("Hello!") { JSONDeserializer.deserializeAny(json) }
    }

    companion object {

        private val calendarFields = arrayOf(Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
                Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, Calendar.ZONE_OFFSET)

        private fun calendarEquals(a: Calendar, b: Calendar): Boolean {
            for (field in calendarFields)
                if (a.get(field) != b.get(field))
                    return false
            return true
        }

    }

}
