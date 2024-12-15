/*
 * @(#) CoreSerializersTest.kt
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

import io.kstuff.test.shouldBe

import io.kjson.JSONConfig
import io.kjson.util.CoCapture

class CoreSerializersTest {

    @Test fun `should serialize UUID`() {
        val config = JSONConfig.defaultConfig
        val uuid = UUID.fromString(uuidString)
        with(UUIDSerializer.serialize(uuid, config, mutableListOf())) {
            value shouldBe uuidString
        }
    }

    @Test fun `should stringify UUID`() {
        val config = JSONConfig.defaultConfig
        val uuid = UUID.fromString(uuidString)
        val result = buildString {
            UUIDSerializer.appendTo(this, uuid, config, mutableListOf())
        }
        result shouldBe "\"$uuidString\""
    }

    @Test fun `should stringify UUID non-blocking`() = runBlocking {
        val coCapture = CoCapture()
        val config = JSONConfig.defaultConfig
        val uuid = UUID.fromString(uuidString)
        UUIDSerializer.output(coCapture, uuid, config, mutableListOf())
        coCapture.toString() shouldBe "\"$uuidString\""
    }

    companion object {
        @Suppress("ConstPropertyName")
        const val uuidString = "c733ab74-0c33-11ef-9789-0b8bce3ccb14"
    }

}
