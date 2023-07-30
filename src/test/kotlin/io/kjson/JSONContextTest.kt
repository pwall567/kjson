/*
 * @(#) JSONContextTest.kt
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
import kotlin.test.assertSame
import kotlin.test.expect

import io.kjson.pointer.JSONPointer

class JSONContextTest {

    @Test fun `should create JSONContext with config only`() {
        val config = JSONConfig()
        val context = JSONContext(config)
        assertSame(config, context.config)
        assertSame(JSONPointer.root, context.pointer)
    }

    @Test fun `should create JSONContext with config and pointer`() {
        val config = JSONConfig()
        val pointer = JSONPointer("/abc/def")
        val context = JSONContext(config, pointer)
        assertSame(config, context.config)
        expect(pointer) { context.pointer }
    }

    @Test fun `should create JSONContext with pointer only`() {
        val pointer = JSONPointer("/abc/def")
        val context = JSONContext(pointer)
        assertSame(JSONConfig.defaultConfig, context.config)
        expect(pointer) { context.pointer }
    }

    @Test fun `should create child JSONContext with property name`() {
        val config = JSONConfig()
        val context = JSONContext(config, JSONPointer("/abc/def"))
        val child = context.child("xyz")
        assertSame(config, child.config)
        expect(JSONPointer("/abc/def/xyz")) { child.pointer }
    }

    @Test fun `should create child JSONContext with array index`() {
        val config = JSONConfig()
        val context = JSONContext(config, JSONPointer("/abc/def"))
        val child = context.child(1)
        assertSame(config, child.config)
        expect(JSONPointer("/abc/def/1")) { child.pointer }
    }

}
