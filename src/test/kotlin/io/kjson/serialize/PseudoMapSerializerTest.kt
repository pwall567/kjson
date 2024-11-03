/*
 * @(#) PseudoMapSerializerTest.kt
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
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

import io.kjson.JSONArray
import io.kjson.JSONConfig
import io.kjson.JSONInt
import io.kjson.JSONString
import io.kjson.util.CoCapture
import io.kjson.util.shouldBe

class PseudoMapSerializerTest {

    @Test fun `should serialize Pseudo-Map`() {
        val config = JSONConfig.defaultConfig
        val map = mapOf<Int?, String?>(
            123 to "alpha",
            456 to "beta",
            789 to "gamma",
        )
        val serializer = PseudoMapSerializer(
            keySerializer = IntSerializer,
            valueSerializer = CharSequenceSerializer,
        )
        with(serializer.serialize(map, config, mutableListOf())) {
            size shouldBe 3
            with(this[0]) {
                assertIs<JSONArray>(this)
                size shouldBe 2
                this[0] shouldBe JSONInt(123)
                this[1] shouldBe JSONString("alpha")
            }
            with(this[1]) {
                assertIs<JSONArray>(this)
                size shouldBe 2
                this[0] shouldBe JSONInt(456)
                this[1] shouldBe JSONString("beta")
            }
            with(this[2]) {
                assertIs<JSONArray>(this)
                size shouldBe 2
                this[0] shouldBe JSONInt(789)
                this[1] shouldBe JSONString("gamma")
            }
        }
    }

    @Test fun `should stringify Pseudo-Map`() {
        val config = JSONConfig.defaultConfig
        val map = mapOf<Int?, String?>(
            123 to "alpha",
            456 to "beta",
            789 to "gamma",
        )
        val serializer = PseudoMapSerializer(
            keySerializer = IntSerializer,
            valueSerializer = CharSequenceSerializer,
        )
        val result = buildString {
            serializer.appendTo(this, map, config, mutableListOf())
        }
        result shouldBe """[[123,"alpha"],[456,"beta"],[789,"gamma"]]"""
    }

    @Test fun `should stringify Pseudo-Map non-blocking`() = runBlocking {
        val coCapture = CoCapture()
        val config = JSONConfig.defaultConfig
        val map = mapOf<Int?, String?>(
            123 to "alpha",
            456 to "beta",
            789 to "gamma",
        )
        val serializer = PseudoMapSerializer(
            keySerializer = IntSerializer,
            valueSerializer = CharSequenceSerializer,
        )
        serializer.output(coCapture, map, config, mutableListOf())
        coCapture.toString() shouldBe """[[123,"alpha"],[456,"beta"],[789,"gamma"]]"""
    }

}
