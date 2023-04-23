/*
 * @(#) JSONDeserializerOptTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2023 Peter Wall
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
import kotlin.test.assertTrue
import kotlin.test.expect
import kotlin.test.fail
import io.kjson.optional.Opt
import io.kjson.testclasses.OptComplexData
import io.kjson.testclasses.OptData

class JSONDeserializerOptTest {

    @Test fun `should deserialize Opt`() {
        val json = JSONInt(123)
        val result = JSONDeserializer.deserialize<Opt<Int>>(json)
        assertNotNull(result)
        assertTrue(result.isSet)
        expect(123) { result.value }
    }

    @Test fun `should deserialize null Opt`() {
        val result = JSONDeserializer.deserialize<Opt<Int>>(null)
        assertNotNull(result)
        assertFalse(result.isSet)
    }

    @Test fun `should deserialize Opt property`() {
        val json = JSON.parse("""{"aaa":123}""") ?: fail()
        val result = JSONDeserializer.deserialize<OptData>(json)
        with(result) {
            assertNotNull(this)
            assertTrue(aaa.isSet)
            expect(123) { aaa.value }
        }
    }

    @Test fun `should deserialize missing Opt property`() {
        val json = JSONObject.EMPTY
        val result = JSONDeserializer.deserialize<OptData>(json)
        with(result) {
            assertNotNull(this)
            assertFalse(aaa.isSet)
        }
    }

    @Test fun `should deserialize parameterised Opt property`() {
        val json = JSON.parse("""{"aaa":["content"]}""") ?: fail()
        val result = JSONDeserializer.deserialize<OptComplexData>(json)
        with(result) {
            assertNotNull(this)
            assertTrue(aaa.isSet)
            expect(listOf("content")) { aaa.value }
        }
    }

    @Test fun `should deserialize missing parameterised Opt property`() {
        val json = JSON.parse("""{}""") ?: fail()
        val result = JSONDeserializer.deserialize<OptComplexData>(json)
        with(result) {
            assertNotNull(this)
            assertFalse(aaa.isSet)
        }
    }

}
