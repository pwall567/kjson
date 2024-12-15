/*
 * @(#) MapSerializerTest.kt
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

package io.kjson.serialize

import kotlin.test.Test
import kotlinx.coroutines.runBlocking

import io.kstuff.test.shouldBe

import io.kjson.JSONConfig
import io.kjson.JSONInt
import io.kjson.util.CoCapture

class MapSerializerTest {

    val config = JSONConfig.defaultConfig

    @Test fun `should serialize simple Map`() {
        val map = mapOf(
            "alpha" to 123,
            "beta" to 456,
            "gamma" to 789,
        )
        val serializer = MapSerializer(
            keyClass = String::class,
            keySerializer = CharSequenceSerializer,
            valueSerializer = IntSerializer,
        )
        with(serializer.serialize(map, config, mutableListOf())) {
            size shouldBe 3
            with(this[0]) {
                name shouldBe "alpha"
                value shouldBe JSONInt(123)
            }
            with(this[1]) {
                name shouldBe "beta"
                value shouldBe JSONInt(456)
            }
            with(this[2]) {
                name shouldBe "gamma"
                value shouldBe JSONInt(789)
            }
        }
    }

    @Test fun `should stringify simple Map`() {
        val map = mapOf(
            "alpha" to 123,
            "beta" to 456,
            "gamma" to 789,
        )
        val serializer = MapSerializer(
            keyClass = String::class,
            keySerializer = CharSequenceSerializer,
            valueSerializer = IntSerializer,
        )
        val result = buildString {
            serializer.appendTo(this, map, config, mutableListOf())
        }
        result shouldBe """{"alpha":123,"beta":456,"gamma":789}"""
    }

    @Test fun `should stringify simple Map non-blocking`() = runBlocking {
        val coCapture = CoCapture()
        val map = mapOf(
            "alpha" to 123,
            "beta" to 456,
            "gamma" to 789,
        )
        val serializer = MapSerializer(
            keyClass = String::class,
            keySerializer = CharSequenceSerializer,
            valueSerializer = IntSerializer,
        )
        serializer.output(coCapture, map, config, mutableListOf())
        coCapture.toString() shouldBe """{"alpha":123,"beta":456,"gamma":789}"""
    }

}
