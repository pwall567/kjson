/*
 * @(#) ParserObjectTest.kt
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
import io.kjson.JSONArray
import io.kjson.JSONInt
import io.kjson.JSONObject
import io.kjson.JSONString

class ParserObjectTest {

    @Test fun `should parse empty object`() {
        val result = Parser.parse("{}")
        assertTrue(result is JSONObject)
        expect(0) { result.size }
    }

    @Test fun `should parse simple object`() {
        val result = Parser.parse("""{"first":123,"second":"Hi there!"}""")
        assertTrue(result is JSONObject)
        expect(2) { result.size }
        val first = result["first"]
        assertTrue(first is JSONInt)
        expect(123) { first.value }
        val second = result["second"]
        assertTrue(second is JSONString)
        expect("Hi there!") { second.value }
    }

    @Test fun `should parse nested object`() {
        val result = Parser.parse("""{"first":123,"second":{"a":[{"aa":0}]}}""")
        assertTrue(result is JSONObject)
        expect(2) { result.size }
        val first = result["first"]
        assertTrue(first is JSONInt)
        expect(123) { first.value }
        val second = result["second"]
        assertTrue(second is JSONObject)
        expect(1) { second.size }
        val a = second["a"]
        assertTrue(a is JSONArray)
        expect(1) { a.size }
        val item = a[0]
        assertTrue(item is JSONObject)
        expect(1) { item.size }
        val aa = item["aa"]
        assertTrue(aa is JSONInt)
        expect(0) { aa.value }
    }

    @Test fun `should throw exception on missing closing brace`() {
        assertFailsWith<ParseException> { Parser.parse("""{"first":123""")}.let {
            expect(Parser.MISSING_CLOSING_BRACE) { it.text }
            expect(Parser.MISSING_CLOSING_BRACE) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
    }

    @Test fun `should throw exception on missing colon`() {
        assertFailsWith<ParseException> { Parser.parse("""{"first"123}""")}.let {
            expect(Parser.MISSING_COLON) { it.text }
            expect(Parser.MISSING_COLON) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
    }

    @Test fun `should throw exception on missing quotes`() {
        assertFailsWith<ParseException> { Parser.parse("""{first:123}""")}.let {
            expect(Parser.ILLEGAL_KEY) { it.text }
            expect(Parser.ILLEGAL_KEY) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
    }

    @Test fun `should throw exception on duplicate keys`() {
        assertFailsWith<ParseException> { Parser.parse("""{"first":123,"first":456}""")}.let {
            expect(Parser.DUPLICATE_KEY) { it.text }
            expect(Parser.DUPLICATE_KEY) { it.message }
            expect(Parser.rootPointer) { it.pointer }
        }
    }

}
