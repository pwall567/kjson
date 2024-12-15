/*
 * @(#) JSONDeserializerDecimalTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2022, 2024 Peter Wall
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

import java.math.BigDecimal
import java.math.BigInteger

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldBeEqual

import io.kjson.JSON.asLong
import io.kjson.testclasses.ConstructBigDecimal
import io.kjson.testclasses.ConstructBigInteger
import io.kjson.testclasses.ConstructByte
import io.kjson.testclasses.ConstructDouble
import io.kjson.testclasses.ConstructFloat
import io.kjson.testclasses.ConstructInt
import io.kjson.testclasses.ConstructLong
import io.kjson.testclasses.ConstructShort
import io.kjson.testclasses.ConstructUByte
import io.kjson.testclasses.ConstructUInt
import io.kjson.testclasses.ConstructULong
import io.kjson.testclasses.ConstructUShort

class JSONDeserializerDecimalTest {

    @Test fun `should return Int from JSONDecimal`() {
        val json = JSONDecimal(1234)
        val expected = 1234
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Long from JSONDecimal`() {
        val json = JSONDecimal(1234)
        val expected: Long = 1234
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Short from JSONDecimal`() {
        val json = JSONDecimal(1234)
        val expected: Short = 1234
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Byte from JSONDecimal`() {
        val json = JSONDecimal(123)
        val expected: Byte = 123
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Double from JSONDecimal`() {
        val json = JSONDecimal("123.0")
        val expected = 123.0
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Float from JSONDecimal`() {
        val json = JSONDecimal("123.0")
        val expected = 123.0F
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return BigInteger from JSONDecimal`() {
        val json = JSONDecimal("123456789.00")
        val expected: BigInteger? = BigInteger.valueOf(json.asLong)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return BigDecimal from JSONDecimal`() {
        val str = "123456789.77777"
        val json = JSONDecimal(str)
        val expected = BigDecimal(str)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return ULong from JSONDecimal`() {
        val json = JSONDecimal("9223372036854775808")
        val expected = 9223372036854775808U // Long.MAX_VALUE + 1
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UInt from JSONDecimal`() {
        val json = JSONDecimal("2147483648")
        val expected = 2147483648U // Int.MAX_VALUE + 1
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UShort from JSONDecimal`() {
        val json = JSONDecimal(32768)
        val expected: UShort = 32768U
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UByte from JSONDecimal`() {
        val json = JSONDecimal(128)
        val expected: UByte = 128U
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Long constructor from JSONDecimal`() {
        val json = JSONDecimal(1234567)
        val expected = ConstructLong(1234567)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Int constructor from JSONDecimal`() {
        val json = JSONDecimal(1234567)
        val expected = ConstructInt(1234567)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Short constructor from JSONDecimal`() {
        val json = JSONDecimal(12345)
        val expected = ConstructShort(12345)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Byte constructor from JSONDecimal`() {
        val json = JSONDecimal(123)
        val expected = ConstructByte(123)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with ULong constructor from JSONDecimal`() {
        val json = JSONDecimal(1234567)
        val expected = ConstructULong(1234567U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with UInt constructor from JSONDecimal`() {
        val json = JSONDecimal(1234567)
        val expected = ConstructUInt(1234567U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with UShort constructor from JSONDecimal`() {
        val json = JSONDecimal(12345)
        val expected = ConstructUShort(12345U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with UByte constructor from JSONDecimal`() {
        val json = JSONDecimal(123)
        val expected = ConstructUByte(123U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Double constructor from JSONDecimal`() {
        val json = JSONDecimal("12345.67")
        val expected = ConstructDouble(12345.67)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Float constructor from JSONDecimal`() {
        val json = JSONDecimal("1234.5")
        val expected = ConstructFloat(1234.5F)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with BigDecimal constructor from JSONDecimal`() {
        val json = JSONDecimal("-87654.321")
        val expected = ConstructBigDecimal(BigDecimal("-87654.321"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with BigInteger constructor from JSONDecimal`() {
        val json = JSONDecimal("-87654.000")
        val expected = ConstructBigInteger(BigInteger("-87654"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize JSONDecimal to Any`() {
        val json = JSONDecimal("12345.67")
        json.deserializeAny() shouldBe BigDecimal("12345.67")
    }

}
