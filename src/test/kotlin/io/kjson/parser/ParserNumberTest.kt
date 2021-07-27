/*
 * @(#) ParserNumberTest.kt
 *
 * kjson  JSON functions for Kotlin
 * Copyright (c) 2021 Peter Wall
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

package io.kjson.parser

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.expect
import java.math.BigDecimal
import io.kjson.JSONDecimal
import io.kjson.JSONInt
import io.kjson.JSONLong

class ParserNumberTest {

    @Test fun `should parse zero`() {
        val result = Parser.parse("0")
        assertTrue(result is JSONInt)
        expect(0) { result.value }
    }

    @Test fun `should reject leading zeros`() {
        assertFailsWith<ParseException> { Parser.parse("00") }.let {
            expect(Parser.ILLEGAL_NUMBER) { it.text }
            expect(Parser.ILLEGAL_NUMBER) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
        assertFailsWith<ParseException> { Parser.parse("0123") }.let {
            expect(Parser.ILLEGAL_NUMBER) { it.text }
            expect(Parser.ILLEGAL_NUMBER) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
    }

    @Test fun `should reject incorrect numbers`() {
        assertFailsWith<ParseException> { Parser.parse("123a") }.let {
            expect(Parser.EXCESS_CHARS) { it.text }
            expect(Parser.EXCESS_CHARS) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
        assertFailsWith<ParseException> { Parser.parse("12:00") }.let {
            expect(Parser.EXCESS_CHARS) { it.text }
            expect(Parser.EXCESS_CHARS) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
        assertFailsWith<ParseException> { Parser.parse("1.23/4") }.let {
            expect(Parser.EXCESS_CHARS) { it.text }
            expect(Parser.EXCESS_CHARS) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
    }

    @Test fun `should parse positive integers`() {
        val result1 = Parser.parse("123")
        assertTrue(result1 is JSONInt)
        expect(123) { result1.value }
        val result2 = Parser.parse("5678900")
        assertTrue(result2 is JSONInt)
        expect(5678900) { result2.value }
        val result3 = Parser.parse("2147483647")
        assertTrue(result3 is JSONInt)
        expect(2147483647) { result3.value }
    }

    @Test fun `should parse negative integers`() {
        val result1 = Parser.parse("-1")
        assertTrue(result1 is JSONInt)
        expect(-1) { result1.value }
        val result2 = Parser.parse("-876543")
        assertTrue(result2 is JSONInt)
        expect(-876543) { result2.value }
        val result3 = Parser.parse("-2147483648")
        assertTrue(result3 is JSONInt)
        expect(-2147483648) { result3.value }
    }

    @Test fun `should parse positive long integers`() {
        val result1 = Parser.parse("1234567890000")
        assertTrue(result1 is JSONLong)
        expect(1234567890000) { result1.value }
        val result2 = Parser.parse("567895678956789")
        assertTrue(result2 is JSONLong)
        expect(567895678956789) { result2.value }
        val result3 = Parser.parse("2147483648")
        assertTrue(result3 is JSONLong)
        expect(2147483648) { result3.value }
        val result4 = Parser.parse("9223372036854775807")
        assertTrue(result4 is JSONLong)
        expect(9223372036854775807) { result4.value }
    }

    @Test fun `should parse negative long integers`() {
        val result1 = Parser.parse("-1234567890000")
        assertTrue(result1 is JSONLong)
        expect(-1234567890000) { result1.value }
        val result2 = Parser.parse("-567895678956789")
        assertTrue(result2 is JSONLong)
        expect(-567895678956789) { result2.value }
        val result3 = Parser.parse("-2147483649")
        assertTrue(result3 is JSONLong)
        expect(-2147483649) { result3.value }
        val result4 = Parser.parse("-9223372036854775808")
        assertTrue(result4 is JSONLong)
        expect(-9223372036854775807 - 1) { result4.value }
    }

    @Test fun `should parse decimal`() {
        val result1 = Parser.parse("0.0")
        assertTrue(result1 is JSONDecimal)
        expect(0) { result1.value.compareTo(BigDecimal.ZERO) }
        val result2 = Parser.parse("0.00")
        assertTrue(result2 is JSONDecimal)
        expect(0) { result2.value.compareTo(BigDecimal.ZERO) }
    }

    @Test fun `should parse positive decimal`() {
        val result1 = Parser.parse("12340.0")
        assertTrue(result1 is JSONDecimal)
        expect(0) { result1.value.compareTo(BigDecimal("12340")) }
        val result2 = Parser.parse("1e200")
        assertTrue(result2 is JSONDecimal)
        expect(0) { result2.value.compareTo(BigDecimal("1e200")) }
        val result3 = Parser.parse("27e-60")
        assertTrue(result3 is JSONDecimal)
        expect(0) { result3.value.compareTo(BigDecimal("27e-60")) }
        val result4 = Parser.parse("0.1e-48")
        assertTrue(result4 is JSONDecimal)
        expect(0) { result4.value.compareTo(BigDecimal("0.1e-48")) }
        val result5 = Parser.parse("9223372036854775808")
        assertTrue(result5 is JSONDecimal)
        expect(0) { result5.value.compareTo(BigDecimal("9223372036854775808")) }
    }

    @Test fun `should parse negative decimal`() {
        val result1 = Parser.parse("-12340.0")
        assertTrue(result1 is JSONDecimal)
        expect(0) { result1.value.compareTo(BigDecimal("-12340")) }
        val result2 = Parser.parse("-1e200")
        assertTrue(result2 is JSONDecimal)
        expect(0) { result2.value.compareTo(BigDecimal("-1e200")) }
        val result3 = Parser.parse("-27e-60")
        assertTrue(result3 is JSONDecimal)
        expect(0) { result3.value.compareTo(BigDecimal("-27e-60")) }
        val result4 = Parser.parse("-0.1e-48")
        assertTrue(result4 is JSONDecimal)
        expect(0) { result4.value.compareTo(BigDecimal("-0.1e-48")) }
        val result5 = Parser.parse("-9223372036854775809")
        assertTrue(result5 is JSONDecimal)
        expect(0) { result5.value.compareTo(BigDecimal("-9223372036854775809")) }
    }

}
