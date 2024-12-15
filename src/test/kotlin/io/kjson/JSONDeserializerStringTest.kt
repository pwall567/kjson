/*
 * @(#) JSONDeserializerStringTest.kt
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
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldThrow

import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.DummyEnum

class JSONDeserializerStringTest {

    @Test fun `should return string from JSONString`() {
        val json = JSONString("abc")
        val expected = "abc"
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return character from single character JSONString`() {
        val json = JSONString("Q")
        val expected = 'Q'
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return character array from JSONString`() {
        val json = JSONString("abcdef")
        val expected = arrayOf('a', 'b', 'c', 'd', 'e', 'f').toCharArray()
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return array of Char from JSONString`() {
        val json = JSONString("abcdef")
        val expected: Array<Char> = arrayOf('a', 'b', 'c', 'd', 'e', 'f')
        expected shouldBe JSONDeserializer.deserialize(Array<Char>::class, json)!!
    }

    @Test fun `should return Calendar from JSONString`() {
        val json = JSONString("2019-04-19T15:34:02.234+10:00")
        val cal = Calendar.getInstance()
        cal.set(2019, 3, 19, 15, 34, 2) // month value is month - 1
        cal.set(Calendar.MILLISECOND, 234)
        cal.set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        calendarEquals(cal, JSONDeserializer.deserialize(json)) shouldBe true
    }

    @Test fun `should return Date from JSONString`() {
        val json = JSONString("2019-03-10T15:34:02.234+11:00")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        cal.set(2019, 2, 10, 15, 34, 2) // month value is month - 1
        cal.set(Calendar.MILLISECOND, 234)
        cal.set(Calendar.ZONE_OFFSET, 11 * 60 * 60 * 1000)
        val expected: Date? = cal.time
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return java-sql-Date from JSONString`() {
        val json = JSONString("2019-03-10")
        val expected: java.sql.Date? = java.sql.Date.valueOf("2019-03-10")
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return java-sql-Time from JSONString`() {
        val json = JSONString("22:45:41")
        val expected: java.sql.Time? = java.sql.Time.valueOf("22:45:41")
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return java-sql-Timestamp from JSONString`() {
        val json = JSONString("2019-03-10 22:45:41.5")
        val expected: java.sql.Timestamp? = java.sql.Timestamp.valueOf("2019-03-10 22:45:41.5")
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Instant from JSONString`() {
        val json = JSONString("2019-03-10T15:34:02.234Z")
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("GMT")
        cal.set(2019, 2, 10, 15, 34, 2) // month value is month - 1
        cal.set(Calendar.MILLISECOND, 234)
        cal.set(Calendar.ZONE_OFFSET, 0)
        val expected = Instant.ofEpochMilli(cal.timeInMillis)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return LocalDate from JSONString`() {
        val json = JSONString("2019-03-10")
        val expected = LocalDate.of(2019, 3, 10)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return LocalDateTime from JSONString`() {
        val json = JSONString("2019-03-10T16:43:33")
        val expected = LocalDateTime.of(2019, 3, 10, 16, 43, 33)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return LocalTime from JSONString`() {
        val json = JSONString("16:43:33")
        val expected = LocalTime.of(16, 43, 33)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return OffsetTime from JSONString`() {
        val json = JSONString("16:46:11.234+10:00")
        val expected = OffsetTime.of(16, 46, 11, 234000000, ZoneOffset.ofHours(10))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return OffsetDateTime from JSONString`() {
        val json = JSONString("2019-03-10T16:46:11.234+10:00")
        val expected = OffsetDateTime.of(2019, 3, 10, 16, 46, 11, 234000000, ZoneOffset.ofHours(10))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return ZonedDateTime from JSONString`() {
        val json = JSONString("2019-01-10T16:46:11.234+11:00[Australia/Sydney]")
        val expected = ZonedDateTime.of(2019, 1, 10, 16, 46, 11, 234000000, ZoneId.of("Australia/Sydney"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Year from JSONString`() {
        val json = JSONString("2019")
        val expected = Year.of(2019)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return YearMonth from JSONString`() {
        val json = JSONString("2019-03")
        val expected = YearMonth.of(2019, 3)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return MonthDay from JSONString`() {
        val json = JSONString("--03-10")
        val expected = MonthDay.of(3, 10)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Java Duration from JSONString`() {
        val json = JSONString("PT2H")
        val expected = JavaDuration.ofHours(2)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Period from JSONString`() {
        val json = JSONString("P3M")
        val expected = Period.ofMonths(3)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Duration from JSONString`() {
        val json = JSONString("PT2H")
        val expected = 2.hours
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UUID from JSONString`() {
        val uuid = "b082b046-ac9b-11eb-8ea7-5fc81989f104"
        val json = JSONString(uuid)
        val expected = UUID.fromString("b082b046-ac9b-11eb-8ea7-5fc81989f104")
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should fail on invalid UUID`() {
        val json = JSONString("b082b046-ac9b-11eb-8ea7-5fc81989f1") // 2 bytes too short
        shouldThrow<JSONException>(
            message = "Error deserializing java.util.UUID - Not a valid UUID - b082b046-ac9b-11eb-8ea7-5fc81989f1",
        ) {
            JSONDeserializer.deserialize<UUID>(json)
        }
    }

    @Test fun `should return URI from JSONString`() {
        val uriString = "http://kjson.io"
        val json = JSONString(uriString)
        val expected = URI(uriString)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return URL from JSONString`() {
        val urlString = "https://kjson.io"
        val json = JSONString(urlString)
        val expected = URL(urlString)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return enum from JSONString`() {
        val json = JSONString("ALPHA")
        val expected = DummyEnum.ALPHA
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return BigInteger from JSONString`() {
        val str = "123456789"
        val json = JSONString(str)
        val expected = BigInteger(str)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return BigDecimal from JSONString`() {
        val str = "123456789.77777"
        val json = JSONString(str)
        val expected = BigDecimal(str)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize JSONString to Any`() {
        val json = JSONString("Hello!")
        json.deserializeAny() shouldBe "Hello!"
    }

    @Test fun `should use constructor with additional parameters defaulted`() {
        val json = JSONString("Hello!")
        val result: Dummy1 = json.deserialize()
        result.field1 shouldBe "Hello!"
        result.field2 shouldBe 999
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
