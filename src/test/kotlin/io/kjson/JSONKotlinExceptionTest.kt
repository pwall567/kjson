/*
 * @(#) JSONKotlinExceptionTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2019, 2020, 2021, 2024 Peter Wall
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

import io.kstuff.test.shouldBe
import io.kstuff.test.shouldThrow

import io.kjson.JSONKotlinException.Companion.fatal
import io.kjson.pointer.JSONPointer

class JSONKotlinExceptionTest {

    @Test fun `should create simple exception`() {
        val e = JSONKotlinException("Test message")
        e.text shouldBe "Test message"
        e.message shouldBe "Test message"
        e.pointer shouldBe null
        e.cause shouldBe null
    }

    @Test fun `should create exception with root pointer`() {
        val e = JSONKotlinException("Test message", JSONPointer.root)
        e.text shouldBe "Test message"
        e.message shouldBe "Test message"
        e.pointer shouldBe JSONPointer.root
        e.cause shouldBe null
    }

    @Test fun `should create exception with pointer`() {
        val e = JSONKotlinException("Test message", JSONPointer.root.child(0).child("ace"))
        e.text shouldBe "Test message"
        e.message shouldBe "Test message, at /0/ace"
        e.pointer shouldBe JSONPointer("/0/ace")
        e.cause shouldBe null
    }

    @Test fun `should create exception with cause`() {
        val nested = JSONException("Nested")
        val e = JSONKotlinException("Test message", cause = nested)
        e.text shouldBe "Test message"
        e.message shouldBe "Test message"
        e.pointer shouldBe null
        e.cause shouldBe nested
    }

    @Test fun `should create exception with pointer and cause`() {
        val nested = JSONException("Nested")
        val e = JSONKotlinException("Test message", JSONPointer.root.child(0).child("ace"), nested)
        e.text shouldBe "Test message"
        e.message shouldBe "Test message, at /0/ace"
        e.pointer shouldBe JSONPointer("/0/ace")
        e.cause shouldBe nested
    }

    @Test fun `should throw simple exception`() {
        shouldThrow<JSONKotlinException>("Test message") {
            fatal("Test message")
        }.let {
            it.text shouldBe "Test message"
            it.pointer shouldBe null
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception with root pointer`() {
        shouldThrow<JSONKotlinException>("Test message") {
            fatal("Test message", JSONPointer.root)
        }.let {
            it.text shouldBe "Test message"
            it.pointer shouldBe JSONPointer.root
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception with pointer`() {
        shouldThrow<JSONKotlinException>("Test message, at /0/ace") {
            fatal("Test message", JSONPointer.root.child(0).child("ace"))
        }.let {
            it.text shouldBe "Test message"
            it.pointer shouldBe JSONPointer("/0/ace")
            it.cause shouldBe null
        }
    }

    @Test fun `should throw exception with cause`() {
        val nested = JSONException("Nested")
        shouldThrow<JSONKotlinException>("Test message") {
            fatal("Test message", cause = nested)
        }.let {
            it.text shouldBe "Test message"
            it.pointer shouldBe null
            it.cause shouldBe nested
        }
    }

    @Test fun `should throw exception with pointer and cause`() {
        val nested = JSONException("Nested")
        shouldThrow<JSONKotlinException>("Test message, at /0/ace") {
            fatal("Test message", JSONPointer.root.child(0).child("ace"), nested)
        }.let {
            it.text shouldBe "Test message"
            it.pointer shouldBe JSONPointer("/0/ace")
            it.cause shouldBe nested
        }
    }

}
