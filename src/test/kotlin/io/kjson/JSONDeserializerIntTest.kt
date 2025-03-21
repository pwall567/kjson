/*
 * @(#) JSONDeserializerIntTest.kt
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

class JSONDeserializerIntTest {

    @Test fun `should return Int from JSONInt`() {
        val json = JSONInt(1234)
        val expected = 1234
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Long from JSONInt`() {
        val json = JSONInt(1234)
        val expected: Long = 1234
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Short from JSONInt`() {
        val json = JSONInt(1234)
        val expected: Short = 1234
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Byte from JSONInt`() {
        val json = JSONInt(123)
        val expected: Byte = 123
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Double from JSONInt`() {
        val json = JSONInt(123)
        val expected = 123.0
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return Float from JSONInt`() {
        val json = JSONInt(123)
        val expected = 123.0F
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return BigInteger from JSONInt`() {
        val value = 12345678
        val json = JSONInt(value)
        val expected: BigInteger? = BigInteger.valueOf(value.toLong())
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return BigDecimal from JSONInt`() {
        val json = JSONInt(1234)
        val expected = BigDecimal(1234)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return ULong from JSONInt`() {
        val json = JSONInt(1234567)
        val expected: ULong = 1234567U
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UInt from JSONInt`() {
        val json = JSONInt(1234567)
        val expected = 1234567U
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UShort from JSONInt`() {
        val json = JSONInt(40000)
        val expected: UShort = 40000U
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return UByte from JSONInt`() {
        val json = JSONInt(200)
        val expected: UByte = 200U
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Long constructor from JSONInt`() {
        val json = JSONInt(1234567)
        val expected = ConstructLong(1234567)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Int constructor from JSONInt`() {
        val json = JSONInt(1234567)
        val expected = ConstructInt(1234567)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Short constructor from JSONInt`() {
        val json = JSONInt(12345)
        val expected = ConstructShort(12345)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Byte constructor from JSONInt`() {
        val json = JSONInt(123)
        val expected = ConstructByte(123)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with ULong constructor from JSONInt`() {
        val json = JSONInt(1234567)
        val expected = ConstructULong(1234567U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with UInt constructor from JSONInt`() {
        val json = JSONInt(1234567)
        val expected = ConstructUInt(1234567U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with UShort constructor from JSONInt`() {
        val json = JSONInt(12345)
        val expected = ConstructUShort(12345U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with UByte constructor from JSONInt`() {
        val json = JSONInt(123)
        val expected = ConstructUByte(123U)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Double constructor from JSONInt`() {
        val json = JSONInt(12345678)
        val expected = ConstructDouble(12345678.toDouble())
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with Float constructor from JSONInt`() {
        val json = JSONInt(12345678)
        val expected = ConstructFloat(12345678.toFloat())
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with BigDecimal constructor from JSONInt`() {
        val json = JSONInt(12345678)
        val expected = ConstructBigDecimal(BigDecimal("12345678"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should return class with BigInteger constructor from JSONInt`() {
        val json = JSONInt(1234567)
        val expected = ConstructBigInteger(BigInteger("1234567"))
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should deserialize JSONInt to Any`() {
        val json = JSONInt(123456)
        json.deserializeAny() shouldBe 123456
    }

}
