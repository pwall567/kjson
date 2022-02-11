/*
 * @(#) JSONSerializerFunctionsTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2020, 2022 Peter Wall
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect
import java.time.Instant
import java.util.Calendar
import io.kjson.JSONSerializerFunctions.appendCalendar
import io.kjson.JSONSerializerFunctions.findSealedClass
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.isToStringClass
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.NotANumber

class JSONSerializerFunctionsTest {

    @Test fun `should find toJSON when it is present`() {
        assertNotNull(DummyFromJSON::class.findToJSON())
    }

    @Test fun `should return null when toJSON is not present`() {
        assertNull(Dummy1::class.findToJSON())
    }

    @Test fun `should return parent when class is a subclass of a sealed class`() {
        expect("Expr") { NotANumber::class.findSealedClass()?.simpleName }
    }

    @Test fun `should return null when class is not a subclass of a sealed class`() {
        assertNull(Dummy1::class.findSealedClass())
    }

    @Test fun `should recognise a toString-able class`() {
        assertTrue(Instant::class.isToStringClass())
    }

    @Test fun `should recognise a not-toString-able class`() {
        assertFalse(Dummy1::class.isToStringClass())
    }

    @Test fun `should correctly format Calendar`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, 3)
            set(Calendar.DAY_OF_MONTH, 23)
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 25)
            set(Calendar.SECOND, 31)
            set(Calendar.MILLISECOND, 123)
            set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        }
        expect("2020-04-23T19:25:31.123+10:00") { StringBuilder().apply { appendCalendar(cal) }.toString() }
    }

    @Test fun `should not crash on reflection on system classes`() {
        val map: Map<String, String> = emptyMap()
        assertNull(map::class.findToJSON())
        val int = 1
        assertNull(int::class.findToJSON())
        val lambda: (Int) -> Int = { it }
        assertNull(lambda::class.findToJSON())
        val str = "???"
        assertNull(str::class.findToJSON())
    }

}
