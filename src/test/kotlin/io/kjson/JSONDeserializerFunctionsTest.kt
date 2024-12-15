/*
 * @(#) JSONDeserializerFunctionsTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2024 Peter Wall
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

import java.util.Calendar
import java.util.TimeZone

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldThrow

import io.kjson.JSONDeserializerFunctions.parseCalendar

class JSONDeserializerFunctionsTest {

    @Test fun `should parse Calendar correctly`() {
        with(parseCalendar("2021-08-16")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe false
            isSet(Calendar.MINUTE) shouldBe false
            isSet(Calendar.SECOND) shouldBe false
            isSet(Calendar.MILLISECOND) shouldBe false
            isSet(Calendar.ZONE_OFFSET) shouldBe false
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
        }
        with(parseCalendar("2021-08-16T18:48:30")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe false
            isSet(Calendar.ZONE_OFFSET) shouldBe false
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
        }
        with(parseCalendar("2021-08-16T18:48:30.5")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe true
            isSet(Calendar.ZONE_OFFSET) shouldBe false
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.MILLISECOND) shouldBe 500
        }
        with(parseCalendar("2021-08-16T18:48:30.56")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe true
            isSet(Calendar.ZONE_OFFSET) shouldBe false
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.MILLISECOND) shouldBe 560
        }
        with(parseCalendar("2021-08-16T18:48:30.567")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe true
            isSet(Calendar.ZONE_OFFSET) shouldBe false
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.MILLISECOND) shouldBe 567
        }
        with(parseCalendar("2021-08-16T18:48:30.5678")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe true
            isSet(Calendar.ZONE_OFFSET) shouldBe false
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.MILLISECOND) shouldBe 567
        }
        with(parseCalendar("2021-08-16T18:48:30.567Z")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe true
            isSet(Calendar.ZONE_OFFSET) shouldBe true
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.MILLISECOND) shouldBe 567
            get(Calendar.ZONE_OFFSET) shouldBe 0
            timeZone shouldBe TimeZone.getTimeZone("GMT")
        }
        with(parseCalendar("2021-08-16T18:48:30.567+10")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe true
            isSet(Calendar.ZONE_OFFSET) shouldBe true
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.MILLISECOND) shouldBe 567
            get(Calendar.ZONE_OFFSET) shouldBe 10 * 60 * 60 * 1000
            timeZone shouldBe TimeZone.getTimeZone("GMT+10:00")
        }
        with(parseCalendar("2021-08-16T18:48:30-5")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe false
            isSet(Calendar.ZONE_OFFSET) shouldBe true
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.ZONE_OFFSET) shouldBe -5 * 60 * 60 * 1000
            timeZone shouldBe TimeZone.getTimeZone("GMT-05:00")
        }
        with(parseCalendar("2021-08-16T18:48:30+5:30")) {
            isSet(Calendar.YEAR) shouldBe true
            isSet(Calendar.MONTH) shouldBe true
            isSet(Calendar.DAY_OF_MONTH) shouldBe true
            isSet(Calendar.HOUR_OF_DAY) shouldBe true
            isSet(Calendar.MINUTE) shouldBe true
            isSet(Calendar.SECOND) shouldBe true
            isSet(Calendar.MILLISECOND) shouldBe false
            isSet(Calendar.ZONE_OFFSET) shouldBe true
            get(Calendar.YEAR) shouldBe 2021
            get(Calendar.MONTH) shouldBe 7
            get(Calendar.DAY_OF_MONTH) shouldBe 16
            get(Calendar.HOUR_OF_DAY) shouldBe 18
            get(Calendar.MINUTE) shouldBe 48
            get(Calendar.SECOND) shouldBe 30
            get(Calendar.ZONE_OFFSET) shouldBe (5 * 60 + 30) * 60 * 1000
            timeZone shouldBe TimeZone.getTimeZone("GMT+05:30")
        }
    }

    @Test fun `should report error on incorrect Calendar`() {
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 4 - 2021") {
            parseCalendar("2021")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 4 - 2021/08/16") {
            parseCalendar("2021/08/16")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 7 - 2021-16-08") {
            parseCalendar("2021-16-08")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 10 - 2021-02-30") {
            parseCalendar("2021-02-30")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 10 - 2021-02-14 12:30") {
            parseCalendar("2021-02-14 12:30")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 11 - 2021-02-14T") {
            parseCalendar("2021-02-14T")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 16 - 2021-08-16T18:46") {
            parseCalendar("2021-08-16T18:46")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 13 - 2021-02-14T32:30:00") {
            parseCalendar("2021-02-14T32:30:00")
        }
        shouldThrow<IllegalArgumentException>("Error in calendar string at offset 16 - 2021-02-14T12:88:00") {
            parseCalendar("2021-02-14T12:88:00")
        }
    }

}
