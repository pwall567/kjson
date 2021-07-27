/*
 * @(#) JSONValueTest.kt
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

package io.kjson

import kotlin.test.Test
import kotlin.test.expect
import java.math.BigDecimal

class JSONValueTest {

    @Test fun `should create JSONInt`() {
        val test1 = JSONInt(12345)
        expect(12345) { test1.value }
        expect("12345") { test1.toJSON() }
    }

    @Test fun `should create JSONLong`() {
        val test1 = JSONLong(12345)
        expect(12345) { test1.value }
        expect("12345") { test1.toJSON() }
    }

    @Test fun `should create JSONDecimal`() {
        val testDecimal1 = JSONDecimal(BigDecimal.ZERO)
        expect(BigDecimal.ZERO) { testDecimal1.value }
        expect("0") { testDecimal1.toJSON() }
        val testDecimal2 = JSONDecimal("0.00")
        expect(testDecimal1) { testDecimal2 }
    }

    @Test fun `should use JSONBoolean`() {
        val testBoolean1 = JSONBoolean.TRUE
        expect(true) { testBoolean1.value }
        expect("true") { testBoolean1.toJSON() }
        val testBoolean2 = JSONBoolean.FALSE
        expect(false) { testBoolean2.value }
        expect("false") { testBoolean2.toJSON() }
    }

    @Test fun `should create JSONString`() {
        val testString = JSONString("ab\u2014c\n")
        expect("ab\u2014c\n") { testString.value }
        expect("\"ab\\u2014c\\n\"") { testString.toJSON() }
    }

    @Test fun `should create JSONArray`() {
        val testArray = JSONArray(arrayOf(JSONInt(123), JSONInt(456)), 2)
        expect(2) { testArray.size }
        expect("[123,456]") { testArray.toJSON() }
    }

    @Test fun `should use nullable functions`() {
        var testNull: JSONValue? = null
        expect("null") { testNull.toJSON() }
        val sb = StringBuilder()
        testNull.appendTo(sb)
        expect("null") { sb.toString() }
        testNull = createJSONValue()
        expect("222") { testNull.toJSON() }
        sb.setLength(0)
        testNull.appendTo(sb)
    }

    @Suppress("RedundantNullableReturnType")
    private fun createJSONValue(): JSONValue? {
        return JSONInt(222)
    }

}
