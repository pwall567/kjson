/*
 * @(#) TypeMapTest.kt
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

import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

import io.kjson.deserialize.Deserializer
import io.kjson.deserialize.IntDeserializer
import io.kjson.deserialize.LongDeserializer
import io.kjson.deserialize.StringDeserializer
import io.kjson.testclasses.Dummy1

class TypeMapTest {

    @Test fun `should save simple class`() {
        val deserializerTypeMap = TypeMap<Deserializer<*>>(Deserializer.initialEntries)
        assertNull(deserializerTypeMap[Dummy1::class])
        val initialSize = deserializerTypeMap.classMap.size
        deserializerTypeMap[Dummy1::class] = StringDeserializer
        deserializerTypeMap[Dummy1::class] shouldBe StringDeserializer
        deserializerTypeMap[Dummy1::class.createType()] shouldBe StringDeserializer
        deserializerTypeMap.classMap.size shouldBe initialSize + 1
        with(deserializerTypeMap.classMap[Dummy1::class]) {
            assertIs<TypeMap.SimpleClassEntry<*>>(this)
            handler shouldBe StringDeserializer
        }
    }

    @Test fun `should save parameterised class`() {
        val deserializerTypeMap = TypeMap<Deserializer<*>>(Deserializer.initialEntries)
        val listStringType = typeOf<List<String>>()
        assertNull(deserializerTypeMap[listStringType])
        val initialSize = deserializerTypeMap.classMap.size
        deserializerTypeMap[listStringType] = IntDeserializer
        deserializerTypeMap[listStringType] shouldBe IntDeserializer
        deserializerTypeMap.classMap.size shouldBe initialSize + 1
        with(deserializerTypeMap.classMap[List::class]) {
            assertIs<TypeMap.ParameterizedClassEntry<*>>(this)
            numParams shouldBe 1
            candidates.size shouldBe 1
            with(candidates[0]) {
                first shouldBe IntDeserializer
                second.size shouldBe 1
                with(second[0]) {
                    with(this) {
                        with(type) {
                            assertIs<KType>(this)
                            classifier shouldBe String::class
                            assertFalse(isMarkedNullable)
                        }
                    }
                }
            }
        }
    }

    @Test fun `should save second parameterised class`() {
        val deserializerTypeMap = TypeMap<Deserializer<*>>(Deserializer.initialEntries)
        val listStringType = typeOf<List<String>>()
        val listStringQType = typeOf<List<String?>>()
        assertNull(deserializerTypeMap[listStringType])
        val initialSize = deserializerTypeMap.classMap.size
        deserializerTypeMap[listStringType] = IntDeserializer
        deserializerTypeMap[listStringType] shouldBe IntDeserializer
        deserializerTypeMap[listStringQType] = LongDeserializer
        deserializerTypeMap[listStringQType] shouldBe LongDeserializer
        deserializerTypeMap[listStringType] shouldBe IntDeserializer
        deserializerTypeMap.classMap.size shouldBe initialSize + 1
        with(deserializerTypeMap.classMap[List::class]) {
            assertIs<TypeMap.ParameterizedClassEntry<*>>(this)
            numParams shouldBe 1
            candidates.size shouldBe 2
            with(candidates[0]) {
                first shouldBe IntDeserializer
                second.size shouldBe 1
                with(second[0]) {
                    with(this) {
                        with(type) {
                            assertIs<KType>(this)
                            classifier shouldBe String::class
                            assertFalse(isMarkedNullable)
                        }
                    }
                }
            }
            with(candidates[1]) {
                first shouldBe LongDeserializer
                second.size shouldBe 1
                with(second[0]) {
                    with(this) {
                        with(type) {
                            assertIs<KType>(this)
                            classifier shouldBe String::class
                            assertTrue(isMarkedNullable)
                        }
                    }
                }
            }
        }
    }

}
