/*
 * @(#) SequenceSerializerTest.kt
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

import java.util.UUID

import io.kjson.JSONConfig
import io.kjson.JSONString
import io.kjson.util.CoCapture
import io.kjson.util.shouldBe

class SequenceSerializerTest {

    @Test fun `should serialize simple Sequence`() {
        val config = JSONConfig.defaultConfig
        val sequence = sequenceOf(
            UUID.fromString(uuidString1),
            UUID.fromString(uuidString2),
        )
        val serializer = SequenceSerializer(
            itemSerializer = UUIDSerializer,
        )
        with(serializer.serialize(sequence, config, mutableListOf())) {
            size shouldBe 2
            this[0] shouldBe JSONString(uuidString1)
            this[1] shouldBe JSONString(uuidString2)
        }
    }

    @Test fun `should stringify simple Sequence`() {
        val config = JSONConfig.defaultConfig
        val sequence = sequenceOf(
            UUID.fromString(uuidString1),
            UUID.fromString(uuidString2),
        )
        val serializer = SequenceSerializer(
            itemSerializer = UUIDSerializer,
        )
        val result = buildString {
            serializer.appendTo(this, sequence, config, mutableListOf())
        }
        result shouldBe """["$uuidString1","$uuidString2"]"""
    }

    @Test fun `should stringify simple Sequence non-blocking`() = runBlocking {
        val coCapture = CoCapture()
        val config = JSONConfig.defaultConfig
        val sequence = sequenceOf(
            UUID.fromString(uuidString1),
            UUID.fromString(uuidString2),
        )
        val serializer = SequenceSerializer(
            itemSerializer = UUIDSerializer,
        )
        serializer.output(coCapture, sequence, config, mutableListOf())
        coCapture.toString() shouldBe """["$uuidString1","$uuidString2"]"""
    }

    companion object {

        @Suppress("ConstPropertyName")
        const val uuidString1 = "c733ab74-0c33-11ef-9789-0b8bce3ccb14"
        @Suppress("ConstPropertyName")
        const val uuidString2 = "deacaaf6-0c35-11ef-8d06-c73d07a6b923"

    }

}
