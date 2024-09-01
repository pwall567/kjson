/*
 * @(#) ReflectionUtilTest.kt
 *
 * kjson  Reflection-based JSON serialization and deserialization for Kotlin
 * Copyright (c) 2024 Peter Wall
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

package io.kjson.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

import io.kjson.JSONArray
import io.kjson.JSONBoolean
import io.kjson.JSONInt
import io.kjson.JSONNumber
import io.kjson.JSONObject
import io.kjson.JSONPrimitive
import io.kjson.JSONStructure
import io.kjson.testclasses.Dummy1
import io.kjson.testclasses.JavaClass1

class ReflectionUtilTest {

    @Test fun `should determine whether class is a Kotlin class`() {
        val kotlinClass = Dummy1::class
        assertTrue(kotlinClass.isKotlinClass())
        val javaClass = JavaClass1::class
        assertFalse(javaClass.isKotlinClass())
    }

    @Test fun `should locate sealed class in hierarchy of a given class`() {
        JSONInt::class.findSealedClass() shouldBe JSONNumber::class
        JSONBoolean::class.findSealedClass() shouldBe JSONPrimitive::class
        JSONArray::class.findSealedClass() shouldBe JSONStructure::class
        assertNull(JSONObject.Property::class.findSealedClass())
    }

    @Test fun `should test whether a Java class member is public`() {
        val javaClass1Class = JavaClass1::class.java
        assertFalse(javaClass1Class.getDeclaredField("field1").isPublic())
        assertTrue(javaClass1Class.getDeclaredMethod("getField1").isPublic())
    }

    @Test fun `should test whether a Java class member is static`() {
        val javaClass1Class = JavaClass1::class.java
        assertFalse(javaClass1Class.getDeclaredMethod("getField1").isStatic())
        assertTrue(javaClass1Class.getDeclaredMethod("getDescription").isStatic())
    }

}
