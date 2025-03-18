/*
 * @(#) JSONDeserializerInterfaceTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2025 Peter Wall
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
import io.kstuff.test.shouldBeEqual
import io.kstuff.test.shouldThrow

import io.kjson.pointer.JSONPointer

class JSONDeserializerInterfaceTest {

    @Test fun `should deserialize nullable interface`() {
        val nullInterface: EmptyInterface? = null
        shouldBeEqual(nullInterface, null.deserialize())
    }

    @Test fun `should fail to deserialize non-nullable interface`() {
        shouldThrow<JSONKotlinException> {
            null.deserialize<EmptyInterface>()
        }.let {
            it.text shouldBe "Can't deserialize null as ${EmptyInterface::class.qualifiedName}"
            it.pointer shouldBe null
        }
    }

    @Test fun `should deserialize empty collection of interface`() {
        val json = JSONArray.EMPTY
        val empty = listOf<EmptyInterface>()
        shouldBeEqual(empty, json.deserialize())
    }

    @Test fun `should fail to deserialize non-empty collection of interface`() {
        val json = JSONArray.build { add(0) }
        shouldThrow<JSONKotlinException> {
            json.deserialize<List<EmptyInterface>>()
        }.let {
            it.text shouldBe "Can't deserialize 0 as ${EmptyInterface::class.qualifiedName}"
            it.pointer shouldBe JSONPointer("/0")
        }
    }

    @Test fun `should deserialize nullable reference to interface`() {
        val json = JSONObject.EMPTY
        val expected = ClassWithInterfaceReference(null)
        shouldBeEqual(expected, json.deserialize())
    }

    @Test fun `should fail deserialize non-null reference to interface`() {
        val json = JSONObject.build {
            add("ref", 123)
        }
        shouldThrow<JSONKotlinException> {
            json.deserialize<ClassWithInterfaceReference>()
        }.let {
            it.text shouldBe "Can't deserialize 123 as ${EmptyInterface::class.qualifiedName}"
            it.pointer shouldBe JSONPointer("/ref")
        }
    }

    interface EmptyInterface

    data class ClassWithInterfaceReference(
        val ref: EmptyInterface?,
    )

}
