/*
 * @(#) JSONSerializerFunctionsTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2020, 2022, 2024 Peter Wall
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

import java.net.URL
import java.util.UUID

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeNonNull

import io.kjson.JSONSerializerFunctions.appendUUID
import io.kjson.JSONSerializerFunctions.findToJSON
import io.kjson.JSONSerializerFunctions.isToStringClass
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.DummyFromJSON
import io.kjson.testclasses.NotANumber
import io.kjson.util.findSealedClass

class JSONSerializerFunctionsTest {

    @Test fun `should find toJSON when it is present`() {
        DummyFromJSON::class.findToJSON().shouldBeNonNull()
    }

    @Test fun `should return null when toJSON is not present`() {
        Dummy1::class.findToJSON() shouldBe null
    }

    @Test fun `should return parent when class is a subclass of a sealed class`() {
        NotANumber::class.findSealedClass()?.simpleName shouldBe "Expr"
    }

    @Test fun `should return null when class is not a subclass of a sealed class`() {
        Dummy1::class.findSealedClass() shouldBe null
    }

    @Test fun `should recognise a toString-able class`() {
        URL::class.isToStringClass() shouldBe true
    }

    @Test fun `should recognise a not-toString-able class`() {
        Dummy1::class.isToStringClass() shouldBe false
    }

    @Test fun `should not crash on reflection on system classes`() {
        val map: Map<String, String> = emptyMap()
        map::class.findToJSON() shouldBe null
        val int = 1
        int::class.findToJSON() shouldBe null
        val lambda: (Int) -> Int = { it }
        lambda::class.findToJSON() shouldBe null
        val str = "???"
        str::class.findToJSON() shouldBe null
    }

    @Test fun `should serialize UUID correctly`() {
        val string = "233decc2-b894-11ec-a686-d7d058bdeb9b"
        val uuid = UUID.fromString(string)
        val sb = StringBuilder()
        sb.appendUUID(uuid)
        sb.toString() shouldBe string
    }

}
