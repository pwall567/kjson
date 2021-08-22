/*
 * @(#) JSONDeserializerFunctionsTest.kt
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

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.expect

import java.util.Calendar
import java.util.TimeZone

import io.kjson.JSONDeserializerFunctions.parseCalendar

class JSONDeserializerFunctionsTest {

    @Test fun `should parse Calendar correctly`() {
        with(parseCalendar("2021-08-16")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertFalse(isSet(Calendar.HOUR_OF_DAY))
            assertFalse(isSet(Calendar.MINUTE))
            assertFalse(isSet(Calendar.SECOND))
            assertFalse(isSet(Calendar.MILLISECOND))
            assertFalse(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
        }
        with(parseCalendar("2021-08-16T18:48:30")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertFalse(isSet(Calendar.MILLISECOND))
            assertFalse(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
        }
        with(parseCalendar("2021-08-16T18:48:30.5")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertTrue(isSet(Calendar.MILLISECOND))
            assertFalse(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(500) { get(Calendar.MILLISECOND) }
        }
        with(parseCalendar("2021-08-16T18:48:30.56")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertTrue(isSet(Calendar.MILLISECOND))
            assertFalse(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(560) { get(Calendar.MILLISECOND) }
        }
        with(parseCalendar("2021-08-16T18:48:30.567")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertTrue(isSet(Calendar.MILLISECOND))
            assertFalse(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(567) { get(Calendar.MILLISECOND) }
        }
        with(parseCalendar("2021-08-16T18:48:30.5678")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertTrue(isSet(Calendar.MILLISECOND))
            assertFalse(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(567) { get(Calendar.MILLISECOND) }
        }
        with(parseCalendar("2021-08-16T18:48:30.567Z")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertTrue(isSet(Calendar.MILLISECOND))
            assertTrue(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(567) { get(Calendar.MILLISECOND) }
            expect(0) { get(Calendar.ZONE_OFFSET) }
            expect(TimeZone.getTimeZone("GMT")) { timeZone }
        }
        with(parseCalendar("2021-08-16T18:48:30.567+10")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertTrue(isSet(Calendar.MILLISECOND))
            assertTrue(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(567) { get(Calendar.MILLISECOND) }
            expect(10 * 60 * 60 * 1000) { get(Calendar.ZONE_OFFSET) }
            expect(TimeZone.getTimeZone("GMT+10:00")) { timeZone }
        }
        with(parseCalendar("2021-08-16T18:48:30-5")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertFalse(isSet(Calendar.MILLISECOND))
            assertTrue(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect(-5 * 60 * 60 * 1000) { get(Calendar.ZONE_OFFSET) }
            expect(TimeZone.getTimeZone("GMT-05:00")) { timeZone }
        }
        with(parseCalendar("2021-08-16T18:48:30+5:30")) {
            assertTrue(isSet(Calendar.YEAR))
            assertTrue(isSet(Calendar.MONTH))
            assertTrue(isSet(Calendar.DAY_OF_MONTH))
            assertTrue(isSet(Calendar.HOUR_OF_DAY))
            assertTrue(isSet(Calendar.MINUTE))
            assertTrue(isSet(Calendar.SECOND))
            assertFalse(isSet(Calendar.MILLISECOND))
            assertTrue(isSet(Calendar.ZONE_OFFSET))
            expect(2021) { get(Calendar.YEAR) }
            expect(7) { get(Calendar.MONTH) }
            expect(16) { get(Calendar.DAY_OF_MONTH) }
            expect(18) { get(Calendar.HOUR_OF_DAY) }
            expect(48) { get(Calendar.MINUTE) }
            expect(30) { get(Calendar.SECOND) }
            expect((5 * 60 + 30) * 60 * 1000) { get(Calendar.ZONE_OFFSET) }
            expect(TimeZone.getTimeZone("GMT+05:30")) { timeZone }
        }
    }

    @Test fun `should report error on incorrect Calendar`() {
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021") }.let {
            expect("Error in calendar string at offset 4 - 2021") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021/08/16") }.let {
            expect("Error in calendar string at offset 4 - 2021/08/16") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-16-08") }.let {
            expect("Error in calendar string at offset 7 - 2021-16-08") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-02-30") }.let {
            expect("Error in calendar string at offset 10 - 2021-02-30") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-02-14 12:30") }.let {
            expect("Error in calendar string at offset 10 - 2021-02-14 12:30") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-02-14T") }.let {
            expect("Error in calendar string at offset 11 - 2021-02-14T") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-08-16T18:46") }.let {
            expect("Error in calendar string at offset 16 - 2021-08-16T18:46") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-02-14T32:30:00") }.let {
            expect("Error in calendar string at offset 13 - 2021-02-14T32:30:00") { it.message }
        }
        assertFailsWith<IllegalArgumentException> { parseCalendar("2021-02-14T12:88:00") }.let {
            expect("Error in calendar string at offset 16 - 2021-02-14T12:88:00") { it.message }
        }
    }

}
